from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer
from django.db import transaction
from django.utils import timezone

from .calls import clear_call_liveness, serialize_call, touch_call_liveness
from .models import CallSession
from .push import send_call_state_push


MAX_SDP_LENGTH = 256_000
MAX_ICE_CANDIDATE_LENGTH = 16_000
TERMINAL_CALL_STATUSES = {
    CallSession.Status.DECLINED,
    CallSession.Status.CANCELED,
    CallSession.Status.ENDED,
    CallSession.Status.MISSED,
    CallSession.Status.FAILED,
}


def call_group_name(call_id):
    return f"call.{call_id}"


class CallConsumer(AsyncJsonWebsocketConsumer):
    """Authenticated 1:1 call lifecycle + WebRTC signaling relay."""

    async def connect(self):
        self.call_id = str(self.scope["url_route"]["kwargs"]["call_id"])
        self.group_name = call_group_name(self.call_id)
        self.joined_group = False

        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            await self.close(code=4401)
            return

        payload = await self._call_payload(user.pk)
        if payload is None:
            await self.close(code=4403)
            return

        await database_sync_to_async(touch_call_liveness)(self.call_id)
        await self.channel_layer.group_add(self.group_name, self.channel_name)
        self.joined_group = True
        await self.accept()
        await self.send_json({"type": "call.ready", "call": payload})
        # WebRTC signaling is intentionally gated until both peers are actually
        # attached to this Channels group. A two-way ACK makes the handshake
        # work regardless of which participant connects first.
        await self.channel_layer.group_send(
            self.group_name,
            {
                "type": "call.joined",
                "user_id": user.pk,
            },
        )

    async def disconnect(self, close_code):
        if getattr(self, "joined_group", False):
            await self.channel_layer.group_discard(self.group_name, self.channel_name)
            self.joined_group = False

    async def receive_json(self, content, **kwargs):
        if not getattr(self, "joined_group", False):
            return

        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            await self.close(code=4401)
            return

        await database_sync_to_async(touch_call_liveness)(self.call_id)
        event_type = str(content.get("type") or "")
        if event_type == "ping":
            await self.send_json({"type": "pong"})
            return

        if event_type == "call.ice_restart":
            allowed, detail = await self._can_restart_ice(user.pk)
            if not allowed:
                await self.send_json({"type": "call.error", "detail": detail})
                return
            await self.channel_layer.group_send(
                self.group_name,
                {
                    "type": "call.signal",
                    "sender_id": user.pk,
                    "signal": {"type": "call.ice_restart"},
                },
            )
            return

        if event_type in ("call.offer", "call.answer", "call.ice"):
            allowed, detail = await self._can_signal(user.pk, event_type)
            if not allowed:
                await self.send_json({"type": "call.error", "detail": detail})
                return

            signal = self._clean_signal(event_type, content)
            if signal is None:
                await self.send_json({"type": "call.error", "detail": "Invalid WebRTC signaling payload."})
                return

            await self.channel_layer.group_send(
                self.group_name,
                {
                    "type": "call.signal",
                    "sender_id": user.pk,
                    "signal": signal,
                },
            )
            return

        action_map = {
            "call.accept": "accept",
            "call.decline": "decline",
            "call.cancel": "cancel",
            "call.end": "end",
            "call.timeout": "timeout",
            "call.failed": "failed",
        }
        action = action_map.get(event_type)
        if action is None:
            return

        result = await self._transition(user.pk, action)
        if result.get("error"):
            await self.send_json({"type": "call.error", "detail": result["error"]})
            return

        # Broadcast only the fact that state changed. Each socket serializes the
        # call for its own authenticated user so `peer` and `is_caller` can never
        # leak the actor's point of view to the other participant.
        await self.channel_layer.group_send(
            self.group_name,
            {
                "type": "call.state",
            },
        )

        call_payload = result["call"]
        if call_payload.get("status") in TERMINAL_CALL_STATUSES:
            caller_id = call_payload["caller"]["id"]
            callee_id = call_payload["callee"]["id"]
            target_user_id = callee_id if user.pk == caller_id else caller_id
            await database_sync_to_async(clear_call_liveness)(self.call_id)
            await database_sync_to_async(send_call_state_push)(
                self.call_id,
                target_user_id,
            )

    def _clean_signal(self, event_type, content):
        if event_type in ("call.offer", "call.answer"):
            sdp = str(content.get("sdp") or "")
            if not sdp or len(sdp) > MAX_SDP_LENGTH:
                return None
            return {"type": event_type, "sdp": sdp}

        candidate = str(content.get("candidate") or "")
        if not candidate or len(candidate) > MAX_ICE_CANDIDATE_LENGTH:
            return None
        try:
            sdp_mline_index = int(content.get("sdp_mline_index"))
        except (TypeError, ValueError):
            return None
        sdp_mid = str(content.get("sdp_mid") or "")
        return {
            "type": "call.ice",
            "candidate": candidate,
            "sdp_mid": sdp_mid,
            "sdp_mline_index": sdp_mline_index,
        }

    async def call_joined(self, event):
        user = self.scope.get("user")
        joined_user_id = event.get("user_id")
        if not user or not user.is_authenticated or joined_user_id == user.pk:
            return

        await self.send_json({"type": "call.peer_ready", "user_id": joined_user_id})
        await self.channel_layer.group_send(
            self.group_name,
            {
                "type": "call.peer_ack",
                "user_id": user.pk,
                "target_user_id": joined_user_id,
            },
        )

    async def call_peer_ack(self, event):
        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            return
        if event.get("target_user_id") != user.pk:
            return
        await self.send_json({"type": "call.peer_ready", "user_id": event.get("user_id")})

    async def call_signal(self, event):
        user = self.scope.get("user")
        if user and event.get("sender_id") == user.pk:
            return
        await self.send_json(event["signal"])

    async def call_state(self, event):
        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            return
        payload = await self._call_payload(user.pk)
        if payload is not None:
            await self.send_json({"type": "call.state", "call": payload})

    @database_sync_to_async
    def _call_payload(self, user_id):
        call = (
            CallSession.objects.select_related("caller", "callee", "conversation")
            .filter(pk=self.call_id)
            .first()
        )
        if call is None or user_id not in (call.caller_id, call.callee_id):
            return None
        return serialize_call(call, viewer_id=user_id)

    @database_sync_to_async
    def _can_signal(self, user_id, event_type):
        call = CallSession.objects.filter(pk=self.call_id).first()
        if call is None or user_id not in (call.caller_id, call.callee_id):
            return False, "Call not found."
        if call.status not in (CallSession.Status.RINGING, CallSession.Status.ACTIVE):
            return False, "This call has already ended."
        if event_type == "call.offer" and user_id != call.caller_id:
            return False, "Only the caller can create the offer."
        if event_type == "call.answer" and user_id != call.callee_id:
            return False, "Only the callee can create the answer."
        return True, ""

    @database_sync_to_async
    def _can_restart_ice(self, user_id):
        call = CallSession.objects.filter(pk=self.call_id).first()
        if call is None or user_id not in (call.caller_id, call.callee_id):
            return False, "Call not found."
        if call.status != CallSession.Status.ACTIVE:
            return False, "ICE recovery is only available during an active call."
        return True, ""

    @database_sync_to_async
    def _transition(self, user_id, action):
        with transaction.atomic():
            call = (
                CallSession.objects.select_for_update()
                .select_related("caller", "callee", "conversation")
                .filter(pk=self.call_id)
                .first()
            )
            if call is None or user_id not in (call.caller_id, call.callee_id):
                return {"error": "Call not found."}

            now = timezone.now()
            if action == "accept":
                if user_id != call.callee_id:
                    return {"error": "Only the person being called can answer."}
                if call.status == CallSession.Status.ACTIVE:
                    return {"call": serialize_call(call, viewer_id=user_id)}
                if call.status != CallSession.Status.RINGING:
                    return {"error": "This call is no longer ringing."}
                call.status = CallSession.Status.ACTIVE
                call.answered_at = now
                call.save(update_fields=("status", "answered_at"))

            elif action == "decline":
                if user_id != call.callee_id or call.status != CallSession.Status.RINGING:
                    return {"error": "This call can no longer be declined."}
                call.status = CallSession.Status.DECLINED
                call.ended_at = now
                call.ended_by_id = user_id
                call.end_reason = "declined"
                call.save(update_fields=("status", "ended_at", "ended_by", "end_reason"))

            elif action == "cancel":
                if user_id != call.caller_id or call.status != CallSession.Status.RINGING:
                    return {"error": "This call can no longer be canceled."}
                call.status = CallSession.Status.CANCELED
                call.ended_at = now
                call.ended_by_id = user_id
                call.end_reason = "canceled"
                call.save(update_fields=("status", "ended_at", "ended_by", "end_reason"))

            elif action == "timeout":
                if user_id != call.caller_id or call.status != CallSession.Status.RINGING:
                    return {"error": "This call can no longer time out."}
                call.status = CallSession.Status.MISSED
                call.ended_at = now
                call.ended_by_id = user_id
                call.end_reason = "timeout"
                call.save(update_fields=("status", "ended_at", "ended_by", "end_reason"))

            elif action == "failed":
                if call.status not in (CallSession.Status.RINGING, CallSession.Status.ACTIVE):
                    return {"call": serialize_call(call, viewer_id=user_id)}
                call.status = CallSession.Status.FAILED
                call.ended_at = now
                call.ended_by_id = user_id
                call.end_reason = "connection_failed"
                call.save(update_fields=("status", "ended_at", "ended_by", "end_reason"))

            elif action == "end":
                if call.status == CallSession.Status.RINGING:
                    if user_id == call.caller_id:
                        call.status = CallSession.Status.CANCELED
                        call.end_reason = "canceled"
                    else:
                        call.status = CallSession.Status.DECLINED
                        call.end_reason = "declined"
                elif call.status == CallSession.Status.ACTIVE:
                    call.status = CallSession.Status.ENDED
                    call.end_reason = "hangup"
                else:
                    return {"call": serialize_call(call, viewer_id=user_id)}
                call.ended_at = now
                call.ended_by_id = user_id
                call.save(update_fields=("status", "ended_at", "ended_by", "end_reason"))

            return {"call": serialize_call(call, viewer_id=user_id)}
