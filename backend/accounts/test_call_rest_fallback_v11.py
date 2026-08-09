from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework_simplejwt.tokens import AccessToken

from .models import CallSession, Conversation


User = get_user_model()
PASSWORD = "StrongNovaPass2026!"


class CallRestFallbackV11Tests(TestCase):
    def setUp(self):
        self.caller = User.objects.create_user(
            email="caller-rest@example.com",
            username="caller-rest",
            password=PASSWORD,
            name="Caller",
        )
        self.callee = User.objects.create_user(
            email="callee-rest@example.com",
            username="callee-rest",
            password=PASSWORD,
            name="Callee",
        )
        self.stranger = User.objects.create_user(
            email="stranger-rest@example.com",
            username="stranger-rest",
            password=PASSWORD,
            name="Stranger",
        )
        first, second = sorted((self.caller.pk, self.callee.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )

    def auth(self, user):
        return {"HTTP_AUTHORIZATION": f"Bearer {AccessToken.for_user(user)}"}

    def make_call(self, status=CallSession.Status.RINGING):
        return CallSession.objects.create(
            conversation=self.conversation,
            caller=self.caller,
            callee=self.callee,
            kind=CallSession.Kind.AUDIO,
            status=status,
        )

    @patch("accounts.calls.send_call_state_push")
    def test_callee_can_accept_over_rest_and_retry_is_idempotent(self, push):
        call = self.make_call()
        url = f"/api/v1/calls/{call.pk}/action/"

        response = self.client.post(
            url,
            data={"action": "accept"},
            content_type="application/json",
            **self.auth(self.callee),
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "active")
        call.refresh_from_db()
        self.assertEqual(call.status, CallSession.Status.ACTIVE)
        self.assertIsNotNone(call.answered_at)
        push.assert_not_called()

        retry = self.client.post(
            url,
            data={"action": "accept"},
            content_type="application/json",
            **self.auth(self.callee),
        )
        self.assertEqual(retry.status_code, 200)
        self.assertEqual(retry.json()["status"], "active")

    @patch("accounts.calls.send_call_state_push")
    def test_active_call_can_end_over_rest(self, push):
        call = self.make_call(status=CallSession.Status.ACTIVE)
        response = self.client.post(
            f"/api/v1/calls/{call.pk}/action/",
            data={"action": "end"},
            content_type="application/json",
            **self.auth(self.caller),
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "ended")
        call.refresh_from_db()
        self.assertEqual(call.status, CallSession.Status.ENDED)
        self.assertIsNotNone(call.ended_at)
        push.assert_called_once_with(str(call.pk), self.callee.pk)

    @patch("accounts.calls.send_call_state_push")
    def test_callee_can_decline_over_rest(self, push):
        call = self.make_call()
        response = self.client.post(
            f"/api/v1/calls/{call.pk}/action/",
            data={"action": "decline"},
            content_type="application/json",
            **self.auth(self.callee),
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "declined")
        push.assert_called_once_with(str(call.pk), self.caller.pk)

    def test_nonparticipant_cannot_change_call(self):
        call = self.make_call()
        response = self.client.post(
            f"/api/v1/calls/{call.pk}/action/",
            data={"action": "end"},
            content_type="application/json",
            **self.auth(self.stranger),
        )
        self.assertEqual(response.status_code, 404)
