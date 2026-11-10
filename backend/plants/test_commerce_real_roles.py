"""Real commerce operations must be grantable without superuser or QA entitlements."""
from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings
from django.urls import reverse

from .company_roles import COMMERCE_OPERATOR, create_commerce_operator_role
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
            username="merchant_operator", password="test-only", is_staff=True,
        )
        self.client.force_login(staff)
        self.assertEqual(self.client.get(reverse("company:commerce-orders")).status_code, 403)
        staff.groups.add(group)
        self.assertEqual(self.client.get(reverse("company:commerce-orders")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:commerce-products")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:commerce-stock")).status_code, 200)
        self.assertEqual(self.client.get(reverse("company:handovers")).status_code, 403)
