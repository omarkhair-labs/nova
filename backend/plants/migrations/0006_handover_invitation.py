"""G2 QA-gated recipient-bound handover tokens and explicit claim state."""
import uuid

import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0005_installation_staging"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AlterField(
            model_name="installationbatch",
            name="status",
            field=models.CharField(
                max_length=12, default="draft",
                choices=[
                    ("draft", "Draft"),
                    ("ready", "Inventory reviewed; ready for handover design"),
                    ("claimed", "Claimed by verified recipient"),
                ],
            ),
        ),
        migrations.CreateModel(
            name="HandoverInvitation",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("token_digest", models.CharField(max_length=64, unique=True, editable=False)),
                ("mode", models.CharField(
                    max_length=20,
                    choices=[
                        ("customer_owned", "Customer-owned sale"),
                        ("managed", "Company-maintained site"),
                    ],
                )),
                ("expires_at", models.DateTimeField()),
                ("revoked_at", models.DateTimeField(blank=True, null=True)),
                ("claimed_at", models.DateTimeField(blank=True, null=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("batch", models.ForeignKey(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="invitations", to="plants.installationbatch",
                )),
                ("intended_recipient", models.ForeignKey(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="incoming_plantupia_handovers",
                    to=settings.AUTH_USER_MODEL,
                )),
                ("issued_by", models.ForeignKey(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="issued_plantupia_handovers",
                    to=settings.AUTH_USER_MODEL,
                )),
                ("claimed_by", models.ForeignKey(
                    blank=True, null=True, on_delete=django.db.models.deletion.PROTECT,
                    related_name="claimed_plantupia_handovers",
                    to=settings.AUTH_USER_MODEL,
                )),
            ],
        ),
    ]
