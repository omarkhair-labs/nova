"""Disposable local fixture only. Never connect this command to a hosted database."""
import json
import secrets
from pathlib import Path
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone
from plants.handovers import issue_pilot_invitation, pilot_enabled
from plants.models import InstallationBatch, InstallationManifestRow, PlantUnit, Space


class Command(BaseCommand):
    help = "Create seven explicitly synthetic plants in an EMPTY disposable SQLite QA database."

    def add_arguments(self, parser):
        parser.add_argument("--credentials-file", required=True,
                            help="Private local output outside the repository; contains test credentials and a short-lived secret.")

    @transaction.atomic
    def handle(self, *args, **options):
        if settings.IS_PRODUCTION or not pilot_enabled() or settings.DATABASES["default"]["ENGINE"] != "django.db.backends.sqlite3":
            raise CommandError("Only explicitly enabled local SQLite QA is supported.")
        if Space.objects.exists() or get_user_model().objects.exists():
            raise CommandError("Use an empty disposable database; existing records will not be changed.")
        destination = Path(options["credentials_file"]).resolve()
        if destination.exists() or not destination.parent.is_dir():
            raise CommandError("Choose a new file in an existing private local directory.")
        users = get_user_model()
        accounts = {}
        for name in ("staff", "customer", "stranger", "independent"):
            password = secrets.token_urlsafe(18)
            user = users.objects.create_user(username=f"qa_{name}", email=f"{name}@plantupia.example.test",
                                             password=password, name=f"Synthetic QA {name}",
                                             is_staff=name == "staff", is_superuser=name == "staff")
            accounts[name] = {"id": user.pk, "email": user.email, "password": password}
        staff = users.objects.get(pk=accounts["staff"]["id"])
        customer = users.objects.get(pk=accounts["customer"]["id"])
        space = Space.objects.create(owner=staff, name="Synthetic QA seven-plant space")
        batch = InstallationBatch.objects.create(reference_code="QA-LOCAL-SEVEN", space=space, prepared_by=staff)
        plant_ids = []
        for number in range(1, 8):
            plant = PlantUnit.objects.create(space=space, label=f"Synthetic plant {number:02}",
                                              placement_note=f"Synthetic position {number:02}")
            plant_ids.append(str(plant.pk))
            InstallationManifestRow.objects.create(batch=batch, plant=plant, source_key=f"QA-{number:02}")
        batch.status = InstallationBatch.Status.READY
        batch.reviewed_by = staff
        batch.reviewed_at = timezone.now()
        batch.full_clean()
        batch.save()
        code, _ = issue_pilot_invitation(batch_id=batch.pk, recipient=customer, staff=staff, mode="customer_owned")
        # Secrets go to one explicitly requested local file, never command logs or permanent admin fields.
        with destination.open("x", encoding="utf-8") as output:
            json.dump({"synthetic": True, "accounts": accounts, "invitation": code,
                       "space_id": str(space.pk), "plant_ids": plant_ids}, output, indent=2)
        self.stdout.write("Synthetic fixture created. Private credentials were written to the requested local file.")
