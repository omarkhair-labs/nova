"""Add unverified user-selected botanical identity and curated search aliases."""
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [("plants", "0012_catalog_product_identity_and_gallery")]

    operations = [
        migrations.AddField(
            model_name="plantunit",
            name="selected_species",
            field=models.ForeignKey(
                blank=True, null=True, on_delete=django.db.models.deletion.PROTECT,
                related_name="customer_selected_plants", to="plants.botanicalspecies",
                help_text="User selection for reference only; does NOT verify the physical plant.",
            ),
        ),
        migrations.CreateModel(
            name="BotanicalSpeciesAlias",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("name", models.CharField(max_length=180)),
                ("language", models.CharField(default="en", max_length=12)),
                ("species", models.ForeignKey(
                    on_delete=django.db.models.deletion.CASCADE,
                    related_name="aliases", to="plants.botanicalspecies",
                )),
            ],
        ),
        migrations.AddConstraint(
            model_name="botanicalspeciesalias",
            constraint=models.UniqueConstraint(
                fields=("species", "name", "language"), name="plant_species_alias_unique",
            ),
        ),
    ]
