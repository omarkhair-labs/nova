from datetime import timedelta

from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from ..models import Follow, User, UserBlock
from ..pulse_models import Pulse


class PulseChainApiTests(APITestCase):
    def setUp(self):
        self.me = User.objects.create_user(
            email="chain-me@example.com",
            username="chain_me",
            password="StrongNovaPass2026!",
            name="Me",
        )
        self.friend = User.objects.create_user(
            email="chain-friend@example.com",
            username="chain_friend",
            password="StrongNovaPass2026!",
            name="Friend",
        )
        Follow.objects.create(follower=self.me, following=self.friend)

    def authenticate(self, user):
        self.client.force_authenticate(user=user)

    def root_pulse(self, **overrides):
        values = {
            "author": self.friend,
            "media_type": Pulse.MediaType.TEXT,
            "note": "Root moment",
            "expires_at": timezone.now() + timedelta(hours=1),
        }
        values.update(overrides)
        return Pulse.objects.create(**values)

    def test_reply_requires_a_live_visible_parent(self):
        stranger = User.objects.create_user(
            email="chain-stranger@example.com",
            username="chain_stranger",
            password="StrongNovaPass2026!",
        )
        hidden = Pulse.objects.create(
            author=stranger,
            media_type=Pulse.MediaType.TEXT,
            note="Hidden",
            expires_at=timezone.now() + timedelta(hours=1),
        )
        expired = self.root_pulse(
            note="Expired",
            expires_at=timezone.now() - timedelta(seconds=1),
        )
        self.authenticate(self.me)

        hidden_response = self.client.post(
            reverse("pulse-reply", kwargs={"pulse_id": hidden.pk}),
            {"note": "Nope"},
            format="json",
        )
        expired_response = self.client.post(
            reverse("pulse-reply", kwargs={"pulse_id": expired.pk}),
            {"note": "Too late"},
            format="json",
        )

        self.assertEqual(hidden_response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(expired_response.status_code, status.HTTP_404_NOT_FOUND)

    def test_reply_is_a_fresh_pulse_linked_to_root_and_visible_to_parent_author(self):
        root = self.root_pulse()
        self.authenticate(self.me)
        response = self.client.post(
            reverse("pulse-reply", kwargs={"pulse_id": root.pk}),
            {"note": "My moment back", "audience": "followers"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        reply = Pulse.objects.get(pk=response.data["id"])
        self.assertEqual(reply.reply_to_id, root.pk)
        self.assertEqual(reply.chain_root_id, root.pk)
        self.assertGreater(reply.expires_at, root.expires_at)
        self.assertEqual(response.data["reply_to_id"], root.pk)
        self.assertEqual(response.data["chain_root_id"], root.pk)

        # The parent author sees a direct moment reply even without following back.
        self.authenticate(self.friend)
        detail = self.client.get(reverse("pulse-detail", kwargs={"pulse_id": reply.pk}))
        self.assertEqual(detail.status_code, status.HTTP_200_OK)

    def test_nested_reply_keeps_original_chain_root(self):
        root = self.root_pulse()
        first = Pulse.objects.create(
            author=self.me,
            reply_to=root,
            media_type=Pulse.MediaType.TEXT,
            note="First reply",
            expires_at=timezone.now() + timedelta(hours=2),
        )

        self.authenticate(self.friend)
        response = self.client.post(
            reverse("pulse-reply", kwargs={"pulse_id": first.pk}),
            {"note": "Back again"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        nested = Pulse.objects.get(pk=response.data["id"])
        self.assertEqual(nested.reply_to_id, first.pk)
        self.assertEqual(nested.chain_root_id, root.pk)

    def test_chain_returns_only_members_visible_to_requester(self):
        root = self.root_pulse()
        mine = Pulse.objects.create(
            author=self.me,
            reply_to=root,
            media_type=Pulse.MediaType.TEXT,
            note="Visible reply",
            expires_at=timezone.now() + timedelta(hours=2),
        )
        stranger = User.objects.create_user(
            email="chain-outsider@example.com",
            username="chain_outsider",
            password="StrongNovaPass2026!",
        )
        Follow.objects.create(follower=stranger, following=self.friend)
        hidden = Pulse.objects.create(
            author=stranger,
            reply_to=root,
            media_type=Pulse.MediaType.TEXT,
            note="Not in my orbit",
            expires_at=timezone.now() + timedelta(hours=2),
        )

        self.authenticate(self.me)
        response = self.client.get(reverse("pulse-chain", kwargs={"pulse_id": mine.pk}))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["root_id"], root.pk)
        ids = [item["id"] for item in response.data["results"]]
        self.assertEqual(ids, [root.pk, mine.pk])
        self.assertNotIn(hidden.pk, ids)

    def test_block_policy_overrides_direct_reply_visibility(self):
        root = self.root_pulse()
        reply = Pulse.objects.create(
            author=self.me,
            reply_to=root,
            media_type=Pulse.MediaType.TEXT,
            note="Reply",
            expires_at=timezone.now() + timedelta(hours=2),
        )
        UserBlock.objects.create(blocker=self.friend, blocked=self.me)

        self.authenticate(self.friend)
        response = self.client.get(reverse("pulse-detail", kwargs={"pulse_id": reply.pk}))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_deleting_root_does_not_delete_other_users_replies(self):
        root = self.root_pulse()
        reply = Pulse.objects.create(
            author=self.me,
            reply_to=root,
            media_type=Pulse.MediaType.TEXT,
            note="I own this reply",
            expires_at=timezone.now() + timedelta(hours=2),
        )

        root.delete()
        reply.refresh_from_db()

        self.assertIsNone(reply.reply_to_id)
        self.assertIsNone(reply.chain_root_id)
        self.assertTrue(Pulse.objects.filter(pk=reply.pk).exists())
