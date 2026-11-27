"""Explicit least-privilege QA role presets; no users or superuser grants."""
from django.contrib.auth.models import Group, Permission

OPERATOR = {
    "access_company_dashboard", "view_installationbatch", "add_installationbatch", "change_installationbatch",
    "view_space", "add_space", "change_space", "view_plantunit", "add_plantunit", "change_plantunit",
    "view_installationmanifestrow", "add_installationmanifestrow", "change_installationmanifestrow",
    "import_inventory", "view_botanicalspecies", "add_botanicalspecies", "view_careguideversion",
    "add_careguideversion", "change_careguideversion", "view_inventoryimportreceipt",
    "view_handoverinvitation", "add_handoverinvitation", "change_handoverinvitation",
    "view_careinspectionplan", "add_careinspectionplan", "change_careinspectionplan", "manage_service_requests",
    "view_catalogproduct", "add_catalogproduct", "change_catalogproduct",
    "view_demoorder", "change_demoorder", "view_demoentitlement", "change_demoentitlement",
    "view_demooffer", "change_demooffer",
}
REVIEWER = {"access_company_dashboard", "view_installationbatch", "view_space", "view_plantunit",
            "view_botanicalspecies", "view_careguideversion", "add_careguideversion",
            "change_careguideversion", "publish_careguideversion", "view_careinspectionplan",
            "add_careinspectionplan", "change_careinspectionplan", "approve_careinspectionplan", "manage_service_requests"}


def create_qa_roles():
    roles = []
    for name, codenames in (("Plantupia QA operator", OPERATOR), ("Plantupia QA care reviewer", REVIEWER)):
        group, _ = Group.objects.get_or_create(name=name)
        group.permissions.set(Permission.objects.filter(content_type__app_label="plants", codename__in=codenames))
        roles.append(group)
    return roles


# An actual operating role. Not added to any user automatically and not the
# historical CEO QA group. Assignment remains an explicit Company decision.
COMMERCE_OPERATOR = {
    "access_company_dashboard", "view_installationbatch",
    "view_commerceproduct", "add_commerceproduct", "change_commerceproduct",
    "view_commercesku", "add_commercesku", "change_commercesku",
    "view_commercestocklocation", "add_commercestocklocation", "change_commercestocklocation",
    "view_commercestockmovement", "add_commercestockmovement",
    "view_commerceorder", "change_commerceorder",
}


def create_commerce_operator_role():
    """Idempotently provision permissions, without auto-enrolling staff."""
    group, _ = Group.objects.get_or_create(name="Plantupia Commerce Operator")
    permissions = Permission.objects.filter(content_type__app_label="plants",
        codename__in=COMMERCE_OPERATOR)
    group.permissions.set(permissions)
    return group

# Commercial roles intentionally separate money verification from warehouse
# dispatch. Group creation never assigns a user.
COMMERCE_FINANCE_REVIEWER = {
    "access_company_dashboard", "view_commerceorder",
    "add_commercepaymentrecord", "add_commercerefundrecord",
}
B2B_COMMERCIAL_REVIEWER = {
    "access_company_dashboard", "view_commercebusinessinquiry",
    "change_commercebusinessinquiry",
}


def create_finance_reviewer_role():
    group, _ = Group.objects.get_or_create(name="Plantupia Finance Reviewer")
    group.permissions.set(Permission.objects.filter(
        content_type__app_label="plants",
        codename__in=COMMERCE_FINANCE_REVIEWER,
    ))
    return group


def create_b2b_commercial_role():
    group, _ = Group.objects.get_or_create(name="Plantupia Business Quotation Reviewer")
    group.permissions.set(Permission.objects.filter(
        content_type__app_label="plants",
        codename__in=B2B_COMMERCIAL_REVIEWER,
    ))
    return group
