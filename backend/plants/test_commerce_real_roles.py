"""Real commerce operations must be grantable without superuser or QA entitlements."""
import uuid
from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings
from django.urls import reverse

from .company_roles import (
    COMMERCE_OPERATOR, create_commerce_operator_role,
    COMMERCE_FINANCE_REVIEWER, B2B_COMMERCIAL_REVIEWER,
    create_finance_reviewer_role, create_b2b_commercial_role,
)
from .test_ceo_demo import DemoFixture, SETTINGS


@override_settings(**SETTINGS)
class RealCommerceRolesTests(DemoFixture, TestCase):
    def test_role_is_explicit_and_does_not_make_all_staff_operators(self):
        group = create_commerce_operator_role()
        self.assertEqual(group.name, "Plantupia Commerce Operator")
        self.assertEqual(set(group.permissions.values_list("codename", flat=True)),
                         COMMERCE_OPERATOR)
        self.assertFalse(group.user_set.exists())
        create_commerce_operator_role()
        self.assertFalse(group.user_set.exists())

        staff = get_user_model().objects.create_user(
            username="merchant_operator", email="merchant-operator@example.test", password="test-only", is_staff=True,
        )
        self.client.force_login(staff)
        self.assertEqual(self.client.get(reverse("company:commerce-orders")).status_code, 403)
        staff.groups.add(group)
        self.assertEqual(self.client.get(reverse("company:commerce-orders")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:commerce-products")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:commerce-stock")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:handovers")).status_code, 403)

    def test_finance_role_cannot_mutate_logistics_or_quote_business_requests(self):
        group = create_finance_reviewer_role()
        self.assertEqual(set(group.permissions.values_list("codename", flat=True)),
                         COMMERCE_FINANCE_REVIEWER)
        self.assertFalse(group.user_set.exists())
        staff = get_user_model().objects.create_user(
            username="merchant_finance", email="merchant-finance@example.test",
            password="test-only", is_staff=True,
        )
        staff.groups.add(group)
        self.client.force_login(staff)
        self.assertEqual(self.client.get(reverse("company:commerce-orders")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:projects-home")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:installations")).status_code, 403)
        oid = uuid.uuid4()
        self.assertEqual(self.client.post(
            reverse("company:commerce-order-fulfillment", args=[oid]),
            {"action": "prepare"}).status_code, 403)
        self.assertEqual(self.client.post(
            reverse("company:commerce-order-return", args=[oid]),
            {"action": "approve", "staff_note": "Test note only"}).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:business-inquiries")).status_code, 403)

    def test_b2b_quote_role_does_not_get_order_or_finance_permissions(self):
        group = create_b2b_commercial_role()
        self.assertEqual(set(group.permissions.values_list("codename", flat=True)),
                         B2B_COMMERCIAL_REVIEWER)
        self.assertFalse(group.user_set.exists())
        staff = get_user_model().objects.create_user(
            username="merchant_b2b", email="merchant-b2b@example.test",
            password="test-only", is_staff=True,
        )
        staff.groups.add(group)
        self.client.force_login(staff)
        self.assertEqual(self.client.get(reverse("company:business-inquiries")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:projects-home")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:installations")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:commerce-orders")).status_code, 403)
        self.assertEqual(self.client.get(reverse("company:commerce-products")).status_code, 403)
