from django.conf import settings
from django.db import migrations, models
import django.core.validators
import django.db.models.deletion
import uuid


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0029_commerce_returns_and_refunds"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="CommerceBusinessInquiry",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("client_id", models.UUIDField()),
                ("fingerprint", models.CharField(max_length=64, editable=False)),
                ("kind", models.CharField(max_length=16, choices=[
                    ("wholesale", "Wholesale supply"), ("installation", "Supply and installation project"),
                ])),
                ("company_name", models.CharField(max_length=180)),
                ("contact_name", models.CharField(max_length=160)),
                ("phone", models.CharField(max_length=32)),
                ("email", models.EmailField(blank=True, max_length=254)),
                ("city", models.CharField(max_length=120)),
                ("sku_code_snapshot", models.CharField(max_length=64, blank=True)),
                ("quantity", models.PositiveIntegerField(default=1,
                    validators=[django.core.validators.MinValueValidator(1)])),
                ("scope", models.TextField(max_length=2000)),
                ("status", models.CharField(max_length=12, default="received", choices=[
                    ("received", "Company reviewing request"), ("quoted", "Actual nonbinding quote sent"),
                    ("accepted", "Customer accepts quote for follow-up"), ("declined", "Company unable to quote"),
                ])),
                ("quoted_amount", models.DecimalField(max_digits=12, decimal_places=2, null=True, blank=True)),
                ("quote_version", models.PositiveIntegerField(default=0)),
                ("quoted_at", models.DateTimeField(null=True, blank=True)),
                ("quote_note", models.CharField(max_length=800, blank=True)),
                ("accepted_at", models.DateTimeField(null=True, blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("sku", models.ForeignKey(to="plants.commercesku", null=True, blank=True,
                    on_delete=django.db.models.deletion.PROTECT)),
                ("requester", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    related_name="plantupia_real_business_inquiries", on_delete=django.db.models.deletion.PROTECT)),
                ("quoted_by", models.ForeignKey(to=settings.AUTH_USER_MODEL, null=True, blank=True,
                    related_name="plantupia_real_b2b_quotes", on_delete=django.db.models.deletion.PROTECT)),
            ],
            options={"ordering": ("-created_at", "id")},
        ),
        migrations.AddConstraint(
            model_name="commercebusinessinquiry",
            constraint=models.UniqueConstraint(fields=("requester", "client_id"),
                name="plant_real_b2b_retry_unique"),
        ),
    ]
