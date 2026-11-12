"""Shop product taxonomy is not verification of an actual delivered plant."""
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [("plants", "0015_catalog_nutrition_metadata")]

    operations = [
        migrations.AddField(
            model_name="catalogproduct",
            name="reference_species",
            field=models.ForeignKey(
                to="plants.botanicalspecies",
                on_delete=django.db.models.deletion.PROTECT,
                null=True, blank=True,
                related_name="catalogue_references",
                help_text="Curated SKU reference only. A physical plant is NOT verified by its Shop listing.",
            ),
        ),
    ]
