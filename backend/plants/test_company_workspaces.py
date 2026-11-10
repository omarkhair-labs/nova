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
        self.assertNotContains(commercial, "B2B / PROJECTS")
        self.assertContains(commercial, "B2C / SHOP")

    def test_home_has_three_keyboard_native_expanding_worlds(self):
        response = self.client.get(reverse("company:overview"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'name="plantupia-worlds"', count=3)
        self.assertContains(response, 'A place for every')
        self.assertContains(response, 'المشروعات والتركيبات')
        self.assertContains(response, reverse("company:projects-home"))
        self.assertContains(response, reverse("company:commerce-home"))
        self.assertContains(response, reverse("company:care-home"))
        self.assertNotContains(response, 'class="sidebar"')

    def test_navigation_is_scoped_and_qa_records_remain_accessible(self):
        commerce = self.client.get(reverse("company:commerce-home"))
        self.assertEqual(commerce.status_code, 200)
        self.assertContains(commerce, "Products")
        self.assertContains(commerce, "Orders")
        self.assertContains(commerce, "Stock &amp; operations")
        self.assertContains(commerce, "Original QA catalog")
        self.assertContains(commerce, "Original QA orders")
        self.assertNotIn("Installed plant inventory", commerce.content.decode().split("<nav", 1)[1].split("</nav>", 1)[0])
        projects = self.client.get(reverse("company:projects-home"))
        self.assertContains(projects, "QR handovers")
        self.assertNotContains(projects, "Product drafts / SKUs")

    def test_mixed_language_navigation_and_b2c_icons_have_explicit_layout(self):
        response = self.client.get(reverse("company:commerce-home"))
        self.assertEqual(response.status_code, 200)
        # Orders are gated; default QA-off environments expose only products and stock.
        self.assertContains(response, 'class="workspace-nav-link"', count=2)
        self.assertContains(response, '<span class="nav-label-ar" lang="ar" dir="rtl">المنتجات</span>')
        self.assertContains(response, '<span class="nav-label-ar" lang="ar" dir="rtl">المخزون</span>')
        with patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "1"}):
            qa_response = self.client.get(reverse("company:commerce-home"))
            self.assertContains(qa_response, 'class="workspace-nav-link"', count=3)
            self.assertContains(qa_response, '<span class="nav-label-ar" lang="ar" dir="rtl">الطلبات</span>')
        self.assertContains(response, 'viewBox="0 0 24 24"', count=3)
        self.assertNotContains(response, 'class="path-figure" aria-hidden="true">02 <span>≋')

    def test_care_has_a_real_home_and_no_new_data_mutation(self):
        response = self.client.get(reverse("company:care-home"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Botanical library")
        self.assertContains(response, "Inspection plans")
        self.assertContains(response, "Service requests")
        self.assertContains(response, reverse("company:care"))
        self.assertNotContains(response, "Commercial publishing")

    def test_commerce_metrics_are_persisted_drafts_not_qa_store_revenue(self):
        product = CommerceProduct.objects.create(
            name="Real internal candidate", kind="plant", created_by=self.staff,
        )
        CommerceSKU.objects.create(product=product, code="WORKSPACE-001", price="165.00")
        commercial = self.client.get(reverse("company:commerce-home"))
        self.assertContains(commercial, "1</strong> product drafts")
        self.assertContains(commercial, "1 exact SKU")
        self.assertNotContains(commercial, "Revenue")
        self.assertNotContains(commercial, "Payment collected")

    def test_unprivileged_customer_cannot_open_either_workspace(self):
        self.client.force_login(self.owner)
        self.assertEqual(self.client.get(reverse("company:projects-home")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:commerce-home")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:care-home")).status_code, 403)

    def test_qa_request_inbox_not_advertised_if_flag_disabled(self):
        with patch.dict(os.environ, {"PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED": "0"}):
            response = self.client.get(reverse("company:commerce-home"))
            self.assertEqual(response.status_code, 200)
            self.assertNotContains(response, "awaiting review")
