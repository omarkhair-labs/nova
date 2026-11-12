"""Add status timeline and simple no-charge QA fulfilment tracking."""
import uuid
from decimal import Decimal
from django.conf import settings
from django.core.validators import MinValueValidator
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0019_commerce_qa_delivery_pin"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AlterField(
            model_name="commerceorderrequest", name="status",
            field=models.CharField(max_length=22, default="qa_submitted", choices=[
                ("qa_submitted", "QA request submitted"),
                ("qa_reviewed", "QA reviewed"),
                ("qa_preparing", "QA preparing"),
                ("qa_out_for_delivery", "QA out for delivery"),
                ("qa_delivered", "QA delivered"),
                ("qa_cancelled", "QA cancelled"),
                ("qa_return_requested", "QA return requested"),
                ("qa_returned", "QA returned"),
                ("qa_declined", "QA request declined"),
            ]),
        ),
        migrations.AddField(model_name="commerceorderrequest", name="qa_shipping_fee",
            field=models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True,
                validators=[MinValueValidator(Decimal("0.00"))])),
        migrations.AddField(model_name="commerceorderrequest", name="qa_tracking_reference",
            field=models.CharField(max_length=120, blank=True)),
        migrations.AddField(model_name="commerceorderrequest", name="staff_first_seen_at",
            field=models.DateTimeField(null=True, blank=True)),
        migrations.AddField(model_name="commerceorderrequest", name="customer_cancel_requested_at",
            field=models.DateTimeField(null=True, blank=True)),
        migrations.AddField(model_name="commerceorderrequest", name="customer_return_requested_at",
            field=models.DateTimeField(null=True, blank=True)),
        migrations.AddField(model_name="commerceorderrequest", name="last_status_at",
            field=models.DateTimeField(null=True, blank=True)),
        migrations.CreateModel(
            name="CommerceOrderStatusEvent",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4,
                    serialize=False, editable=False)),
                ("kind", models.CharField(max_length=40)),
                ("label", models.CharField(max_length=120)),
                ("note", models.CharField(max_length=320, blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("order", models.ForeignKey(to="plants.commerceorderrequest",
                    on_delete=django.db.models.deletion.PROTECT, related_name="status_events")),
                ("actor", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_commerce_order_status_events")),
            ],
            options={"ordering": ("created_at", "pk")},
        ),
    ]
