from unittest.mock import patch

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Conversation, DevicePushToken, Follow, Message, User, UserBlock, UserReport


class TrustSafetyV13Tests(APITestCase):
    def setUp(self):
        self.alice = User.objects.create_user(
            email="alice@example.com",
            username="alice",
            password="AlicePass123!",
            name="Alice",
        )
        self.bob = User.objects.create_user(
            email="bob@example.com",
            username="bob",
            password="BobPass123!",
            name="Bob",
        )
        first, second = sorted((self.alice.pk, self.bob.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )

    def authenticate(self, user):
        self.client.force_authenticate(user=user)

    def test_block_removes_social_connection_and_prevents_new_contact_both_ways(self):
        Follow.objects.create(follower=self.alice, following=self.bob)
        Follow.objects.create(follower=self.bob, following=self.alice)
        self.authenticate(self.alice)

        response = self.client.post(
            reverse("person-block", kwargs={"username": self.bob.username}),
            {},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(UserBlock.objects.filter(blocker=self.alice, blocked=self.bob).exists())
        self.assertFalse(Follow.objects.filter(follower=self.alice, following=self.bob).exists())
        self.assertFalse(Follow.objects.filter(follower=self.bob, following=self.alice).exists())

        people = self.client.get(reverse("people"))
        self.assertEqual(people.status_code, status.HTTP_200_OK)
        self.assertNotIn("bob", {item["username"] for item in people.data["results"]})
        self.assertEqual(
            self.client.get(reverse("person-detail", kwargs={"username": "bob"})).status_code,
            status.HTTP_404_NOT_FOUND,
        )

        open_chat = self.client.post(reverse("conversations"), {"username": "bob"}, format="json")
        self.assertEqual(open_chat.status_code, status.HTTP_403_FORBIDDEN)
        send_message = self.client.post(
            reverse("conversation-messages", kwargs={"conversation_id": self.conversation.pk}),
            {"body": "hello", "client_id": "alice-blocked-send"},
            format="json",
        )
        self.assertEqual(send_message.status_code, status.HTTP_403_FORBIDDEN)

        with patch("accounts.call_reliability_view.send_call_push", return_value=1):
            call = self.client.post(
                reverse("call-create"),
                {"conversation_id": self.conversation.pk, "kind": "audio"},
                format="json",
            )
        self.assertEqual(call.status_code, status.HTTP_403_FORBIDDEN)

        self.authenticate(self.bob)
        reverse_send = self.client.post(
            reverse("conversation-messages", kwargs={"conversation_id": self.conversation.pk}),
            {"body": "hello", "client_id": "bob-blocked-send"},
            format="json",
        )
        self.assertEqual(reverse_send.status_code, status.HTTP_403_FORBIDDEN)

    def test_report_is_recorded_and_an_open_report_is_updated_in_place(self):
        self.authenticate(self.alice)
        url = reverse("person-report", kwargs={"username": "bob"})

        created = self.client.post(
            url,
            {"reason": "harassment", "details": "Repeated unwanted contact."},
            format="json",
        )
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        report = UserReport.objects.get(reporter=self.alice, reported=self.bob)
        self.assertEqual(report.reason, UserReport.Reason.HARASSMENT)

        updated = self.client.post(
            url,
            {"reason": "spam", "details": "Repeated links."},
            format="json",
        )
        self.assertEqual(updated.status_code, status.HTTP_200_OK)
        self.assertEqual(
            UserReport.objects.filter(reporter=self.alice, reported=self.bob).count(),
            1,
        )
        report.refresh_from_db()
        self.assertEqual(report.reason, UserReport.Reason.SPAM)
        self.assertEqual(report.details, "Repeated links.")

    def test_delete_account_requires_password_and_preserves_shared_message_history(self):
        Message.objects.create(
            conversation=self.conversation,
            sender=self.alice,
            recipient=self.bob,
            body="A message that Bob should keep.",
            client_id="before-delete",
        )
        Follow.objects.create(follower=self.alice, following=self.bob)
        DevicePushToken.objects.create(
            user=self.alice,
            token="a" * 40,
            platform="android",
            active=True,
        )
        self.authenticate(self.alice)
        url = reverse("account-delete")

        rejected = self.client.post(url, {"current_password": "wrong"}, format="json")
        self.assertEqual(rejected.status_code, status.HTTP_400_BAD_REQUEST)
        self.alice.refresh_from_db()
        self.assertTrue(self.alice.is_active)

        deleted = self.client.post(
            url,
            {"current_password": "AlicePass123!"},
            format="json",
        )
        self.assertEqual(deleted.status_code, status.HTTP_200_OK)
        self.alice.refresh_from_db()
        self.assertFalse(self.alice.is_active)
        self.assertEqual(self.alice.name, "Deleted user")
        self.assertFalse(self.alice.has_usable_password())
        self.assertFalse(DevicePushToken.objects.filter(user=self.alice).exists())
        self.assertFalse(Follow.objects.filter(follower=self.alice).exists())
        self.assertTrue(Conversation.objects.filter(pk=self.conversation.pk).exists())
        self.assertTrue(Message.objects.filter(client_id="before-delete").exists())

        self.authenticate(self.bob)
        inbox = self.client.get(reverse("conversations"))
        self.assertEqual(inbox.status_code, status.HTTP_200_OK)
        row = next(item for item in inbox.data["results"] if item["id"] == self.conversation.pk)
        self.assertEqual(row["other_user"]["name"], "Deleted user")
        self.assertEqual(row["other_user"]["username"], "deleted")
