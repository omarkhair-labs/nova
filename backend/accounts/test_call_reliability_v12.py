from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework_simplejwt.tokens import AccessToken

from .models import CallSession, Conversation, Message


User = get_user_model()
PASSWORD = "StrongNovaPass2026!"


class CallReliabilityV12Tests(TestCase):
    def setUp(self):
        self.caller = User.objects.create_user(
            email="caller-reliability@example.com",
            username="caller-reliability",
            password=PASSWORD,
            name="Caller",
        )
        self.callee = User.objects.create_user(
            email="callee-reliability@example.com",
            username="callee-reliability",
            password=PASSWORD,
            name="Callee",
        )
        first, second = sorted((self.caller.pk, self.callee.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )

    def auth(self, user):
        return {"HTTP_AUTHORIZATION": f"Bearer {AccessToken.for_user(user)}"}

    def create_url(self):
        return "/api/v1/calls/"

    @patch("accounts.call_reliability_view.call_is_live", return_value=False)
    @patch("accounts.call_reliability_view.send_call_state_push")
    @patch("accounts.call_reliability_view.send_call_push", return_value=1)
    def test_retry_supersedes_orphan_ringing_call_without_false_busy(
        self,
        push,
        state_push,
        call_is_live,
    ):
        previous = CallSession.objects.create(
            conversation=self.conversation,
            caller=self.caller,
            callee=self.callee,
            kind=CallSession.Kind.AUDIO,
        )

        response = self.client.post(
            self.create_url(),
            data={"conversation_id": self.conversation.pk, "kind": "audio"},
            content_type="application/json",
            **self.auth(self.caller),
        )

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["status"], "ringing")
        previous.refresh_from_db()
        self.assertEqual(previous.status, CallSession.Status.FAILED)
        self.assertEqual(previous.end_reason, "replaced_by_retry")
        call_is_live.assert_called_once_with(previous.pk)
        state_push.assert_called_once_with(str(previous.pk), self.callee.pk)
        self.assertEqual(push.call_count, 1)

        history = Message.objects.get(
            sender=self.caller,
            client_id=f"call:{previous.pk}",
        )
        self.assertEqual(history.body, "Voice call · Failed")
        self.assertIsNotNone(history.delivered_at)
        self.assertIsNotNone(history.read_at)

    @patch("accounts.call_reliability_view.call_is_live", return_value=True)
    @patch("accounts.call_reliability_view.send_call_push", return_value=1)
    def test_retry_does_not_replace_a_still_live_ringing_call(self, push, call_is_live):
        previous = CallSession.objects.create(
            conversation=self.conversation,
            caller=self.caller,
            callee=self.callee,
            kind=CallSession.Kind.AUDIO,
        )

        response = self.client.post(
            self.create_url(),
            data={"conversation_id": self.conversation.pk, "kind": "audio"},
            content_type="application/json",
            **self.auth(self.caller),
        )

        self.assertEqual(response.status_code, 409)
        previous.refresh_from_db()
        self.assertEqual(previous.status, CallSession.Status.RINGING)
        call_is_live.assert_called_once_with(previous.pk)
        push.assert_not_called()

    @patch("accounts.call_reliability_view.send_call_state_push")
    @patch("accounts.call_reliability_view.send_call_push", return_value=0)
    def test_missing_push_destination_fails_fast_and_releases_busy_lock(self, push, state_push):
        failed = self.client.post(
            self.create_url(),
            data={"conversation_id": self.conversation.pk, "kind": "video"},
            content_type="application/json",
            **self.auth(self.caller),
        )
        self.assertEqual(failed.status_code, 503)

        first_call = CallSession.objects.get()
        self.assertEqual(first_call.status, CallSession.Status.FAILED)
        self.assertEqual(first_call.end_reason, "push_unavailable")
        self.assertTrue(
            Message.objects.filter(client_id=f"call:{first_call.pk}").exists()
        )

        # A later valid device registration must be able to call immediately;
        # the failed push attempt must not leave either user falsely busy.
        push.return_value = 1
        retry = self.client.post(
            self.create_url(),
            data={"conversation_id": self.conversation.pk, "kind": "video"},
            content_type="application/json",
            **self.auth(self.caller),
        )
        self.assertEqual(retry.status_code, 201)
        self.assertEqual(retry.json()["status"], "ringing")
        state_push.assert_not_called()

    @patch("accounts.calls.send_call_state_push")
    def test_terminal_rest_action_creates_exactly_one_durable_history_event(self, state_push):
        call = CallSession.objects.create(
            conversation=self.conversation,
            caller=self.caller,
            callee=self.callee,
            kind=CallSession.Kind.AUDIO,
        )
        url = f"/api/v1/calls/{call.pk}/action/"

        ended = self.client.post(
            url,
            data={"action": "cancel"},
            content_type="application/json",
            **self.auth(self.caller),
        )
        self.assertEqual(ended.status_code, 200)
        self.assertEqual(ended.json()["status"], "canceled")

        retry = self.client.post(
            url,
            data={"action": "cancel"},
            content_type="application/json",
            **self.auth(self.caller),
        )
        self.assertEqual(retry.status_code, 200)

        history = Message.objects.filter(client_id=f"call:{call.pk}")
        self.assertEqual(history.count(), 1)
        self.assertEqual(history.get().body, "Voice call · Canceled")
        state_push.assert_called_once_with(str(call.pk), self.callee.pk)
