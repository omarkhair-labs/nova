"""G1: staff-only prepared plant inventory. No QR or customer claim endpoint."""
import uuid

import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0004_plantsubmission"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="InstallationBatch",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("reference_code", models.CharField(max_length=80, unique=True)),
                ("status", models.CharField(
                    choices=[
                        ("draft", "Draft"),
                        ("ready", "Inventory reviewed; ready for handover design"),
                    ],
                    default="draft", max_length=12,
                )),
                ("reviewed_at", models.DateTimeField(blank=True, null=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("space", models.OneToOneField(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="installation_batch", to="plants.space",
                )),
                ("prepared_by", models.ForeignKey(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="prepared_plantupia_installations",
                    to=settings.AUTH_USER_MODEL,
                )),
                ("reviewed_by", models.ForeignKey(
                    blank=True, null=True, on_delete=django.db.models.deletion.PROTECT,
                    related_name="reviewed_plantupia_installations",
                    to=settings.AUTH_USER_MODEL,
                )),
            ],
        ),
        migrations.CreateModel(
            name="InstallationManifestRow",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("source_key", models.CharField(max_length=80)),
                ("batch", models.ForeignKey(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="manifest_rows", to="plants.installationbatch",
                )),
                ("plant", models.OneToOneField(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="installation_manifest_row", to="plants.plantunit",
                )),
            ],
        ),
        migrations.AddConstraint(
            model_name="installationmanifestrow",
            constraint=models.UniqueConstraint(
                fields=("batch", "source_key"), name="plant_installation_source_key_unique",
            ),
        ),
    ]
