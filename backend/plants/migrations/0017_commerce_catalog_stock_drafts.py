"""Separate draft commercial catalogue and stock ledger from fictional QA shop and PlantUnit inventory."""
import uuid
from decimal import Decimal

from django.conf import settings
from django.core.validators import MinValueValidator
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0016_catalog_reference_species"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="CommerceProduct",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("name", models.CharField(max_length=160)),
                ("description", models.TextField(max_length=3000, blank=True)),
                ("kind", models.CharField(max_length=12, choices=[
                    ("plant", "Plant"), ("planter", "Pot / Planter"), ("care", "Care supply")
                ])),
                ("cover_photo", models.ImageField(upload_to="plantupia/commerce/drafts/%Y/%m/", blank=True)),
                ("image_description", models.CharField(max_length=180, blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("created_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    on_delete=django.db.models.deletion.PROTECT, related_name="commerce_products_created")),
                ("reference_species", models.ForeignKey(to="plants.botanicalspecies",
                    on_delete=django.db.models.deletion.PROTECT, null=True, blank=True,
                    related_name="commerce_product_references",
                    help_text="A catalogue reference, not verification of an individual customer's plant.")),
            ],
            options={"ordering": ("name", "id")},
        ),
        migrations.CreateModel(
            name="CommerceSKU",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("code", models.CharField(max_length=64, unique=True)),
                ("option_label", models.CharField(max_length=120, blank=True)),
                ("price", models.DecimalField(max_digits=11, decimal_places=2,
                    validators=[MinValueValidator(Decimal("0.01"))])),
                ("currency", models.CharField(max_length=3, default="EGP", choices=[("EGP", "EGP")])),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("product", models.ForeignKey(to="plants.commerceproduct",
                    on_delete=django.db.models.deletion.PROTECT, related_name="variants")),
            ],
            options={"ordering": ("code",)},
        ),
        migrations.CreateModel(
            name="CommerceStockLocation",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("code", models.SlugField(max_length=60, unique=True)),
                ("name", models.CharField(max_length=140)),
                ("active", models.BooleanField(default=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={"ordering": ("code",)},
        ),
        migrations.CreateModel(
            name="CommerceStockMovement",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("quantity_delta", models.IntegerField()),
                ("reason", models.CharField(max_length=12, choices=[
                    ("opening", "Initial counted stock"),
                    ("correction", "Stock count correction"),
                ])),
                ("reference_note", models.CharField(max_length=200)),
                ("client_id", models.UUIDField(help_text="Client-generated retry identity; reuse on a network retry.")),
                ("fingerprint", models.CharField(max_length=64, editable=False)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("sku", models.ForeignKey(to="plants.commercesku",
                    on_delete=django.db.models.deletion.PROTECT, related_name="stock_movements")),
                ("location", models.ForeignKey(to="plants.commercestocklocation",
                    on_delete=django.db.models.deletion.PROTECT, related_name="stock_movements")),
                ("recorded_by", models.ForeignKey(to=settings.AUTH_USER_MODEL,
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="commerce_stock_movements_recorded")),
            ],
            options={
                "ordering": ("-created_at",),
                "constraints": [
                    models.CheckConstraint(condition=~models.Q(quantity_delta=0),
                        name="plant_commerce_stock_nonzero"),
                    models.UniqueConstraint(fields=("recorded_by", "client_id"),
                        name="plant_commerce_stock_idempotency"),
                ],
            },
        ),
    ]
