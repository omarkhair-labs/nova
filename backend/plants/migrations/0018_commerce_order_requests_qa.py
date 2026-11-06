"""Noncommercial customer request snapshots. Zero payment or shipment side effects."""
import uuid

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0017_commerce_catalog_stock_drafts"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="CommerceOrderRequest",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, serialize=False, editable=False)),
                ("client_id", models.UUIDField(help_text="Stable customer retry identifier.")),
                ("fingerprint", models.CharField(max_length=64, editable=False)),
                ("contact_name", models.CharField(max_length=160)),
                ("contact_phone", models.CharField(max_length=32)),
                ("city", models.CharField(max_length=120)),
                ("delivery_address", models.CharField(max_length=320)),
                ("customer_note", models.CharField(max_length=500, blank=True)),
                ("payment_preference", models.CharField(max_length=20, choices=[
                    ("cod", "Cash on delivery — preference only"),
                    ("manual_transfer", "Manual InstaPay/bank transfer — preference only"),
                ])),
                ("provisional_subtotal", models.DecimalField(max_digits=12, decimal_places=2)),
                ("status", models.CharField(max_length=15, default="qa_submitted", choices=[
                    ("qa_submitted", "QA request submitted"),
                    ("qa_reviewed", "QA staff reviewed — no sale accepted"),
                    ("qa_declined", "QA request declined"),
                ])),
                ("review_note", models.CharField(max_length=500, blank=True)),
                ("reviewed_at", models.DateTimeField(null=True, blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("customer", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_commerce_requests")),
                ("reviewed_by", models.ForeignKey(to=settings.AUTH_USER_MODEL, null=True, blank=True,
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_commerce_requests_reviewed")),
            ],
            options={
                "ordering": ("-created_at",),
                "constraints": [
                    models.UniqueConstraint(fields=("customer", "client_id"),
                        name="plant_commerce_request_idempotency"),
                ],
            },
        ),
        migrations.CreateModel(
            name="CommerceOrderRequestLine",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, serialize=False, editable=False)),
                ("product_name_snapshot", models.CharField(max_length=160)),
                ("sku_code_snapshot", models.CharField(max_length=64)),
                ("option_snapshot", models.CharField(max_length=120, blank=True)),
                ("unit_price_snapshot", models.DecimalField(max_digits=11, decimal_places=2)),
                ("quantity", models.PositiveSmallIntegerField()),
                ("line_subtotal_snapshot", models.DecimalField(max_digits=12, decimal_places=2)),
                ("order", models.ForeignKey(to="plants.commerceorderrequest",
                    on_delete=django.db.models.deletion.PROTECT, related_name="lines")),
                ("sku", models.ForeignKey(to="plants.commercesku",
                    on_delete=django.db.models.deletion.PROTECT)),
            ],
            options={
                "constraints": [
                    models.UniqueConstraint(fields=("order", "sku"),
                        name="plant_commerce_request_line_unique"),
                    models.CheckConstraint(condition=models.Q(quantity__gte=1, quantity__lte=10),
                        name="plant_commerce_request_line_qty"),
                ],
            },
        ),
    ]
