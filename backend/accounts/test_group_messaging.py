from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .messaging_models import GroupMembership, GroupReadState
from .models import Conversation, Message, User, UserBlock


class GroupMessagingTests(APITestCase):
    def setUp(self):
        self.me = self.user("me")
        self.alice = self.user("alice")
        self.bob = self.user("bob")
        self.cara = self.user("cara")
        self.outsider = self.user("outsider")

    def user(self, username):
        return User.objects.create_user(
            email=f"{username}@example.com",
            username=username,
            name=username.title(),
            password="StrongNovaPass2026!",
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def create_group(self, title="Nova Crew", usernames=None):
        self.auth(self.me)
        response = self.client.post(
            reverse("group-conversation-create"),
            {
                "title": title,
                "usernames": usernames or [self.alice.username, self.bob.username],
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)
        return Conversation.objects.get(pk=response.data["id"]), response

    def send(self, conversation, sender, body="hello", client_id="group-test-1"):
        self.auth(sender)
        return self.client.post(
            reverse("conversation-messages", kwargs={"conversation_id": conversation.pk}),
            {"body": body, "client_id": client_id},
            format="json",
        )

    def test_create_group_sets_owner_and_members(self):
        conversation, response = self.create_group()

        self.assertEqual(conversation.kind, Conversation.Kind.GROUP)
        self.assertIsNone(conversation.participant_one_id)
        self.assertIsNone(conversation.participant_two_id)
        self.assertEqual(response.data["kind"], "group")
        self.assertEqual(response.data["title"], "Nova Crew")
        self.assertEqual(response.data["members_count"], 3)
        self.assertEqual(response.data["current_user_role"], "owner")
        self.assertEqual(conversation.group_memberships.count(), 3)
        self.assertEqual(
            GroupMembership.objects.get(conversation=conversation, user=self.me).role,
            GroupMembership.Role.OWNER,
        )

    def test_group_requires_three_people_and_respects_block(self):
        self.auth(self.me)
        too_small = self.client.post(
            reverse("group-conversation-create"),
            {"title": "Two", "usernames": [self.alice.username]},
            format="json",
        )
        self.assertEqual(too_small.status_code, status.HTTP_400_BAD_REQUEST)

        UserBlock.objects.create(blocker=self.me, blocked=self.bob)
        blocked = self.client.post(
            reverse("group-conversation-create"),
            {
                "title": "Blocked",
                "usernames": [self.alice.username, self.bob.username],
            },
            format="json",
        )
        self.assertEqual(blocked.status_code, status.HTTP_403_FORBIDDEN)

    def test_unified_inbox_contains_direct_and_group(self):
        group, _ = self.create_group()
        self.auth(self.me)
        direct = self.client.post(
            reverse("conversations"),
            {"username": self.cara.username},
            format="json",
        )
        self.assertEqual(direct.status_code, status.HTTP_201_CREATED)

        inbox = self.client.get(reverse("conversations"))
        self.assertEqual(inbox.status_code, status.HTTP_200_OK)
        by_id = {item["id"]: item for item in inbox.data["results"]}
        self.assertEqual(by_id[group.pk]["kind"], "group")
        self.assertEqual(by_id[group.pk]["title"], "Nova Crew")
        self.assertEqual(by_id[direct.data["id"]]["kind"], "direct")
        self.assertIsNotNone(by_id[direct.data["id"]]["other_user"])

    def test_group_uses_existing_message_endpoint_and_per_member_read_cursor(self):
        group, _ = self.create_group()
        sent = self.send(group, self.me, body="hello group")
        self.assertEqual(sent.status_code, status.HTTP_201_CREATED, sent.data)
        message = Message.objects.get(pk=sent.data["id"])
        self.assertIsNone(message.recipient_id)

        self.auth(self.alice)
        inbox = self.client.get(reverse("conversations"))
        group_row = next(item for item in inbox.data["results"] if item["id"] == group.pk)
        self.assertEqual(group_row["unread_count"], 1)

        messages = self.client.get(
            reverse("conversation-messages", kwargs={"conversation_id": group.pk})
        )
        self.assertEqual(messages.status_code, status.HTTP_200_OK)
        self.assertEqual(messages.data["results"][-1]["body"], "hello group")

        read = self.client.post(
            reverse("conversation-read", kwargs={"conversation_id": group.pk}),
            {},
            format="json",
        )
        self.assertEqual(read.status_code, status.HTTP_200_OK)
        self.assertEqual(read.data["unread_count"], 0)
        state = GroupReadState.objects.get(conversation=group, user=self.alice)
        self.assertEqual(state.last_read_message_id, message.pk)

    def test_outsider_cannot_read_or_send_group_messages(self):
        group, _ = self.create_group()
        self.auth(self.outsider)
        read = self.client.get(
            reverse("conversation-messages", kwargs={"conversation_id": group.pk})
        )
        self.assertEqual(read.status_code, status.HTTP_404_NOT_FOUND)
        send = self.client.post(
            reverse("conversation-messages", kwargs={"conversation_id": group.pk}),
            {"body": "nope", "client_id": "outsider-1"},
            format="json",
        )
        self.assertEqual(send.status_code, status.HTTP_404_NOT_FOUND)

    def test_new_member_does_not_inherit_old_unread_history(self):
        group, _ = self.create_group()
        self.send(group, self.me, body="before cara", client_id="before-cara")

        self.auth(self.me)
        added = self.client.post(
            reverse("group-members", kwargs={"conversation_id": group.pk}),
            {"usernames": [self.cara.username]},
            format="json",
        )
        self.assertEqual(added.status_code, status.HTTP_200_OK, added.data)

        self.auth(self.cara)
        inbox = self.client.get(reverse("conversations"))
        row = next(item for item in inbox.data["results"] if item["id"] == group.pk)
        self.assertEqual(row["unread_count"], 0)

        second = self.send(group, self.alice, body="after cara", client_id="after-cara")
        self.assertEqual(second.status_code, status.HTTP_201_CREATED)
        self.auth(self.cara)
        inbox = self.client.get(reverse("conversations"))
        row = next(item for item in inbox.data["results"] if item["id"] == group.pk)
        self.assertEqual(row["unread_count"], 1)

    def test_owner_leaving_transfers_ownership(self):
        group, _ = self.create_group()
        self.auth(self.me)
        left = self.client.delete(
            reverse(
                "group-member-detail",
                kwargs={"conversation_id": group.pk, "username": self.me.username},
            )
        )
        self.assertEqual(left.status_code, status.HTTP_200_OK, left.data)
        self.assertTrue(left.data["left"])
        self.assertFalse(GroupMembership.objects.filter(conversation=group, user=self.me).exists())
        owners = GroupMembership.objects.filter(
            conversation=group,
            role=GroupMembership.Role.OWNER,
        )
        self.assertEqual(owners.count(), 1)

    def test_group_call_is_rejected_without_touching_direct_call_flow(self):
        group, _ = self.create_group()
        self.auth(self.me)
        response = self.client.post(
            reverse("call-create"),
            {"conversation_id": group.pk, "kind": "audio"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("Group calls", response.data["detail"])

    def test_blocked_member_activity_is_hidden_inside_shared_group(self):
        group, _ = self.create_group()
        sent = self.send(group, self.alice, body="hidden", client_id="hidden-blocked")
        self.assertEqual(sent.status_code, status.HTTP_201_CREATED)

        UserBlock.objects.create(blocker=self.me, blocked=self.alice)
        self.auth(self.me)
        messages = self.client.get(
            reverse("conversation-messages", kwargs={"conversation_id": group.pk})
        )
        self.assertEqual(messages.status_code, status.HTTP_200_OK)
        self.assertEqual(messages.data["results"], [])
        inbox = self.client.get(reverse("conversations"))
        row = next(item for item in inbox.data["results"] if item["id"] == group.pk)
        self.assertEqual(row["unread_count"], 0)
