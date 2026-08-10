from io import BytesIO

from PIL import Image
from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .messaging_models import GroupConversationProfile, GroupMembership
from .models import Conversation, User


class GroupManagementTests(APITestCase):
    def setUp(self):
        self.owner = self.user("owner")
        self.alice = self.user("alice")
        self.bob = self.user("bob")
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

    def create_group(self):
        self.auth(self.owner)
        response = self.client.post(
            reverse("group-conversation-create"),
            {
                "title": "Nova Crew",
                "usernames": [self.alice.username, self.bob.username],
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        return Conversation.objects.get(pk=response.data["id"])

    def image_upload(self, name="group.png"):
        buffer = BytesIO()
        Image.new("RGB", (32, 32), (20, 40, 80)).save(buffer, format="PNG")
        return SimpleUploadedFile(name, buffer.getvalue(), content_type="image/png")

    def test_owner_can_rename_upload_and_remove_group_photo(self):
        self.auth(self.owner)
        manage_url = reverse(
            "group-management-detail",
            kwargs={"conversation_id": self.group.pk},
        )
        response = self.client.post(
            manage_url,
            {"title": "Design Crew", "avatar": self.image_upload()},
            format="multipart",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.group.refresh_from_db()
        self.assertEqual(self.group.title, "Design Crew")
        self.assertTrue(response.data["conversation"]["group_avatar_url"])
        self.assertTrue(
            GroupConversationProfile.objects.get(conversation=self.group).avatar.name
        )

        inbox = self.client.get(reverse("conversations"))
        row = next(item for item in inbox.data["results"] if item["id"] == self.group.pk)
        self.assertTrue(row["group_avatar_url"])

        removed = self.client.post(
            manage_url,
            {"remove_avatar": True},
            format="json",
        )
        self.assertEqual(removed.status_code, status.HTTP_200_OK, removed.data)
        self.assertEqual(removed.data["conversation"]["group_avatar_url"], "")

    def test_owner_can_promote_and_demote_admin(self):
        self.auth(self.owner)
        role_url = reverse(
            "group-member-role",
            kwargs={
                "conversation_id": self.group.pk,
                "username": self.alice.username,
            },
        )
        promoted = self.client.post(role_url, {"role": "admin"}, format="json")
        self.assertEqual(promoted.status_code, status.HTTP_200_OK, promoted.data)
        membership = GroupMembership.objects.get(
            conversation=self.group,
            user=self.alice,
        )
        self.assertEqual(membership.role, GroupMembership.Role.ADMIN)

        demoted = self.client.post(role_url, {"role": "member"}, format="json")
        self.assertEqual(demoted.status_code, status.HTTP_200_OK, demoted.data)
        membership.refresh_from_db()
        self.assertEqual(membership.role, GroupMembership.Role.MEMBER)

    def test_admin_can_edit_appearance_but_cannot_manage_admin_roles(self):
        GroupMembership.objects.filter(
            conversation=self.group,
            user=self.alice,
        ).update(role=GroupMembership.Role.ADMIN)
        self.auth(self.alice)
        manage_url = reverse(
            "group-management-detail",
            kwargs={"conversation_id": self.group.pk},
        )
        renamed = self.client.post(manage_url, {"title": "Alice Crew"}, format="json")
        self.assertEqual(renamed.status_code, status.HTTP_200_OK, renamed.data)

        role_url = reverse(
            "group-member-role",
            kwargs={"conversation_id": self.group.pk, "username": self.bob.username},
        )
        denied = self.client.post(role_url, {"role": "admin"}, format="json")
        self.assertEqual(denied.status_code, status.HTTP_403_FORBIDDEN)

    def test_member_and_outsider_cannot_edit_group(self):
        manage_url = reverse(
            "group-management-detail",
            kwargs={"conversation_id": self.group.pk},
        )
        self.auth(self.bob)
        member_denied = self.client.post(manage_url, {"title": "Nope"}, format="json")
        self.assertEqual(member_denied.status_code, status.HTTP_403_FORBIDDEN)

        self.auth(self.outsider)
        outsider_denied = self.client.get(manage_url)
        self.assertEqual(outsider_denied.status_code, status.HTTP_404_NOT_FOUND)
