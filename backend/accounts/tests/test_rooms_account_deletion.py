from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from ..messaging_models import GroupMembership
from ..models import Conversation, User
from ..room_models import RoomItem


class RoomAccountDeletionTests(APITestCase):
    def setUp(self):
        self.owner = User.objects.create_user(
            email="owner-delete@rooms.example.com",
            username="owner_delete_rooms",
            password="StrongNovaPass2026!",
            name="Owner",
        )
        self.member = User.objects.create_user(
            email="member-delete@rooms.example.com",
            username="member_delete_rooms",
            password="StrongNovaPass2026!",
            name="Member",
        )
        self.room = Conversation.objects.create(
            kind=Conversation.Kind.GROUP,
            title="Deletion room",
            created_by=self.owner,
        )
        GroupMembership.objects.create(
            conversation=self.room,
            user=self.owner,
            role=GroupMembership.Role.OWNER,
        )
        GroupMembership.objects.create(
            conversation=self.room,
            user=self.member,
            role=GroupMembership.Role.MEMBER,
        )

    def test_account_deletion_removes_authored_room_items_but_keeps_other_room_history(self):
        mine = RoomItem.objects.create(
            conversation=self.room,
            created_by=self.member,
            kind=RoomItem.Kind.NOTE,
            body="Delete with my account",
        )
        other = RoomItem.objects.create(
            conversation=self.room,
            created_by=self.owner,
            kind=RoomItem.Kind.NOTE,
            body="Shared history stays",
        )
        self.client.force_authenticate(user=self.member)

        response = self.client.post(
            reverse("account-delete"),
            {"current_password": "StrongNovaPass2026!"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(RoomItem.objects.filter(pk=mine.pk).exists())
        self.assertTrue(RoomItem.objects.filter(pk=other.pk).exists())
        self.assertTrue(Conversation.objects.filter(pk=self.room.pk).exists())
