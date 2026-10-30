"""Disposable local fixture only. Never connect this command to a hosted database."""
import json
import os
import secrets
from pathlib import Path
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone
from plants.handovers import issue_pilot_invitation, pilot_enabled
from plants.models import (BotanicalSpecies, CareGuideVersion, InstallationBatch,
                          InstallationManifestRow, PlantUnit, Space)
from plants.qa_demo_care import SAMPLE_CARE_GUIDES



def write_private_qa_credentials(destination, payload):
    """Create the one-time local QA secret file with owner-only permissions.

    Path.open("x") inherits the process umask and can produce a world-readable
    file. An exclusive OS create with mode 0600 avoids that exposure, including
    when the developer's umask is permissive.
    """
    fd = os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as output:
            json.dump(payload, output, indent=2)
    except BaseException:
        destination.unlink(missing_ok=True)
        raise


class Command(BaseCommand):
    help = "Create seven explicitly synthetic plants in an EMPTY disposable SQLite QA database."

    def add_arguments(self, parser):
        parser.add_argument(
            "--with-sample-care-guides", action="store_true",
            help="Seed three RHS-linked SYNTHETIC QA examples; never company-approved advice.",
        )
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
        if options["with_sample_care_guides"]:
            for index, sample in enumerate(SAMPLE_CARE_GUIDES):
                species = BotanicalSpecies.objects.create(
                    key=sample["key"], scientific_name=sample["scientific_name"],
                )
                guide = CareGuideVersion(
                    species=species, version=1, source_url=sample["source_url"],
                    source_reference=sample["source_reference"],
                    instructions=sample["instructions"], context_notes=sample["context_notes"],
                    reviewer=staff, reviewed_at=timezone.now(), published=True,
                )
                guide.full_clean()
                guide.save()
                plant = PlantUnit.objects.get(pk=plant_ids[index])
                plant.species_label = species.scientific_name
                # Synthetic fixture assumption, NOT a real plant identification.
                plant.verified_species = species
                plant.full_clean()
                plant.save(update_fields=("species_label", "verified_species"))
        batch.status = InstallationBatch.Status.READY
        batch.reviewed_by = staff
        batch.reviewed_at = timezone.now()
        batch.full_clean()
        batch.save()
        code, _ = issue_pilot_invitation(batch_id=batch.pk, recipient=customer, staff=staff, mode="customer_owned")
        # Secrets go to one explicitly requested local file, never command logs or permanent admin fields.
        write_private_qa_credentials(destination, {
            "synthetic": True, "accounts": accounts, "invitation": code,
            "space_id": str(space.pk), "plant_ids": plant_ids,
        })
        self.stdout.write(
            "Synthetic fixture created. Private credentials were written to the requested local file."
            + (" Three SYNTHETIC QA care guides were seeded, not company-approved." if options["with_sample_care_guides"] else "")
        )
