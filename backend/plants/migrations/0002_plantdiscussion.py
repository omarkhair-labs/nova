# Generated from explicit new PlantDiscussion model for Nova-backed Plantupia Q&A.
import uuid
from django.db import migrations, models
import django.db.models.deletion

class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="PlantDiscussion",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("public_topic", models.CharField(max_length=80)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("resolved_at", models.DateTimeField(blank=True, null=True)),
                ("plant", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="discussions", to="plants.plantunit")),
                ("post", models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name="plantupia_discussion", to="accounts.post")),
                ("resolution_event", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="resolved_discussions", to="plants.plantevent")),
            ],
            options={"ordering": ("-created_at",)},
        ),
    ]
