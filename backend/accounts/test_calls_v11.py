from unittest.mock import patch

from channels.db import database_sync_to_async
from channels.layers import channel_layers
from channels.testing import WebsocketCommunicator
from django.contrib.auth import get_user_model
from django.test import TransactionTestCase
from rest_framework_simplejwt.tokens import AccessToken

from nova_backend.asgi import application

from .models import CallSession, Conversation


User = get_user_model()
PASSWORD = "StrongNovaPass2026!"


class CallsV11Tests(TransactionTestCase):
    reset_sequences = True

    def setUp(self):
        channel_layers.backends.clear()
        self.omar = User.objects.create_user(
            email="omar-calls@example.com",
            username="omar-calls",
            password=PASSWORD,
            name="Omar",
        )
        self.maya = User.objects.create_user(
            email="maya-calls@example.com",
            username="maya-calls",
            password=PASSWORD,
            name="Maya",
        )
        self.stranger = User.objects.create_user(
            email="stranger-calls@example.com",
            username="stranger-calls",
            password=PASSWORD,
            name="Stranger",
        )
        first, second = sorted((self.omar.pk, self.maya.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )

    def auth(self, user):
        return {"HTTP_AUTHORIZATION": f"Bearer {AccessToken.for_user(user)}"}

    @patch("accounts.call_reliability_view.send_call_push", return_value=1)
    def test_create_call_persists_ringing_session_and_prevents_busy_overlap(self, push):
        response = self.client.post(
            "/api/v1/calls/",
            data={"conversation_id": self.conversation.pk, "kind": "video"},
            content_type="application/json",
            **self.auth(self.omar),
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["status"], "ringing")
        self.assertEqual(response.json()["kind"], "video")
        self.assertEqual(response.json()["callee"]["id"], self.maya.pk)
        call = CallSession.objects.get(pk=response.json()["id"])
        self.assertEqual(call.caller_id, self.omar.pk)
        self.assertEqual(call.callee_id, self.maya.pk)
        push.assert_called_once_with(call.pk)

        busy = self.client.post(
            "/api/v1/calls/",
            data={"conversation_id": self.conversation.pk, "kind": "audio"},
            content_type="application/json",
            **self.auth(self.maya),
        )
        self.assertEqual(busy.status_code, 409)

    def test_ice_config_has_safe_stun_fallback(self):
        with patch.dict("os.environ", {"NOVA_CALL_ICE_SERVERS_JSON": ""}):
            response = self.client.get("/api/v1/calls/ice/", **self.auth(self.omar))
        self.assertEqual(response.status_code, 200)
        self.assertFalse(response.json()["turn_configured"])
        self.assertTrue(response.json()["ice_servers"])
        self.assertTrue(response.json()["ice_servers"][0]["urls"][0].startswith("stun:"))

    def test_nonparticipant_cannot_read_call(self):
        call = CallSession.objects.create(
            conversation=self.conversation,
            caller=self.omar,
            callee=self.maya,
            kind=CallSession.Kind.AUDIO,
        )
        response = self.client.get(
            f"/api/v1/calls/{call.pk}/",
            **self.auth(self.stranger),
        )
        self.assertEqual(response.status_code, 404)

    def communicator(self, call, user):
        token = str(AccessToken.for_user(user))
        return WebsocketCommunicator(
            application,
            f"/ws/calls/{call.pk}/",
            headers=[(b"authorization", f"Bearer {token}".encode("ascii"))],
        )

    async def connect(self, call, user):
        socket = self.communicator(call, user)
        connected, _ = await socket.connect()
        self.assertTrue(connected)
        ready = await socket.receive_json_from(timeout=1)
        self.assertEqual(ready["type"], "call.ready")
        return socket, ready

    async def test_call_socket_relays_offer_answer_ice_and_lifecycle(self):
        call = await database_sync_to_async(CallSession.objects.create)(
            conversation=self.conversation,
            caller=self.omar,
            callee=self.maya,
            kind=CallSession.Kind.VIDEO,
        )
        caller, _ = await self.connect(call, self.omar)
        callee, _ = await self.connect(call, self.maya)

        try:
            caller_peer = await caller.receive_json_from(timeout=1)
            callee_peer = await callee.receive_json_from(timeout=1)
            self.assertEqual(caller_peer["type"], "call.peer_ready")
            self.assertEqual(caller_peer["user_id"], self.maya.pk)
            self.assertEqual(callee_peer["type"], "call.peer_ready")
            self.assertEqual(callee_peer["user_id"], self.omar.pk)

            await caller.send_json_to({"type": "call.offer", "sdp": "offer-sdp"})
            offer = await callee.receive_json_from(timeout=1)
            self.assertEqual(offer, {"type": "call.offer", "sdp": "offer-sdp"})
            self.assertTrue(await caller.receive_nothing(timeout=0.1))

            await callee.send_json_to({"type": "call.accept"})
            caller_state = await caller.receive_json_from(timeout=1)
            callee_state = await callee.receive_json_from(timeout=1)
            self.assertEqual(caller_state["type"], "call.state")
            self.assertEqual(caller_state["call"]["status"], "active")
            self.assertEqual(callee_state["call"]["status"], "active")
            self.assertTrue(caller_state["call"]["is_caller"])
            self.assertEqual(caller_state["call"]["peer"]["id"], self.maya.pk)
            self.assertFalse(callee_state["call"]["is_caller"])
            self.assertEqual(callee_state["call"]["peer"]["id"], self.omar.pk)

            await callee.send_json_to({"type": "call.answer", "sdp": "answer-sdp"})
            answer = await caller.receive_json_from(timeout=1)
            self.assertEqual(answer, {"type": "call.answer", "sdp": "answer-sdp"})

            await caller.send_json_to(
                {
                    "type": "call.ice",
                    "candidate": "candidate:1 1 udp 1 127.0.0.1 12345 typ host",
                    "sdp_mid": "0",
                    "sdp_mline_index": 0,
                }
            )
            ice = await callee.receive_json_from(timeout=1)
            self.assertEqual(ice["type"], "call.ice")
            self.assertEqual(ice["sdp_mline_index"], 0)

            await caller.send_json_to({"type": "call.end"})
            end_for_caller = await caller.receive_json_from(timeout=1)
            end_for_callee = await callee.receive_json_from(timeout=1)
            self.assertEqual(end_for_caller["call"]["status"], "ended")
            self.assertEqual(end_for_callee["call"]["status"], "ended")
            self.assertTrue(end_for_caller["call"]["is_caller"])
            self.assertFalse(end_for_callee["call"]["is_caller"])

            saved = await database_sync_to_async(CallSession.objects.get)(pk=call.pk)
            self.assertEqual(saved.status, CallSession.Status.ENDED)
            self.assertIsNotNone(saved.ended_at)
        finally:
            await caller.disconnect()
            await callee.disconnect()

    async def test_callee_cannot_send_offer(self):
        call = await database_sync_to_async(CallSession.objects.create)(
            conversation=self.conversation,
            caller=self.omar,
            callee=self.maya,
            kind=CallSession.Kind.AUDIO,
        )
        callee, _ = await self.connect(call, self.maya)
        try:
            await callee.send_json_to({"type": "call.offer", "sdp": "wrong-side"})
            error = await callee.receive_json_from(timeout=1)
            self.assertEqual(error["type"], "call.error")
            self.assertIn("caller", error["detail"].lower())
        finally:
            await callee.disconnect()
