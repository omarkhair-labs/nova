from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase


class AuthFlowTests(APITestCase):
    def setUp(self):
        self.register_url = reverse("register")
        self.login_url = reverse("login")
        self.me_url = reverse("me")
        self.payload = {
            "email": "omar@example.com",
            "password": "StrongNovaPass2026!",
            "username": "omar",
            "name": "Omar Khair",
        }

    def register(self, payload):
        return self.client.post(self.register_url, payload, format="json")

    def authenticate(self, token):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")

    def test_register_login_and_me_flow(self):
        register_response = self.register(self.payload)
        self.assertEqual(register_response.status_code, status.HTTP_201_CREATED)
        self.assertIn("access", register_response.data)
        self.assertIn("refresh", register_response.data)
        self.assertEqual(register_response.data["user"]["username"], "omar")

        self.authenticate(register_response.data["access"])
        me_response = self.client.get(self.me_url)
        self.assertEqual(me_response.status_code, status.HTTP_200_OK)
        self.assertEqual(me_response.data["email"], "omar@example.com")
        self.assertEqual(me_response.data["followers_count"], 0)
        self.assertEqual(me_response.data["following_count"], 0)

        self.client.credentials()
        login_response = self.client.post(
            self.login_url,
            {
                "email": self.payload["email"],
                "password": self.payload["password"],
            },
            format="json",
        )
        self.assertEqual(login_response.status_code, status.HTTP_200_OK)
        self.assertIn("access", login_response.data)
        self.assertEqual(login_response.data["user"]["name"], "Omar Khair")

    def test_authenticated_profile_can_be_edited(self):
        register_response = self.register(self.payload)
        self.assertEqual(register_response.status_code, status.HTTP_201_CREATED)

        self.authenticate(register_response.data["access"])
        response = self.client.patch(
            self.me_url,
            {"name": "Omar Nova", "username": "omar.nova"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["name"], "Omar Nova")
        self.assertEqual(response.data["username"], "omar.nova")
        self.assertEqual(response.data["avatar_url"], "")

    def test_duplicate_username_is_rejected(self):
        first = self.register(self.payload)
        self.assertEqual(first.status_code, status.HTTP_201_CREATED)

        duplicate = {
            **self.payload,
            "email": "another@example.com",
        }
        second = self.register(duplicate)
        self.assertEqual(second.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("username", second.data)

    def test_people_discovery_follow_and_unfollow(self):
        me = self.register(self.payload)
        other = self.register(
            {
                "email": "maya@example.com",
                "password": "StrongNovaPass2026!",
                "username": "maya",
                "name": "Maya Noor",
            }
        )
        self.assertEqual(me.status_code, status.HTTP_201_CREATED)
        self.assertEqual(other.status_code, status.HTTP_201_CREATED)

        self.authenticate(me.data["access"])

        people = self.client.get(reverse("people"))
        self.assertEqual(people.status_code, status.HTTP_200_OK)
        self.assertEqual(len(people.data["results"]), 1)
        self.assertEqual(people.data["results"][0]["username"], "maya")
        self.assertFalse(people.data["results"][0]["is_following"])

        search = self.client.get(reverse("people"), {"q": "may"})
        self.assertEqual(len(search.data["results"]), 1)

        follow_url = reverse("person-follow", kwargs={"username": "maya"})
        followed = self.client.post(follow_url, {}, format="json")
        self.assertEqual(followed.status_code, status.HTTP_200_OK)
        self.assertTrue(followed.data["is_following"])
        self.assertEqual(followed.data["followers_count"], 1)

        me_after_follow = self.client.get(self.me_url)
        self.assertEqual(me_after_follow.data["following_count"], 1)

        detail = self.client.get(reverse("person-detail", kwargs={"username": "maya"}))
        self.assertTrue(detail.data["is_following"])

        unfollowed = self.client.delete(follow_url)
        self.assertEqual(unfollowed.status_code, status.HTTP_200_OK)
        self.assertFalse(unfollowed.data["is_following"])
        self.assertEqual(unfollowed.data["followers_count"], 0)

    def test_user_cannot_follow_self(self):
        me = self.register(self.payload)
        self.authenticate(me.data["access"])
        response = self.client.post(
            reverse("person-follow", kwargs={"username": "omar"}),
            {},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
