"""Explicit least-privilege QA role presets; no users or superuser grants."""
from django.contrib.auth.models import Group, Permission

OPERATOR = {
    "access_company_dashboard", "view_installationbatch", "add_installationbatch", "change_installationbatch",
    "view_space", "add_space", "change_space", "view_plantunit", "add_plantunit", "change_plantunit",
    "view_installationmanifestrow", "add_installationmanifestrow", "change_installationmanifestrow",
    "import_inventory", "view_botanicalspecies", "add_botanicalspecies", "view_careguideversion",
    "add_careguideversion", "change_careguideversion", "view_inventoryimportreceipt",
    "view_handoverinvitation", "add_handoverinvitation", "change_handoverinvitation",
}
REVIEWER = {"access_company_dashboard", "view_installationbatch", "view_space", "view_plantunit",
            "view_botanicalspecies", "view_careguideversion", "add_careguideversion",
            "change_careguideversion", "publish_careguideversion"}


def create_qa_roles():
    roles = []
    for name, codenames in (("Plantupia QA operator", OPERATOR), ("Plantupia QA care reviewer", REVIEWER)):
        group, _ = Group.objects.get_or_create(name=name)
        group.permissions.set(Permission.objects.filter(content_type__app_label="plants", codename__in=codenames))
        roles.append(group)
    return roles
