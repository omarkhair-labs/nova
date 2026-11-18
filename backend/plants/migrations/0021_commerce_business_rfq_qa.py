"""A single short B2B QA enquiry and staff quote, no binding PO."""
import uuid
from decimal import Decimal
from django.conf import settings
from django.db import models, migrations
from django.core.validators import MinValueValidator
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0020_commerce_qa_order_tracking"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="CommerceBusinessRFQ",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, primary_key=True, editable=False,
                    serialize=False)),
                ("client_id", models.UUIDField()),
                ("fingerprint", models.CharField(max_length=64, editable=False)),
                ("company_name", models.CharField(max_length=180)),
                ("contact_name", models.CharField(max_length=160)),
                ("phone", models.CharField(max_length=32)),
                ("email", models.EmailField(max_length=254, blank=True)),
                ("city", models.CharField(max_length=120)),
                ("sku_snapshot", models.CharField(max_length=64, blank=True)),
                ("quantity", models.PositiveIntegerField(default=1)),
                ("scope", models.TextField(max_length=1500)),
                ("status", models.CharField(max_length=15, default="qa_submitted", choices=[
                    ("qa_submitted", "Request received"),
                    ("qa_quoted", "Quote sent (QA)"),
                    ("qa_accepted", "Quote accepted for follow-up (QA)"),
                    ("qa_declined", "Declined"),
                ])),
                ("quoted_amount", models.DecimalField(max_digits=12, decimal_places=2,
                    null=True, blank=True, validators=[MinValueValidator(Decimal("0.01"))])),
                ("quote_note", models.CharField(max_length=800, blank=True)),
                ("replied_at", models.DateTimeField(null=True, blank=True)),
                ("accepted_at", models.DateTimeField(null=True, blank=True)),
                ("staff_first_seen_at", models.DateTimeField(null=True, blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("requester", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_business_rfqs")),
                ("replied_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    on_delete=django.db.models.deletion.PROTECT,
                    null=True, blank=True, related_name="plantupia_business_rfqs_replied")),
                ("sku", models.ForeignKey(to="plants.commercesku",
                    on_delete=django.db.models.deletion.PROTECT, null=True, blank=True)),
            ],
            options={
                "ordering": ("-created_at",),
                "constraints": [
                    models.UniqueConstraint(fields=("requester", "client_id"),
                        name="plant_b2b_rfq_idempotency"),
                    models.CheckConstraint(condition=models.Q(quantity__gte=1, quantity__lte=10000),
                        name="plant_b2b_rfq_quantity"),
                ],
            },
        ),
    ]
