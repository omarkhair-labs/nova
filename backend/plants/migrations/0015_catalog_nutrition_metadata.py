"""Add optional QA-only Care category metadata without altering current carts or orders."""
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("plants", "0014_care_guide_sections")]

    operations = [
        migrations.AddField(
            model_name="catalogproduct", name="subcategory",
            field=models.CharField(
                blank=True, default="", max_length=16,
                choices=[("", "Other care supplies"), ("tools", "Tools"),
                         ("substrate", "Soil & Substrates"), ("nutrition", "Plant Nutrition")],
            ),
        ),
        migrations.AddField(
            model_name="catalogproduct", name="nutrition_label",
            field=models.CharField(blank=True, max_length=350, help_text="Source-checked manufacturer product formulation/label."),
        ),
        migrations.AddField(
            model_name="catalogproduct", name="nutrition_use_notes",
            field=models.TextField(blank=True, max_length=1200, help_text="Exact usage text from the manufacturer's label; not a plant-specific recommendation."),
        ),
        migrations.AddField(
            model_name="catalogproduct", name="nutrition_cautions",
            field=models.TextField(blank=True, max_length=1200),
        ),
        migrations.AddField(
            model_name="catalogproduct", name="nutrition_source_url",
            field=models.URLField(blank=True, max_length=500),
        ),
        migrations.AddField(
            model_name="catalogproduct", name="nutrition_source_reference",
            field=models.CharField(blank=True, max_length=240),
        ),
    ]
