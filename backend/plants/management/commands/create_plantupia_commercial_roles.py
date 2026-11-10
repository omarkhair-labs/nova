"""Provision distinct commercial staff roles without assigning any account."""
from django.core.management.base import BaseCommand
from plants.company_roles import (
    COMMERCE_FINANCE_REVIEWER, B2B_COMMERCIAL_REVIEWER,
    create_finance_reviewer_role, create_b2b_commercial_role,
)


class Command(BaseCommand):
    help = "Provision real Plantupia Finance and B2B Quote Reviewer groups. Does not assign users."

    def handle(self, *args, **options):
        for create, expected in (
            (create_finance_reviewer_role, COMMERCE_FINANCE_REVIEWER),
            (create_b2b_commercial_role, B2B_COMMERCIAL_REVIEWER),
        ):
            group = create()
            assigned = set(group.permissions.values_list("codename", flat=True))
            missing = expected - assigned
            if missing:
                self.stderr.write(self.style.WARNING(
                    f"{group.name}: migration permissions missing: {', '.join(sorted(missing))}"
                ))
            else:
                self.stdout.write(self.style.SUCCESS(
                    f"Ready: {group.name} ({len(assigned)} permissions). Assign staff explicitly."
                ))
