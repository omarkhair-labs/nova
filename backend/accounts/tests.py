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

    def test_register_login_and_me_flow(self):
        register_response = self.client.post(
            self.register_url,
            self.payload,
            format="json",
        )
        self.assertEqual(register_response.status_code, status.HTTP_201_CREATED)
        self.assertIn("access", register_response.data)
        self.assertIn("refresh", register_response.data)
        self.assertEqual(register_response.data["user"]["username"], "omar")

        self.client.credentials(
            HTTP_AUTHORIZATION=f"Bearer {register_response.data['access']}"
        )
        me_response = self.client.get(self.me_url)
        self.assertEqual(me_response.status_code, status.HTTP_200_OK)
        self.assertEqual(me_response.data["email"], "omar@example.com")

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

    def test_duplicate_username_is_rejected(self):
        first = self.client.post(self.register_url, self.payload, format="json")
        self.assertEqual(first.status_code, status.HTTP_201_CREATED)

        duplicate = {
            **self.payload,
            "email": "another@example.com",
        }
        second = self.client.post(self.register_url, duplicate, format="json")
        self.assertEqual(second.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("username", second.data)
