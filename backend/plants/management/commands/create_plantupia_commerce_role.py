"""Create/update the live Plantupia Commerce Operator permission group."""
from django.core.management.base import BaseCommand
from plants.company_roles import COMMERCE_OPERATOR, create_commerce_operator_role


class Command(BaseCommand):
    help = "Provision least-privilege real Plantupia Commerce Operator group; no user assignments."

    def handle(self, *args, **options):
        group = create_commerce_operator_role()
        codes = set(group.permissions.values_list("codename", flat=True))
        missing = COMMERCE_OPERATOR - codes
        if missing:
            self.stderr.write(self.style.WARNING(
                "Missing expected migration permissions: " + ", ".join(sorted(missing))))
            return
        self.stdout.write(self.style.SUCCESS(
            f"Ready: {group.name} ({len(codes)} permissions). Assign staff explicitly."))
