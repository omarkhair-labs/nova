"""Company shell must give one location, without leaking restricted workflows."""
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission
from django.test import TestCase, override_settings
from django.urls import reverse

from .company_roles import (
    create_b2b_commercial_role, create_commerce_operator_role,
    create_finance_reviewer_role,
)
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class CompanyWayfindingTests(DemoFixture, TestCase):
    def test_overview_remains_full_width_and_opens_actual_workspaces(self):
        response = self.client.get(reverse("company:overview"))
        self.assertEqual(response.status_code, 200)
        self.assertNotContains(response, 'class="sidebar workspace-rail"')
        self.assertContains(response, "B2B")
        self.assertContains(response, "B2C")

    def test_b2b_has_workspace_switcher_local_nav_and_breadcrumb(self):
        self.staff.groups.add(create_b2b_commercial_role())
        response = self.client.get(reverse("company:business-inquiries"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'class="sidebar workspace-rail"')
        self.assertContains(response, 'aria-label="Switch workspaces"')
        self.assertContains(response, 'aria-label="B2B tools"')
        self.assertContains(response, 'aria-label="You are here"')
        self.assertContains(response, "B2B · Projects")
        self.assertContains(response, "B2C · Commerce")
        self.assertContains(response, "Care &amp; Support")
        self.assertContains(response, 'aria-current="location"')
        self.assertContains(response, 'aria-current="page"')

    def test_commerce_tools_include_actual_city_rates_and_keep_qa_archived(self):
        self.staff.groups.add(create_commerce_operator_role())
        response = self.client.get(reverse("company:commerce-home"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Delivery prices")
        self.assertContains(response, "Orders &amp; fulfilment")
        self.assertContains(response, "Products &amp; publication")
        self.assertContains(response, "QA &amp; archived tools")
        self.assertContains(response, "B2C · Commerce")

    def test_finance_only_user_does_not_get_project_care_or_stock_actions(self):
        User = get_user_model()
        finance = User.objects.create_user(
            username="staff_finance_nav", email="finance-nav@example.invalid",
            password="qa-only", is_staff=True,
        )
        finance.groups.add(create_finance_reviewer_role())
        self.client.force_login(finance)
        response = self.client.get(reverse("company:commerce-orders"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "B2C · Commerce")
        self.assertContains(response, "Orders &amp; fulfilment")
        self.assertNotContains(response, "B2B · Projects")
        self.assertNotContains(response, "Care &amp; Support")
        self.assertNotContains(response, "Warehouse stock")
        self.assertNotContains(response, "All workspaces")

    def test_shared_website_inbox_is_located_as_shared_not_unrelated_hub(self):
        permission = Permission.objects.get(codename="view_websiteenquiry",
                                            content_type__app_label="plants")
        self.staff.user_permissions.add(permission)
        self.staff.refresh_from_db()
        response = self.client.get(reverse("company:website-inbox"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'aria-label="Shared tools"')
        self.assertContains(response, "Website inbox")
        self.assertContains(response, 'aria-current="location"')
        self.assertContains(response, 'aria-label="You are here"')
