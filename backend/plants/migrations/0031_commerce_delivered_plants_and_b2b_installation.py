from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0030_commerce_business_inquiries"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AddField(
            model_name="installationbatch",
            name="source_b2b_inquiry",
            field=models.OneToOneField(
                to="plants.commercebusinessinquiry",
                on_delete=django.db.models.deletion.PROTECT,
                related_name="prepared_installation",
                null=True, blank=True,
                help_text="Accepted B2B project scope; draft only until verified actual plant inventory and handover.",
            ),
        ),
        migrations.CreateModel(
            name="CommerceCustomerPlantSpace",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("customer", models.OneToOneField(
                    to=settings.AUTH_USER_MODEL, on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_purchase_space",
                )),
                ("space", models.OneToOneField(
                    to="plants.space", on_delete=django.db.models.deletion.PROTECT,
                    related_name="commerce_purchase_space",
                )),
            ],
        ),
        migrations.CreateModel(
            name="CommerceDeliveredPlant",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ("unit_index", models.PositiveSmallIntegerField()),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("order_line", models.ForeignKey(
                    to="plants.commerceorderline", on_delete=django.db.models.deletion.PROTECT,
                    related_name="delivered_plants",
                )),
                ("plant", models.OneToOneField(
                    to="plants.plantunit", on_delete=django.db.models.deletion.PROTECT,
                    related_name="commerce_delivery_origin",
                )),
            ],
        ),
        migrations.AddConstraint(
            model_name="commercedeliveredplant",
            constraint=models.UniqueConstraint(
                fields=("order_line", "unit_index"), name="plant_store_delivery_unit_unique",
            ),
        ),
    ]
