"""Fresh disposable local SQLite fixture, never a hosted setup command."""
import json
import os
import secrets
from pathlib import Path

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError
from django.db import connection, transaction

from plants import company_services as service
from plants.company_roles import create_qa_roles
from plants.handovers import pilot_enabled
from plants.models import BotanicalSpecies, CareGuideVersion, InstallationBatch, InstallationManifestRow, PlantUnit, Space


class Command(BaseCommand):
    help = "Seed fictional company-dashboard QA only into an empty local development SQLite database."

    def add_arguments(self, parser):
        parser.add_argument("--credentials-file", required=True)

    def handle(self, *args, **options):
        if (settings.ENVIRONMENT != "development" or settings.IS_PRODUCTION or connection.vendor != "sqlite"
                or not service.enabled() or not pilot_enabled()):
            raise CommandError("Requires development, local SQLite, explicit dashboard and handover QA flags.")
        if get_user_model().objects.exists() or Space.objects.exists() or InstallationBatch.objects.exists():
            raise CommandError("Refusing a nonempty database. Use a fresh disposable local fixture.")
        target = Path(options["credentials_file"]).expanduser().resolve()
        repository = settings.BASE_DIR.parent.resolve()
        if target.is_relative_to(repository) or target.exists() or not target.parent.is_dir():
            raise CommandError("Choose a new private credentials file outside the repository, in an existing directory.")
        descriptor = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        try:
            with os.fdopen(descriptor, "w", encoding="utf-8") as output, transaction.atomic():
                operator_role, reviewer_role = create_qa_roles()
                accounts = {}
                users = {}
                for role, staff in (("operator", True), ("reviewer", True), ("recipient", False), ("stranger", False)):
                    password = secrets.token_urlsafe(24)
                    username = f"qa_company_{role}"
                    email = f"{username}@example.test"
                    user = get_user_model().objects.create_user(username=username, email=email, password=password, is_staff=staff)
                    if role == "operator":
                        user.groups.add(operator_role)
                    elif role == "reviewer":
                        user.groups.add(reviewer_role)
                    users[role] = user
                    accounts[role] = {"email": email, "username": username, "password": password}
                for n, name in enumerate(("FICTIONAL QA · Studio courtyard", "FICTIONAL QA · Workshop", "FICTIONAL QA · Office delivery"), 1):
                    space = Space.objects.create(owner=users["operator"], name=name, description="Disposable synthetic dashboard rehearsal. Not a customer installation.")
                    batch = InstallationBatch.objects.create(space=space, prepared_by=users["operator"], reference_code=f"QA-COMPANY-{n:03}")
                    for number in range(1, 3):
                        plant = PlantUnit.objects.create(space=space, label=f"FICTIONAL QA plant {n}.{number}", placement_note=f"QA placement {number}; not a real site", species_label="Unconfirmed QA label")
                        InstallationManifestRow.objects.create(batch=batch, plant=plant, source_key=f"QA-{n}-{number}")
                    if n == 3:
                        token = service.action_token(users["operator"], "ready", batch, service.inventory_state(batch))
                        service.review_installation(users["operator"], batch.pk, token)
                for n in (1, 2):
                    species = BotanicalSpecies.objects.create(key=f"qa-company-fictional-{n}", scientific_name=f"FICTIONAL QA identity {n} — unapproved")
                    CareGuideVersion.objects.create(species=species, version=1, source_url="https://example.test/unapproved-qa-source",
                        source_reference="FICTIONAL QA SOURCE — not botanical advice", instructions="QA DRAFT ONLY. Not approved by Plantupia. Replace with independently reviewed content.",
                        context_notes="No measurements, diagnosis or schedule is implied.", assigned_reviewer=users["reviewer"])
                json.dump({"scope": "DISPOSABLE LOCAL QA ONLY", "accounts": accounts}, output, indent=2)
            self.stdout.write("Created fictional local company QA fixture. Private credentials: " + str(target))
        except Exception:
            # A failed seed must not leave a usable-looking partial credentials file.
            target.unlink(missing_ok=True)
            raise
