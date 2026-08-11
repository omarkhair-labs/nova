from django.test import TestCase
from django.urls import reverse


class PublicLegalPagesTests(TestCase):
    def test_privacy_policy_is_public_and_identifies_nova(self):
        response = self.client.get(reverse("privacy-policy"))

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Privacy Policy")
        self.assertContains(response, "Nova does not sell your personal information")
        self.assertContains(response, "account-deletion")
        self.assertContains(response, "omar.khair70@gmail.com")

    def test_account_deletion_page_is_public_and_explains_both_paths(self):
        response = self.client.get(reverse("account-deletion"))

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Delete your Nova account")
        self.assertContains(response, "Settings → Security → Delete account")
        self.assertContains(response, "Nova account deletion request")
        self.assertContains(response, "Shared message history")
        self.assertContains(response, "privacy")
