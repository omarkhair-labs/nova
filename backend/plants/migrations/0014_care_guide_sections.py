"""Reviewed topic sections extend published narratives without rewriting older guides."""
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [("plants", "0013_user_selected_species_and_aliases")]

    operations = [
        migrations.AlterField(
            model_name="careguideversion",
            name="instructions",
            field=models.TextField(
                blank=True,
                help_text="Legacy narrative. New versions can be authored in reviewed topic sections.",
            ),
        ),
        migrations.CreateModel(
            name="CareGuideSection",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("topic", models.CharField(max_length=16, choices=[
                    ("water", "Water"), ("light", "Light"), ("soil", "Soil"),
                    ("humidity", "Humidity"), ("fertilizing", "Fertilizing"),
                    ("pruning", "Pruning"), ("repotting", "Repotting"),
                    ("inspection", "Inspection"), ("warnings", "Warnings"),
                ])),
                ("title", models.CharField(max_length=120)),
                ("instructions", models.TextField(max_length=8000)),
                ("context_notes", models.TextField(blank=True, max_length=2000)),
                ("source_url", models.URLField(blank=True, max_length=500,
                    help_text="Optional additional HTTPS source; guide source remains the default.")),
                ("source_reference", models.CharField(blank=True, max_length=240)),
                ("position", models.PositiveSmallIntegerField(default=0)),
                ("guide", models.ForeignKey(
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="sections", to="plants.careguideversion",
                )),
            ],
            options={"ordering": ("position", "topic", "pk")},
        ),
        migrations.AddConstraint(
            model_name="careguidesection",
            constraint=models.UniqueConstraint(
                fields=("guide", "topic"),
                name="plant_care_section_unique_topic_per_version",
            ),
        ),
    ]
