from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import uuid


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0028_commerce_manual_payments"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="CommerceReturnCase",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("reason", models.CharField(max_length=40)),
                ("customer_note", models.CharField(max_length=500)),
                ("requested_at", models.DateTimeField(auto_now_add=True)),
                ("decision", models.CharField(default="requested", max_length=12, choices=[
                    ("requested", "Awaiting return review"), ("approved", "Return approved for intake"),
                    ("declined", "Return declined"), ("received", "Physical return received"),
                ])),
                ("staff_note", models.CharField(max_length=500, blank=True)),
                ("reviewed_at", models.DateTimeField(blank=True, null=True)),
                ("received_evidence_reference", models.CharField(max_length=180, blank=True)),
                ("received_at", models.DateTimeField(blank=True, null=True)),
                ("order", models.OneToOneField(to="plants.commerceorder", related_name="return_case",
                    on_delete=django.db.models.deletion.PROTECT)),
                ("requested_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    related_name="plantupia_returns_requested", on_delete=django.db.models.deletion.PROTECT)),
                ("reviewed_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    null=True, blank=True, related_name="plantupia_returns_reviewed",
                    on_delete=django.db.models.deletion.PROTECT)),
            ],
        ),
        migrations.CreateModel(
            name="CommerceRefundRecord",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("amount", models.DecimalField(max_digits=12, decimal_places=2)),
                ("refund_reference", models.CharField(max_length=120)),
                ("evidence_note", models.CharField(max_length=500)),
                ("verified_at", models.DateTimeField(auto_now_add=True)),
                ("order", models.OneToOneField(to="plants.commerceorder", related_name="verified_refund",
                    on_delete=django.db.models.deletion.PROTECT)),
                ("verified_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    related_name="plantupia_refunds_verified", on_delete=django.db.models.deletion.PROTECT)),
            ],
        ),
    ]
