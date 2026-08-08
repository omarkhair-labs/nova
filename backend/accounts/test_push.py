from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import DevicePushToken


class PushDeviceRegistrationTests(APITestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(
            email="push@example.com",
            username="pushuser",
            password="StrongNovaPass2026!",
            name="Push User",
        )
        self.client.force_authenticate(self.user)
        self.url = reverse("push-devices")
        self.token = "fcm-test-token-abcdefghijklmnopqrstuvwxyz-0123456789"

    def test_register_refresh_and_remove_device_token(self):
        created = self.client.post(
            self.url,
            {"token": self.token, "platform": "android"},
            format="json",
        )
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        self.assertTrue(created.data["registered"])

        device = DevicePushToken.objects.get(token=self.token)
        self.assertEqual(device.user, self.user)
        self.assertTrue(device.active)
        self.assertEqual(device.platform, "android")

        repeated = self.client.post(
            self.url,
            {"token": self.token, "platform": "android"},
            format="json",
        )
        self.assertEqual(repeated.status_code, status.HTTP_200_OK)
        self.assertEqual(DevicePushToken.objects.filter(token=self.token).count(), 1)

        removed = self.client.delete(
            self.url,
            {"token": self.token},
            format="json",
        )
        self.assertEqual(removed.status_code, status.HTTP_200_OK)
        self.assertTrue(removed.data["removed"])
        self.assertFalse(DevicePushToken.objects.filter(token=self.token).exists())

    def test_token_moves_to_current_account_on_new_login(self):
        DevicePushToken.objects.create(
            user=self.user,
            token=self.token,
            platform="android",
        )
        other = get_user_model().objects.create_user(
            email="other@example.com",
            username="otherpush",
            password="StrongNovaPass2026!",
        )
        self.client.force_authenticate(other)

        response = self.client.post(
            self.url,
            {"token": self.token, "platform": "android"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(DevicePushToken.objects.get(token=self.token).user, other)

    def test_invalid_device_token_is_rejected(self):
        response = self.client.post(
            self.url,
            {"token": "too-short", "platform": "android"},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        unsupported = self.client.post(
            self.url,
            {"token": self.token, "platform": "ios"},
            format="json",
        )
        self.assertEqual(unsupported.status_code, status.HTTP_400_BAD_REQUEST)
