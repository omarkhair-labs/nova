import base64
import shutil
import tempfile

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .messaging_models import GroupMembership
from .models import Conversation, Follow, Message, Post, User
from .privacy_models import AccountPrivacy
from .sharing_models import MessageShare


class SocialIntegrationTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-social-integration-media-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.me = self.user("me")
        self.alice = self.user("alice")
        self.bob = self.user("bob")
        self.author = self.user("author")
        self.outsider = self.user("outsider")
        self.group = self.create_group()

    def user(self, username):
        return User.objects.create_user(
            email=f"{username}@example.com",
            username=username,
            name=username.title(),
            password="StrongNovaPass2026!",
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def image(self, name="integration.png"):
        png = base64.b64decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
        )
        return SimpleUploadedFile(name, png, content_type="image/png")

    def post(self, author=None):
        return Post.objects.create(
            author=author or self.author,
            image=self.image(f"post-{Post.objects.count()}.png"),
            caption="Shared into a group",
        )

    def create_group(self):
        conversation = Conversation.objects.create(
            kind=Conversation.Kind.GROUP,
            title="Nova Crew",
            created_by=self.me,
        )
        GroupMembership.objects.create(
            conversation=conversation,
            user=self.me,
            role=GroupMembership.Role.OWNER,
        )
        GroupMembership.objects.create(
            conversation=conversation,
            user=self.alice,
            role=GroupMembership.Role.MEMBER,
        )
        GroupMembership.objects.create(
            conversation=conversation,
            user=self.bob,
            role=GroupMembership.Role.MEMBER,
        )
        return conversation

    def test_post_share_to_group_reuses_real_group_message(self):
        post = self.post()
        self.auth(self.me)

        response = self.client.post(
            reverse("message-share"),
            {
                "conversation_id": self.group.pk,
                "kind": "post",
                "post_id": post.pk,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        self.assertEqual(response.data["conversation"]["kind"], "group")
        message = Message.objects.get(pk=response.data["message"]["id"])
        self.assertEqual(message.conversation_id, self.group.pk)
        self.assertIsNone(message.recipient_id)
        self.assertEqual(message.sender_id, self.me.pk)
        self.assertEqual(message.shared_content.kind, MessageShare.Kind.POST)
        self.assertEqual(message.shared_content.post_id, post.pk)
        self.assertEqual(response.data["message"]["share"]["post"]["id"], post.pk)

        self.auth(self.alice)
        history = self.client.get(
            reverse("conversation-messages", kwargs={"conversation_id": self.group.pk})
        )
        self.assertEqual(history.status_code, status.HTTP_200_OK)
        self.assertEqual(history.data["results"][0]["share"]["post"]["id"], post.pk)

    def test_profile_share_to_group_reuses_real_group_message(self):
        self.auth(self.me)

        response = self.client.post(
            reverse("message-share"),
            {
                "conversation_id": self.group.pk,
                "kind": "profile",
                "profile_username": self.author.username,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        message = Message.objects.get(pk=response.data["message"]["id"])
        self.assertIsNone(message.recipient_id)
        self.assertEqual(message.shared_content.kind, MessageShare.Kind.PROFILE)
        self.assertEqual(message.shared_content.profile_id, self.author.pk)
        self.assertEqual(
            response.data["message"]["share"]["profile"]["username"],
            self.author.username,
        )

    def test_outsider_cannot_share_into_group_by_conversation_id(self):
        post = self.post()
        self.auth(self.outsider)

        response = self.client.post(
            reverse("message-share"),
            {
                "conversation_id": self.group.pk,
                "kind": "post",
                "post_id": post.pk,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertFalse(MessageShare.objects.exists())

    def test_private_post_group_share_requires_every_visible_member_to_have_access(self):
        post = self.post()
        AccountPrivacy.objects.create(user=self.author, is_private=True)
        Follow.objects.create(follower=self.me, following=self.author)
        Follow.objects.create(follower=self.alice, following=self.author)
        # Bob is intentionally not an approved follower of the private author.

        self.auth(self.me)
        response = self.client.post(
            reverse("message-share"),
            {
                "conversation_id": self.group.pk,
                "kind": "post",
                "post_id": post.pk,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertIn("isn't available to everyone", response.data["detail"])
        self.assertFalse(MessageShare.objects.exists())

        Follow.objects.create(follower=self.bob, following=self.author)
        allowed = self.client.post(
            reverse("message-share"),
            {
                "conversation_id": self.group.pk,
                "kind": "post",
                "post_id": post.pk,
            },
            format="json",
        )
        self.assertEqual(allowed.status_code, status.HTTP_201_CREATED, allowed.data)
