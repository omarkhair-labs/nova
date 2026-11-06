"""Company workspace navigation must follow real B2B/B2C permission boundaries."""
import os
from unittest.mock import patch

from django.test import TestCase, override_settings
from django.urls import reverse

from .commerce_models import CommerceProduct, CommerceSKU
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class CompanyWorkspaceNavigationTests(DemoFixture, TestCase):
    def setUp(self):
        super().setUp()
        self.staff.is_superuser = True
        self.staff.save(update_fields=["is_superuser"])

    def test_projects_and_commerce_have_distinct_actionable_homes(self):
        projects = self.client.get(reverse("company:projects-home"))
        self.assertEqual(projects.status_code, 200)
        self.assertContains(projects, "B2B")
        self.assertContains(projects, "Installations")
        self.assertContains(projects, "QR handovers")
        commercial = self.client.get(reverse("company:commerce-home"))
        self.assertEqual(commercial.status_code, 200)
        self.assertContains(commercial, "B2C")
        self.assertContains(commercial, "Product drafts / SKUs")
        self.assertContains(commercial, "Warehouse stock")
        self.assertContains(commercial, "B2B · PROJECTS")
        self.assertContains(commercial, "B2C · SHOP")

    def test_commerce_metrics_are_persisted_drafts_not_qa_store_revenue(self):
        product = CommerceProduct.objects.create(
            name="Real internal candidate", kind="plant", created_by=self.staff,
        )
        CommerceSKU.objects.create(product=product, code="WORKSPACE-001", price="165.00")
        commercial = self.client.get(reverse("company:commerce-home"))
        self.assertContains(commercial, "1 drafts")
        self.assertContains(commercial, "1 exact SKU")
        self.assertNotContains(commercial, "Revenue")
        self.assertNotContains(commercial, "Payment collected")

    def test_unprivileged_customer_cannot_open_either_workspace(self):
        self.client.force_login(self.owner)
        self.assertEqual(self.client.get(reverse("company:projects-home")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:commerce-home")).status_code, 403)

    def test_qa_request_inbox_not_advertised_if_flag_disabled(self):
        with patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "0"}):
            response = self.client.get(reverse("company:commerce-home"))
            self.assertEqual(response.status_code, 200)
            self.assertNotContains(response, "awaiting review")
