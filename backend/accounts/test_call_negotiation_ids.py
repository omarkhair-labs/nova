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


class CallNegotiationIdTests(TransactionTestCase):
    reset_sequences = True

    def setUp(self):
        channel_layers.backends.clear()
        self.caller = User.objects.create_user(
            email="caller-negotiation@example.com",
            username="caller-negotiation",
            password=PASSWORD,
            name="Caller",
        )
        self.callee = User.objects.create_user(
            email="callee-negotiation@example.com",
            username="callee-negotiation",
            password=PASSWORD,
            name="Callee",
        )
        first, second = sorted((self.caller.pk, self.callee.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )

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
        return socket

    async def test_offer_and_answer_preserve_negotiation_id(self):
        call = await database_sync_to_async(CallSession.objects.create)(
            conversation=self.conversation,
            caller=self.caller,
            callee=self.callee,
            kind=CallSession.Kind.AUDIO,
        )
        caller = await self.connect(call, self.caller)
        callee = await self.connect(call, self.callee)

        try:
            caller_peer = await caller.receive_json_from(timeout=1)
            callee_peer = await callee.receive_json_from(timeout=1)
            self.assertEqual(caller_peer["type"], "call.peer_ready")
            self.assertEqual(callee_peer["type"], "call.peer_ready")

            await caller.send_json_to(
                {
                    "type": "call.offer",
                    "sdp": "offer-generation-one",
                    "negotiation_id": "generation-1",
                }
            )
            offer = await callee.receive_json_from(timeout=1)
            self.assertEqual(offer["type"], "call.offer")
            self.assertEqual(offer["sdp"], "offer-generation-one")
            self.assertEqual(offer["negotiation_id"], "generation-1")

            await callee.send_json_to(
                {
                    "type": "call.answer",
                    "sdp": "answer-generation-one",
                    "negotiation_id": "generation-1",
                }
            )
            answer = await caller.receive_json_from(timeout=1)
            self.assertEqual(answer["type"], "call.answer")
            self.assertEqual(answer["sdp"], "answer-generation-one")
            self.assertEqual(answer["negotiation_id"], "generation-1")
        finally:
            await caller.disconnect()
            await callee.disconnect()

    async def test_oversized_negotiation_id_is_rejected(self):
        call = await database_sync_to_async(CallSession.objects.create)(
            conversation=self.conversation,
            caller=self.caller,
            callee=self.callee,
            kind=CallSession.Kind.AUDIO,
        )
        caller = await self.connect(call, self.caller)

        try:
            await caller.send_json_to(
                {
                    "type": "call.offer",
                    "sdp": "offer",
                    "negotiation_id": "x" * 129,
                }
            )
            error = await caller.receive_json_from(timeout=1)
            self.assertEqual(error["type"], "call.error")
            self.assertIn("invalid", error["detail"].lower())
        finally:
            await caller.disconnect()
