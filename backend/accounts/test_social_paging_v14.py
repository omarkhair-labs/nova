from datetime import timedelta

from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Conversation, Follow, Post, User, UserBlock


class SocialPagingV14Tests(APITestCase):
    def setUp(self):
        self.viewer = User.objects.create_user(
            email="viewer@example.com",
            username="viewer",
            password="ViewerPass123!",
            name="Viewer",
        )
        self.people = [
            User.objects.create_user(
                email=f"person{i:02d}@example.com",
                username=f"person{i:02d}",
                password="PersonPass123!",
                name=f"Person {i:02d}",
            )
            for i in range(55)
        ]
        self.client.force_authenticate(user=self.viewer)

    def test_people_cursor_pages_do_not_overlap(self):
        first = self.client.get(reverse("people"))
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(len(first.data["results"]), 24)
        self.assertIsNotNone(first.data["next_cursor"])

        second = self.client.get(
            reverse("people"),
            {"cursor": first.data["next_cursor"]},
        )
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertEqual(len(second.data["results"]), 24)
        self.assertIsNotNone(second.data["next_cursor"])

        third = self.client.get(
            reverse("people"),
            {"cursor": second.data["next_cursor"]},
        )
        self.assertEqual(third.status_code, status.HTTP_200_OK)
        self.assertEqual(len(third.data["results"]), 7)
        self.assertIsNone(third.data["next_cursor"])

        ids = [
            item["id"]
            for response in (first, second, third)
            for item in response.data["results"]
        ]
        self.assertEqual(len(ids), 55)
        self.assertEqual(len(set(ids)), 55)

    def test_followers_and_following_are_paginated_and_respect_viewer_blocks(self):
        target = self.people[0]
        followers = self.people[1:31]
        following = self.people[31:55]
        for person in followers:
            Follow.objects.create(follower=person, following=target)
        Follow.objects.create(follower=self.viewer, following=target)
        for person in following:
            Follow.objects.create(follower=target, following=person)

        blocked_follower = followers[0]
        UserBlock.objects.create(blocker=self.viewer, blocked=blocked_follower)

        followers_response = self.client.get(
            reverse("person-followers", kwargs={"username": target.username})
        )
        self.assertEqual(followers_response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(followers_response.data["results"]), 24)
        follower_usernames = {
            item["username"] for item in followers_response.data["results"]
        }
        self.assertNotIn(blocked_follower.username, follower_usernames)
        self.assertIn("viewer", follower_usernames)
        self.assertIsNotNone(followers_response.data["next_cursor"])

        followers_next = self.client.get(
            reverse("person-followers", kwargs={"username": target.username}),
            {"cursor": followers_response.data["next_cursor"]},
        )
        all_followers = followers_response.data["results"] + followers_next.data["results"]
        self.assertEqual(len(all_followers), 30)
        self.assertEqual(len({item["id"] for item in all_followers}), 30)

        following_response = self.client.get(
            reverse("person-following-list", kwargs={"username": target.username})
        )
        self.assertEqual(following_response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(following_response.data["results"]), 24)
        self.assertIsNone(following_response.data["next_cursor"])

    def test_profile_posts_are_cursor_paginated(self):
        target = self.people[0]
        for index in range(30):
            Post.objects.create(
                author=target,
                image=f"posts/test-{index}.jpg",
                caption=f"Post {index}",
            )

        first = self.client.get(
            reverse("person-posts", kwargs={"username": target.username})
        )
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(len(first.data["results"]), 24)
        self.assertIsNotNone(first.data["next_cursor"])

        second = self.client.get(
            reverse("person-posts", kwargs={"username": target.username}),
            {"cursor": first.data["next_cursor"]},
        )
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertEqual(len(second.data["results"]), 6)
        self.assertIsNone(second.data["next_cursor"])
        ids = [item["id"] for item in first.data["results"] + second.data["results"]]
        self.assertEqual(len(set(ids)), 30)

    def test_conversation_inbox_uses_composite_cursor(self):
        now = timezone.now()
        conversation_ids = []
        for index, person in enumerate(self.people[:35]):
            first_id, second_id = sorted((self.viewer.pk, person.pk))
            conversation = Conversation.objects.create(
                participant_one_id=first_id,
                participant_two_id=second_id,
            )
            updated_at = now - timedelta(minutes=index)
            Conversation.objects.filter(pk=conversation.pk).update(updated_at=updated_at)
            conversation_ids.append(conversation.pk)

        first = self.client.get(reverse("conversations"))
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(len(first.data["results"]), 30)
        self.assertIsNotNone(first.data["next_cursor"])

        second = self.client.get(
            reverse("conversations"),
            {"cursor": first.data["next_cursor"]},
        )
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertEqual(len(second.data["results"]), 5)
        self.assertIsNone(second.data["next_cursor"])

        returned_ids = [
            item["id"] for item in first.data["results"] + second.data["results"]
        ]
        self.assertEqual(set(returned_ids), set(conversation_ids))
        self.assertEqual(len(returned_ids), len(set(returned_ids)))

    def test_invalid_cursors_are_rejected(self):
        bad_people = self.client.get(reverse("people"), {"cursor": "bad"})
        self.assertEqual(bad_people.status_code, status.HTTP_400_BAD_REQUEST)

        bad_inbox = self.client.get(reverse("conversations"), {"cursor": "bad"})
        self.assertEqual(bad_inbox.status_code, status.HTTP_400_BAD_REQUEST)
