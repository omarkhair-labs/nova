from datetime import timedelta

from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .comment_reply_models import PostCommentLike
from .messaging_models import GroupMembership
from .models import Comment, Conversation, Message, Post, User
from .privacy_models import AccountPrivacy, FollowRequest, NotificationPreference
from .auth_session_models import AuthSessionRecord
from .memory_models import MemoryDraft
from .pulse_models import Pulse, PulseReaction, PulseView
from .room_models import RoomFollow, RoomItem, RoomProfile, RoomReminder


class ApprovedVisualCapabilityTests(APITestCase):
    def setUp(self):
        self.me = self.user("maya")
        self.friend = self.user("aisha")
        self.auth(self.me)

    def user(self, stem):
        return User.objects.create_user(
            email=f"{stem}@visual.example.com",
            username=f"{stem}_visual",
            name=stem.title(),
            password="StrongNovaPass2026!",
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def test_profile_metadata_round_trips_through_production_user_endpoint(self):
        response = self.client.patch(
            reverse("me"),
            {
                "bio": "Collecting moments, not things.",
                "location": "Seoul, KR",
                "link": "https://nova.example/maya",
                "interests": ["Photography", "Travel"],
                "profile_theme": "cyan",
                "show_orbit": False,
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["location"], "Seoul, KR")
        self.assertEqual(response.data["interests"], ["Photography", "Travel"])
        self.assertFalse(response.data["show_orbit"])

    def test_privacy_and_notification_preferences_persist(self):
        privacy = self.client.post(
            reverse("account-privacy"),
            {
                "show_activity_status": False,
                "send_read_receipts": False,
                "story_audience": "close_friends",
            },
            format="json",
        )
        self.assertEqual(privacy.status_code, status.HTTP_200_OK)
        self.assertFalse(privacy.data["show_activity_status"])
        self.assertEqual(privacy.data["story_audience"], "close_friends")
        self.assertFalse(AccountPrivacy.objects.get(user=self.me).send_read_receipts)

        preferences = self.client.post(
            reverse("notification-preferences"),
            {"messages": False, "live_sessions": False},
            format="json",
        )
        self.assertEqual(preferences.status_code, status.HTTP_200_OK)
        stored = NotificationPreference.objects.get(user=self.me)
        self.assertFalse(stored.messages)
        self.assertFalse(stored.live_sessions)

    def test_sent_follow_requests_are_exposed_to_requester(self):
        row = FollowRequest.objects.create(requester=self.me, target=self.friend)
        response = self.client.get(reverse("sent-follow-requests"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["id"], row.pk)
        self.assertEqual(response.data["results"][0]["target"]["username"], self.friend.username)

    def test_pulse_category_is_persisted_and_filterable(self):
        Pulse.objects.create(
            author=self.me,
            note="Late-night mix",
            media_type=Pulse.MediaType.TEXT,
            category=Pulse.Category.MUSIC,
            expires_at=timezone.now() + timedelta(hours=1),
        )
        response = self.client.get(reverse("pulse-feed"), {"category": "music"})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["category"], "music")
        empty = self.client.get(reverse("pulse-feed"), {"category": "talks"})
        self.assertEqual(empty.data["results"], [])

    def test_pulse_view_and_reaction_are_persisted_and_serialized(self):
        pulse = Pulse.objects.create(
            author=self.me,
            note="Live city lights",
            media_type=Pulse.MediaType.TEXT,
            category=Pulse.Category.LIVE,
            expires_at=timezone.now() + timedelta(hours=1),
        )
        viewed = self.client.post(reverse("pulse-view", args=[pulse.pk]), {}, format="json")
        reacted = self.client.post(
            reverse("pulse-reaction", args=[pulse.pk]),
            {"enabled": True},
            format="json",
        )
        self.assertEqual(viewed.status_code, status.HTTP_200_OK)
        self.assertEqual(reacted.status_code, status.HTTP_200_OK)
        self.assertEqual(reacted.data["viewers_count"], 1)
        self.assertEqual(reacted.data["reactions_count"], 1)
        self.assertTrue(reacted.data["is_reacted"])
        self.assertTrue(PulseView.objects.filter(pulse=pulse, user=self.me).exists())
        self.assertTrue(PulseReaction.objects.filter(pulse=pulse, user=self.me).exists())

    def test_comment_like_is_real_and_idempotently_toggleable(self):
        post = Post.objects.create(
            author=self.friend,
            image=SimpleUploadedFile("visual.jpg", b"visual", content_type="image/jpeg"),
        )
        comment = Comment.objects.create(post=post, author=self.friend, body="City lights")
        url = reverse("comment-like", args=[comment.pk])
        liked = self.client.post(url, {}, format="json")
        self.assertEqual(liked.status_code, status.HTTP_200_OK)
        self.assertTrue(liked.data["is_liked"])
        self.assertTrue(PostCommentLike.objects.filter(comment=comment, user=self.me).exists())
        unliked = self.client.delete(url)
        self.assertFalse(unliked.data["is_liked"])

    def test_room_reminder_is_persisted_for_scheduled_plan(self):
        room = Conversation.objects.create(
            kind=Conversation.Kind.GROUP,
            title="Night Owls",
            created_by=self.me,
        )
        GroupMembership.objects.create(
            conversation=room,
            user=self.me,
            role=GroupMembership.Role.OWNER,
        )
        item = RoomItem.objects.create(
            conversation=room,
            created_by=self.me,
            kind=RoomItem.Kind.PLAN,
            title="City walk",
            scheduled_for=timezone.now() + timedelta(hours=2),
        )
        url = reverse("room-reminder", args=[room.pk, item.pk])
        enabled = self.client.post(url, {}, format="json")
        self.assertEqual(enabled.status_code, status.HTTP_200_OK)
        self.assertTrue(enabled.data["reminder_set"])
        self.assertTrue(RoomReminder.objects.filter(item=item, user=self.me).exists())
        disabled = self.client.delete(url)
        self.assertFalse(disabled.data["reminder_set"])

    def test_public_room_can_be_discovered_followed_and_joined(self):
        room = Conversation.objects.create(
            kind=Conversation.Kind.GROUP,
            title="Design Circle",
            created_by=self.friend,
        )
        GroupMembership.objects.create(
            conversation=room,
            user=self.friend,
            role=GroupMembership.Role.OWNER,
        )
        profile = RoomProfile.objects.create(
            conversation=room,
            description="A public place for design critique.",
            is_public=True,
            topics=["Design", "Feedback"],
        )
        discovered = self.client.get(reverse("rooms"), {"view": "discover"})
        self.assertEqual(discovered.status_code, status.HTTP_200_OK)
        self.assertEqual(discovered.data["rooms"][0]["conversation"]["id"], room.pk)
        followed = self.client.post(reverse("room-follow", args=[room.pk]), {}, format="json")
        self.assertTrue(followed.data["is_following"])
        self.assertTrue(RoomFollow.objects.filter(user=self.me, room=profile).exists())
        joined = self.client.post(reverse("room-membership", args=[room.pk]), {}, format="json")
        self.assertTrue(joined.data["is_member"])
        self.assertTrue(GroupMembership.objects.filter(conversation=room, user=self.me).exists())

    def test_memory_draft_is_real_private_crud(self):
        created = self.client.post(
            reverse("memory-drafts"),
            {
                "kind": "film",
                "title": "Seoul nights",
                "note": "Keep the city-light moments.",
                "media": SimpleUploadedFile("night.jpg", b"visual", content_type="image/jpeg"),
            },
            format="multipart",
        )
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        draft = MemoryDraft.objects.get(pk=created.data["id"])
        self.assertEqual(draft.user, self.me)
        listed = self.client.get(reverse("memory-drafts"))
        self.assertEqual(listed.data["drafts"][0]["title"], "Seoul nights")
        removed = self.client.delete(reverse("memory-draft-detail", args=[draft.pk]))
        self.assertEqual(removed.status_code, status.HTTP_204_NO_CONTENT)

    def test_login_activity_records_and_revokes_individual_devices(self):
        self.client.force_authenticate(user=None)
        first = self.client.post(
            reverse("login"),
            {
                "email": self.me.email,
                "password": "StrongNovaPass2026!",
                "device_name": "Pixel 10",
                "platform": "android",
            },
            format="json",
        )
        second = self.client.post(
            reverse("login"),
            {
                "email": self.me.email,
                "password": "StrongNovaPass2026!",
                "device_name": "Galaxy S26",
                "platform": "android",
            },
            format="json",
        )
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {second.data['access']}")
        sessions = self.client.get(reverse("session-list"))
        self.assertEqual(sessions.status_code, status.HTTP_200_OK)
        self.assertEqual(len(sessions.data["sessions"]), 2)
        other = next(row for row in sessions.data["sessions"] if not row["is_current"])
        revoked = self.client.delete(reverse("session-detail", args=[other["id"]]))
        self.assertEqual(revoked.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(AuthSessionRecord.objects.get(session_key=other["id"]).is_active)

    def test_inbox_filters_are_server_backed(self):
        direct = Conversation.objects.create(
            kind=Conversation.Kind.DIRECT,
            participant_one=min(self.me, self.friend, key=lambda user: user.id),
            participant_two=max(self.me, self.friend, key=lambda user: user.id),
        )
        Message.objects.create(
            conversation=direct,
            sender=self.friend,
            recipient=self.me,
            body=f"Hi @{self.me.username}",
            client_id="approved-mention",
        )
        unread = self.client.get(reverse("conversations"), {"filter": "unread"})
        mentions = self.client.get(reverse("conversations"), {"filter": "mentions"})
        self.assertEqual(unread.status_code, status.HTTP_200_OK)
        self.assertEqual(unread.data["results"][0]["id"], direct.pk)
        self.assertEqual(mentions.data["results"][0]["id"], direct.pk)
