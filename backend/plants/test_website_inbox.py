import json
import uuid
from unittest.mock import patch
from django.core.cache import cache
from django.conf import settings
from django.test import TestCase, override_settings
from django.urls import reverse
from .models import WebsiteEnquiry

TOKEN = "test-website-token-" + "x" * 40

class WebsiteIntakeTests(TestCase):
    def setUp(self):
        cache.clear()
        self.payload = {"submission_id": str(uuid.uuid4()), "topic": "project", "name": "Example visitor",
                        "email": "visitor@example.com", "organization": "", "environment": "industrial",
                        "location": "Cairo", "message": "A project enquiry"}

    def send(self, data=None, token=TOKEN):
        return self.client.post(reverse("plantupia-website-enquiries"), json.dumps(data or self.payload),
                                content_type="application/json", HTTP_AUTHORIZATION="Bearer " + token)

    @patch.dict("os.environ", {"PLANTUPIA_WEBSITE_INBOX_ENABLED": "1", "PLANTUPIA_WEBSITE_INBOX_TOKEN": TOKEN})
    def test_authenticated_ingress_and_retry(self):
        self.assertEqual(self.send(token="wrong").status_code, 401)
        first = self.send()
        self.assertEqual(first.status_code, 201)
        second = self.send()
        self.assertEqual(second.status_code, 200)
        self.assertEqual(first.json(), second.json())
        self.assertEqual(WebsiteEnquiry.objects.count(), 1)
        self.assertEqual(self.send({**self.payload, "message": "Different"}).status_code, 409)
        self.assertEqual(self.client.get(reverse("plantupia-website-enquiries")).status_code, 405)

    @patch.dict("os.environ", {"PLANTUPIA_WEBSITE_INBOX_ENABLED": "1", "PLANTUPIA_WEBSITE_INBOX_TOKEN": TOKEN})
    def test_validation_and_limit(self):
        self.assertEqual(self.send({**self.payload, "email": "invalid"}).status_code, 400)
        self.assertEqual(self.send({**self.payload, "message": " "}).status_code, 400)
        for _ in range(5):
            self.assertEqual(self.send({**self.payload, "submission_id": str(uuid.uuid4())}).status_code, 201)
        self.assertEqual(self.send({**self.payload, "submission_id": str(uuid.uuid4())}).status_code, 429)

    @patch.dict("os.environ", {"PLANTUPIA_WEBSITE_INBOX_ENABLED": "0", "PLANTUPIA_WEBSITE_INBOX_TOKEN": TOKEN})
    def test_disabled(self):
        self.assertEqual(self.send().status_code, 503)
        self.assertFalse(WebsiteEnquiry.objects.exists())

    @override_settings(STORAGES={**settings.STORAGES, "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"}})
    @patch("plants.company_services.enabled", return_value=True)
    def test_company_inbox_authorization(self, _enabled):
        from django.contrib.auth import get_user_model
        from django.contrib.auth.models import Permission
        User = get_user_model()
        staff = User.objects.create_user(username="website-inbox-staff", email="staff@example.test", password="example-password", is_staff=True)
        url = reverse("company:website-inbox")
        self.assertEqual(self.client.get(url).status_code, 302)
        self.client.force_login(staff)
        self.assertEqual(self.client.get(url).status_code, 403)
        staff.user_permissions.add(*Permission.objects.filter(content_type__app_label="plants",
            codename__in=("access_company_dashboard", "view_installationbatch", "view_websiteenquiry")))
        self.assertEqual(self.client.get(url).status_code, 200)

    @override_settings(STORAGES={**settings.STORAGES, "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"}})
    @patch("plants.company_services.enabled", return_value=True)
    def test_inbox_detail_filters_and_read_only_review(self, _enabled):
        from django.contrib.auth import get_user_model
        from django.contrib.auth.models import Permission
        staff = get_user_model().objects.create_user(
            username="inbox-reviewer", email="reviewer@example.test", password="example-password", is_staff=True)
        staff.user_permissions.add(*Permission.objects.filter(content_type__app_label="plants",
            codename__in=("access_company_dashboard", "view_installationbatch", "view_websiteenquiry")))
        record = WebsiteEnquiry.objects.create(
            submission_id=uuid.uuid4(), fingerprint="a" * 64, topic="project",
            name="QA visitor", email="visitor@example.test", organization="QA studio",
            environment="industrial", location="Cairo", message="First line\n<script>test</script>")
        WebsiteEnquiry.objects.create(
            submission_id=uuid.uuid4(), fingerprint="b" * 64, topic="care",
            name="Separate care visitor", email="care@example.test", message="Care question")
        detail_url = reverse("company:website-enquiry", kwargs={"enquiry_id": record.id})
        status_url = reverse("company:website-enquiry-status", kwargs={"enquiry_id": record.id})
        self.client.force_login(staff)
        inbox = self.client.get(reverse("company:website-inbox"), {"topic": "project", "status": "new"})
        self.assertContains(inbox, "inbox-row--new")
        self.assertContains(inbox, "QA visitor")
        self.assertNotContains(inbox, "Separate care visitor")
        self.assertContains(inbox, detail_url)
        detail = self.client.get(detail_url)
        self.assertContains(detail, "enquiry-layout")
        self.assertContains(detail, "visitor@example.test")
        self.assertContains(detail, "First line")
        self.assertContains(detail, "&lt;script&gt;test&lt;/script&gt;")
        self.assertNotContains(detail, "<script>test</script>")
        self.assertNotContains(detail, "Save status")
        self.assertEqual(self.client.post(status_url, {"status": "reviewed"}).status_code, 403)

        staff.user_permissions.add(Permission.objects.get(content_type__app_label="plants",
                                                          codename="change_websiteenquiry"))
        self.assertContains(self.client.get(detail_url), "Save status")
        self.assertRedirects(self.client.post(status_url, {"status": "reviewed"}), detail_url)
        record.refresh_from_db()
        self.assertEqual(record.status, "reviewed")
        self.assertEqual(record.reviewed_by_id, staff.pk)
        self.assertIsNotNone(record.reviewed_at)
        self.assertContains(self.client.get(detail_url), "Last status change")
        self.assertNotContains(self.client.get(reverse("company:website-inbox"),
                                               {"topic": "project", "status": "new"}), "QA visitor")
