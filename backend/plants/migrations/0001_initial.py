# Generated for the initial Plantupia domain. Verify with Django makemigrations --check.
import uuid

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    initial = True

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("accounts", "0036_pulse_publish_identity"),
    ]

    operations = [
        migrations.CreateModel(
            name="Space",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("name", models.CharField(max_length=140)),
                ("description", models.TextField(blank=True)),
                ("location_note", models.CharField(max_length=200, blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("owner", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="owned_plantupia_spaces", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.CreateModel(
            name="PlantUnit",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("unit_type", models.CharField(max_length=12, default="individual", choices=[("individual", "Individual plant"), ("group", "Planted group")])),
                ("label", models.CharField(max_length=120)),
                ("species_label", models.CharField(max_length=180, blank=True)),
                ("placement_note", models.CharField(max_length=180, blank=True)),
                ("is_active", models.BooleanField(default=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("space", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="plants", to="plants.space")),
            ],
        ),
        migrations.CreateModel(
            name="SpaceMember",
            fields=[
                ("id", models.BigAutoField(primary_key=True, serialize=False)),
                ("role", models.CharField(max_length=10, default="viewer", choices=[("viewer", "Viewer"), ("carer", "Carer"), ("manager", "Manager")])),
                ("joined_at", models.DateTimeField(auto_now_add=True)),
                ("space", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="memberships", to="plants.space")),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="plantupia_memberships", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.CreateModel(
            name="PlantNickname",
            fields=[
                ("id", models.BigAutoField(primary_key=True, serialize=False)),
                ("nickname", models.CharField(max_length=80)),
                ("plant", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="nicknames", to="plants.plantunit")),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="plantupia_nicknames", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.CreateModel(
            name="PlantEvent",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("kind", models.CharField(max_length=12, choices=[("observation", "Observation"), ("care", "Care"), ("issue", "Issue"), ("outcome", "Outcome")])),
                ("evidence_type", models.CharField(max_length=10, default="observed", choices=[("observed", "Personally observed"), ("measured", "Measured"), ("reported", "Reported by someone else"), ("inferred", "Inferred")])),
                ("note", models.TextField(blank=True)),
                ("photo", models.ImageField(blank=True, upload_to="plantupia/events/%Y/%m/")),
                ("observed_at", models.DateTimeField()),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("actor", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="plantupia_events", to=settings.AUTH_USER_MODEL)),
                ("plant", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="events", to="plants.plantunit")),
            ],
            options={"ordering": ["-observed_at", "-created_at"]},
        ),
        migrations.AddConstraint(
            model_name="spacemember",
            constraint=models.UniqueConstraint(fields=["space", "user"], name="plant_space_unique_member"),
        ),
        migrations.AddConstraint(
            model_name="plantnickname",
            constraint=models.UniqueConstraint(fields=["plant", "user"], name="plant_unique_personal_nickname"),
        ),
    ]
