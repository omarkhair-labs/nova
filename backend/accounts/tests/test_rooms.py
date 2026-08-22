from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from ..messaging_models import GroupMembership
from ..models import Conversation, User, UserBlock
from ..room_models import RoomItem, RoomProfile


class RoomsApiTests(APITestCase):
    def setUp(self):
        self.owner = self.user("owner")
        self.member = self.user("member")
        self.admin = self.user("admin")
        self.outsider = self.user("outsider")
        self.room = self.group("Night crew", self.owner, self.member, self.admin)
        GroupMembership.objects.filter(
            conversation=self.room,
            user=self.admin,
        ).update(role=GroupMembership.Role.ADMIN)

    def user(self, stem):
        return User.objects.create_user(
            email=f"{stem}@rooms.example.com",
            username=f"{stem}_rooms",
            password="StrongNovaPass2026!",
            name=stem.title(),
        )

    def group(self, title, owner, *members):
        conversation = Conversation.objects.create(
            kind=Conversation.Kind.GROUP,
            title=title,
            created_by=owner,
        )
        GroupMembership.objects.create(
            conversation=conversation,
            user=owner,
            role=GroupMembership.Role.OWNER,
        )
        for member in members:
            GroupMembership.objects.create(
                conversation=conversation,
                user=member,
                role=GroupMembership.Role.MEMBER,
            )
        return conversation

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def test_room_list_requires_authentication(self):
        response = self.client.get(reverse("rooms"))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_room_list_is_existing_group_memberships_not_a_parallel_membership_system(self):
        other = self.group("Other", self.outsider)
        Conversation.objects.create(
            kind=Conversation.Kind.DIRECT,
            participant_one=self.owner,
            participant_two=self.outsider,
        )
        self.auth(self.member)

        response = self.client.get(reverse("rooms"))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = {row["conversation"]["id"] for row in response.data["rooms"]}
        self.assertEqual(ids, {self.room.pk})
        self.assertNotIn(other.pk, ids)

    def test_room_detail_reuses_group_detail_and_adds_room_sections(self):
        RoomProfile.objects.create(conversation=self.room, description="After-hours crew")
        RoomItem.objects.create(
            conversation=self.room,
            created_by=self.owner,
            kind=RoomItem.Kind.NOTE,
            body="Meet at ten",
        )
        RoomItem.objects.create(
            conversation=self.room,
            created_by=self.member,
            kind=RoomItem.Kind.PLAN,
            title="Alexandria",
        )
        self.auth(self.member)

        response = self.client.get(reverse("room-detail", args=[self.room.pk]))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["conversation"]["id"], self.room.pk)
        self.assertEqual(response.data["room"]["description"], "After-hours crew")
        self.assertEqual(response.data["room"]["sections"]["all"], 2)
        self.assertEqual(response.data["room"]["sections"]["note"], 1)
        self.assertEqual(response.data["room"]["sections"]["plan"], 1)
        self.assertEqual(len(response.data["members"]), 3)

    def test_non_member_cannot_read_room(self):
        self.auth(self.outsider)
        response = self.client.get(reverse("room-detail", args=[self.room.pk]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_only_owner_or_admin_can_edit_room_description(self):
        url = reverse("room-detail", args=[self.room.pk])
        self.auth(self.member)
        denied = self.client.patch(url, {"description": "Nope"}, format="json")
        self.assertEqual(denied.status_code, status.HTTP_403_FORBIDDEN)

        self.auth(self.admin)
        allowed = self.client.patch(url, {"description": "Our place"}, format="json")
        self.assertEqual(allowed.status_code, status.HTTP_200_OK)
        self.assertEqual(allowed.data["room"]["description"], "Our place")
        self.assertEqual(
            RoomProfile.objects.get(conversation=self.room).description,
            "Our place",
        )

    def test_member_can_create_native_room_note_plan_and_saved_link(self):
        url = reverse("room-items", args=[self.room.pk])
        self.auth(self.member)

        note = self.client.post(url, {"kind": "note", "body": "Coffee?"}, format="json")
        plan = self.client.post(url, {"kind": "plan", "title": "Friday"}, format="json")
        saved = self.client.post(
            url,
            {"kind": "saved", "title": "Watch later", "url": "https://example.com/x"},
            format="json",
        )

        self.assertEqual(note.status_code, status.HTTP_201_CREATED)
        self.assertEqual(plan.status_code, status.HTTP_201_CREATED)
        self.assertEqual(saved.status_code, status.HTTP_201_CREATED)
        self.assertEqual(RoomItem.objects.filter(conversation=self.room).count(), 3)
        self.assertEqual(note.data["created_by"]["username"], self.member.username)

    def test_room_media_validation_keeps_photo_and_video_types_explicit(self):
        url = reverse("room-items", args=[self.room.pk])
        self.auth(self.member)

        missing = self.client.post(url, {"kind": "photo"}, format="multipart")
        wrong = self.client.post(
            url,
            {
                "kind": "photo",
                "media": SimpleUploadedFile("clip.mp4", b"video", content_type="video/mp4"),
            },
            format="multipart",
        )

        self.assertEqual(missing.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(wrong.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(RoomItem.objects.filter(conversation=self.room).count(), 0)

    def test_blocked_member_room_items_are_hidden_like_blocked_group_messages(self):
        hidden = RoomItem.objects.create(
            conversation=self.room,
            created_by=self.owner,
            kind=RoomItem.Kind.NOTE,
            body="Hidden",
        )
        visible = RoomItem.objects.create(
            conversation=self.room,
            created_by=self.admin,
            kind=RoomItem.Kind.NOTE,
            body="Visible",
        )
        UserBlock.objects.create(blocker=self.member, blocked=self.owner)
        self.auth(self.member)

        response = self.client.get(reverse("room-items", args=[self.room.pk]))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = {row["id"] for row in response.data["items"]}
        self.assertNotIn(hidden.pk, ids)
        self.assertIn(visible.pk, ids)

    def test_section_filter_and_before_pagination_do_not_duplicate_items(self):
        first = RoomItem.objects.create(
            conversation=self.room,
            created_by=self.member,
            kind=RoomItem.Kind.NOTE,
            body="First",
        )
        second = RoomItem.objects.create(
            conversation=self.room,
            created_by=self.member,
            kind=RoomItem.Kind.NOTE,
            body="Second",
        )
        RoomItem.objects.create(
            conversation=self.room,
            created_by=self.member,
            kind=RoomItem.Kind.PLAN,
            title="Different section",
        )
        self.auth(self.member)
        url = reverse("room-items", args=[self.room.pk])

        page1 = self.client.get(url, {"kind": "note", "limit": 1})
        page2 = self.client.get(
            url,
            {"kind": "note", "limit": 1, "before": page1.data["next_before"]},
        )

        self.assertEqual(page1.status_code, status.HTTP_200_OK)
        self.assertEqual(page1.data["items"][0]["id"], second.pk)
        self.assertEqual(page2.data["items"][0]["id"], first.pk)
        self.assertNotEqual(page1.data["items"][0]["id"], page2.data["items"][0]["id"])

    def test_only_admins_pin_but_creator_or_admin_can_delete(self):
        item = RoomItem.objects.create(
            conversation=self.room,
            created_by=self.member,
            kind=RoomItem.Kind.NOTE,
            body="Keep me",
        )
        url = reverse("room-item-detail", args=[self.room.pk, item.pk])

        self.auth(self.member)
        denied_pin = self.client.patch(url, {"pinned": True}, format="json")
        self.assertEqual(denied_pin.status_code, status.HTTP_403_FORBIDDEN)

        self.auth(self.admin)
        pinned = self.client.patch(url, {"pinned": True}, format="json")
        self.assertEqual(pinned.status_code, status.HTTP_200_OK)
        self.assertTrue(pinned.data["pinned"])

        self.auth(self.member)
        deleted = self.client.delete(url)
        self.assertEqual(deleted.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(RoomItem.objects.filter(pk=item.pk).exists())

    def test_deleting_group_cascades_room_profile_and_timeline(self):
        RoomProfile.objects.create(conversation=self.room, description="Temporary")
        RoomItem.objects.create(
            conversation=self.room,
            created_by=self.owner,
            kind=RoomItem.Kind.NOTE,
            body="Temporary",
        )

        self.room.delete()

        self.assertFalse(RoomProfile.objects.exists())
        self.assertFalse(RoomItem.objects.exists())
