from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import uuid


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0027_commerce_fulfillment_receipts"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="CommerceTransferClaim",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("reference", models.CharField(max_length=120)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("order", models.OneToOneField(to="plants.commerceorder", related_name="transfer_claim",
                    on_delete=django.db.models.deletion.PROTECT)),
                ("claimed_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    related_name="plantupia_transfer_claims", on_delete=django.db.models.deletion.PROTECT)),
            ],
        ),
        migrations.CreateModel(
            name="CommercePaymentRecord",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("method", models.CharField(max_length=20, choices=[
                    ("cod", "Cash on delivery"),
                    ("manual_transfer", "Manual bank or InstaPay transfer"),
                ])),
                ("amount", models.DecimalField(max_digits=12, decimal_places=2)),
                ("settlement_reference", models.CharField(max_length=120)),
                ("evidence_note", models.CharField(max_length=500)),
                ("verified_at", models.DateTimeField(auto_now_add=True)),
                ("order", models.OneToOneField(to="plants.commerceorder", related_name="verified_payment",
                    on_delete=django.db.models.deletion.PROTECT)),
                ("verified_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    related_name="plantupia_payments_verified", on_delete=django.db.models.deletion.PROTECT)),
            ],
        ),
    ]
