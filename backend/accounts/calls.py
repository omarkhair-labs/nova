import json
import os
from datetime import timedelta

from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import CallSession, Conversation, User
from .push import send_call_push


ACTIVE_CALL_STATUSES = (CallSession.Status.RINGING, CallSession.Status.ACTIVE)
CALL_RING_TIMEOUT_SECONDS = 45
CALL_LIVENESS_TTL_SECONDS = 90
CALL_LIVENESS_PREFIX = "nova:call:live"


def _user_payload(user):
    avatar_url = ""
    if user.avatar:
        try:
            avatar_url = user.avatar.url
        except Exception:
            avatar_url = ""
    return {
        "id": user.pk,
        "username": user.username,
        "name": user.name,
        "avatar_url": avatar_url,
    }


def serialize_call(call, viewer_id=None):
    caller = call.caller
    callee = call.callee
    peer = callee if viewer_id == caller.pk else caller
    return {
        "id": str(call.pk),
        "conversation_id": call.conversation_id,
        "kind": call.kind,
        "status": call.status,
        "caller": _user_payload(caller),
        "callee": _user_payload(callee),
        "peer": _user_payload(peer),
        "is_caller": viewer_id == caller.pk if viewer_id is not None else None,
        "created_at": call.created_at.isoformat(),
        "answered_at": call.answered_at.isoformat() if call.answered_at else None,
        "ended_at": call.ended_at.isoformat() if call.ended_at else None,
        "end_reason": call.end_reason,
        "ring_timeout_seconds": CALL_RING_TIMEOUT_SECONDS,
    }


def _call_redis():
    url = os.getenv("REDIS_URL", "").strip()
    if not url:
        return None
    try:
        import redis

        return redis.Redis.from_url(url, decode_responses=True)
    except Exception:
        return None


def _call_liveness_key(call_id):
    return f"{CALL_LIVENESS_PREFIX}:{call_id}"


def touch_call_liveness(call_id):
    """Refresh a short Redis lease while at least one call socket is alive."""

    client = _call_redis()
    if client is None:
        return None
    try:
        client.set(
            _call_liveness_key(call_id),
            "1",
            ex=CALL_LIVENESS_TTL_SECONDS,
        )
        return True
    except Exception:
        # Redis outages must not tear down a live call. Call-state durability
        # remains in Postgres; liveness is only used to clean abandoned locks.
        return None


def clear_call_liveness(call_id):
    client = _call_redis()
    if client is None:
        return None
    try:
        client.delete(_call_liveness_key(call_id))
        return True
    except Exception:
        return None


def call_is_live(call_id):
    client = _call_redis()
    if client is None:
        return None
    try:
        return bool(client.exists(_call_liveness_key(call_id)))
    except Exception:
        return None


def expire_stale_call_locks(user_ids):
    """Release abandoned ringing/active rows before deciding a user is busy."""

    ids = tuple(int(user_id) for user_id in user_ids)
    if not ids:
        return

    now = timezone.now()
    participants = Q(caller_id__in=ids) | Q(callee_id__in=ids)
    stale_ringing = CallSession.objects.filter(
        participants,
        status=CallSession.Status.RINGING,
        created_at__lte=now - timedelta(seconds=CALL_RING_TIMEOUT_SECONDS + 15),
    )
    stale_ringing.update(
        status=CallSession.Status.MISSED,
        ended_at=now,
        end_reason="timeout",
    )

    # Active calls have no natural timestamp once media is established. The
    # call signaling client therefore keeps a short Redis lease alive. If Redis
    # can positively tell us the lease is gone, the abandoned DB row is safe to
    # release. If Redis itself is unavailable, be conservative and keep it busy.
    active_calls = list(
        CallSession.objects.filter(participants, status=CallSession.Status.ACTIVE)
        .only("id")
    )
    for active_call in active_calls:
        if call_is_live(active_call.pk) is False:
            CallSession.objects.filter(
                pk=active_call.pk,
                status=CallSession.Status.ACTIVE,
            ).update(
                status=CallSession.Status.FAILED,
                ended_at=now,
                end_reason="connection_lost",
            )


def load_ice_servers():
    raw = os.getenv("NOVA_CALL_ICE_SERVERS_JSON", "").strip()
    if raw:
        try:
            value = json.loads(raw)
            if isinstance(value, list) and value:
                cleaned = []
                for item in value:
                    if not isinstance(item, dict):
                        continue
                    urls = item.get("urls")
                    if isinstance(urls, str):
                        urls = [urls]
                    if not isinstance(urls, list) or not any(str(url).strip() for url in urls):
                        continue
                    server = {"urls": [str(url).strip() for url in urls if str(url).strip()]}
                    username = str(item.get("username") or "").strip()
                    credential = str(item.get("credential") or "").strip()
                    if username:
                        server["username"] = username
                    if credential:
                        server["credential"] = credential
                    cleaned.append(server)
                if cleaned:
                    return cleaned
        except (TypeError, ValueError):
            pass

    # Development/test fallback. STUN is enough for many direct connections,
    # but production reliability still requires a TURN server in the env config.
    return [{"urls": ["stun:stun.l.google.com:19302"]}]


def turn_configured(servers):
    for server in servers:
        for url in server.get("urls", []):
            if str(url).lower().startswith(("turn:", "turns:")):
                return True
    return False


class CallIceConfigView(APIView):
    def get(self, request):
        servers = load_ice_servers()
        return Response(
            {
                "ice_servers": servers,
                "turn_configured": turn_configured(servers),
            }
        )


class CallSessionCreateView(APIView):
    def post(self, request):
        try:
            conversation_id = int(request.data.get("conversation_id"))
        except (TypeError, ValueError):
            return Response(
                {"detail": "A valid conversation is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        kind = str(request.data.get("kind") or "").strip().lower()
        if kind not in CallSession.Kind.values:
            return Response(
                {"detail": "Call kind must be audio or video."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        conversation = get_object_or_404(
            Conversation.objects.select_related("participant_one", "participant_two"),
            pk=conversation_id,
        )
        caller = request.user
        if caller.pk not in (conversation.participant_one_id, conversation.participant_two_id):
            return Response(status=status.HTTP_404_NOT_FOUND)
        callee = (
            conversation.participant_two
            if conversation.participant_one_id == caller.pk
            else conversation.participant_one
        )

        with transaction.atomic():
            # Lock both participant rows in deterministic order so simultaneous
            # calls cannot both pass the busy check.
            list(
                User.objects.select_for_update()
                .filter(pk__in=(caller.pk, callee.pk))
                .order_by("pk")
                .values_list("pk", flat=True)
            )
            expire_stale_call_locks((caller.pk, callee.pk))
            busy = CallSession.objects.filter(status__in=ACTIVE_CALL_STATUSES).filter(
                Q(caller_id__in=(caller.pk, callee.pk))
                | Q(callee_id__in=(caller.pk, callee.pk))
            ).exists()
            if busy:
                return Response(
                    {"detail": "One of you is already in another call."},
                    status=status.HTTP_409_CONFLICT,
                )

            call = CallSession.objects.create(
                conversation=conversation,
                caller=caller,
                callee=callee,
                kind=kind,
            )
            transaction.on_commit(lambda: send_call_push(call.pk))

        return Response(
            serialize_call(call, viewer_id=caller.pk),
            status=status.HTTP_201_CREATED,
        )


class CallSessionDetailView(APIView):
    def get(self, request, call_id):
        call = get_object_or_404(
            CallSession.objects.select_related(
                "conversation",
                "caller",
                "callee",
            ),
            pk=call_id,
        )
        if request.user.pk not in (call.caller_id, call.callee_id):
            return Response(status=status.HTTP_404_NOT_FOUND)

        if (
            call.status == CallSession.Status.RINGING
            and call.created_at <= timezone.now() - timedelta(seconds=CALL_RING_TIMEOUT_SECONDS + 15)
        ):
            CallSession.objects.filter(
                pk=call.pk,
                status=CallSession.Status.RINGING,
            ).update(
                status=CallSession.Status.MISSED,
                ended_at=timezone.now(),
                end_reason="timeout",
            )
            call.refresh_from_db()
        elif call.status == CallSession.Status.ACTIVE and call_is_live(call.pk) is False:
            CallSession.objects.filter(
                pk=call.pk,
                status=CallSession.Status.ACTIVE,
            ).update(
                status=CallSession.Status.FAILED,
                ended_at=timezone.now(),
                end_reason="connection_lost",
            )
            call.refresh_from_db()

        return Response(serialize_call(call, viewer_id=request.user.pk))
