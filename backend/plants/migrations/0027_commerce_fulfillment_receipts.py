from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import uuid


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0026_commerce_real_order_events"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AlterField(
            model_name="commercestockmovement",
            name="reason",
            field=models.CharField(max_length=12, choices=[
                ("opening", "Initial counted stock"),
                ("correction", "Stock count correction"),
                ("dispatch", "Stock released to delivery carrier"),
            ]),
        ),
        migrations.CreateModel(
            name="CommerceFulfillmentReceipt",
            fields=[
                ("id", models.UUIDField(primary_key=True, serialize=False, default=uuid.uuid4, editable=False)),
                ("carrier_name", models.CharField(max_length=120)),
                ("tracking_reference", models.CharField(max_length=120)),
                ("dispatch_note", models.CharField(max_length=300)),
                ("dispatched_at", models.DateTimeField(auto_now_add=True)),
                ("delivery_proof_reference", models.CharField(max_length=180, blank=True)),
                ("delivered_at", models.DateTimeField(null=True, blank=True)),
                ("order", models.OneToOneField(to="plants.commerceorder",
                    related_name="fulfillment_receipt", on_delete=django.db.models.deletion.PROTECT)),
                ("dispatched_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    related_name="plantupia_store_dispatches_recorded", on_delete=django.db.models.deletion.PROTECT)),
                ("delivered_by", models.ForeignKey(to=settings.AUTH_USER_MODEL, null=True, blank=True,
                    related_name="plantupia_store_deliveries_recorded", on_delete=django.db.models.deletion.PROTECT)),
            ],
        ),
    ]
