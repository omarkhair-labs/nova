from django.conf import settings
from django.db import migrations, models
import django.core.validators
import django.db.models.deletion
import uuid
from decimal import Decimal


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0024_commerce_real_sku_cart"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="CommerceOrder",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("client_id", models.UUIDField(help_text="Stable retry identity for this order quote.")),
                ("fingerprint", models.CharField(max_length=64, editable=False)),
                ("cart_fingerprint", models.CharField(max_length=64)),
                ("contact_name", models.CharField(max_length=160)),
                ("contact_phone", models.CharField(max_length=32)),
                ("city", models.CharField(max_length=120)),
                ("delivery_address", models.CharField(max_length=320)),
                ("building_details", models.CharField(max_length=180, blank=True)),
                ("landmark", models.CharField(max_length=180, blank=True)),
                ("delivery_latitude", models.DecimalField(max_digits=10, decimal_places=7, blank=True, null=True)),
                ("delivery_longitude", models.DecimalField(max_digits=10, decimal_places=7, blank=True, null=True)),
                ("delivery_pin_confirmed", models.BooleanField(default=False)),
                ("customer_note", models.CharField(max_length=500, blank=True)),
                ("payment_preference", models.CharField(max_length=20, choices=[
                    ("cod", "Cash on delivery"),
                    ("manual_transfer", "Manual bank or InstaPay transfer"),
                ])),
                ("payment_status", models.CharField(max_length=12, default="unpaid", choices=[
                    ("unpaid", "Not paid"), ("verified", "Payment verified by company"),
                    ("refunded", "Refund verified by company"),
                ])),
                ("subtotal", models.DecimalField(max_digits=12, decimal_places=2)),
                ("shipping_fee", models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True,
                    validators=[django.core.validators.MinValueValidator(Decimal("0.00"))])),
                ("shipping_quote_version", models.PositiveIntegerField(default=0)),
                ("shipping_quote_expires_at", models.DateTimeField(blank=True, null=True)),
                ("status", models.CharField(max_length=22, default="quote_pending", choices=[
                    ("quote_pending", "Awaiting delivery quote"),
                    ("quoted", "Shipping quote ready for customer"),
                    ("confirmed", "Customer accepted exact total"),
                    ("preparing", "Being prepared"),
                    ("dispatched", "With delivery team"),
                    ("delivered", "Delivered with proof"),
                    ("cancelled", "Cancelled"), ("declined", "Cannot fulfill"),
                    ("return_requested", "Return requested"), ("returned", "Returned"),
                ])),
                ("quoted_at", models.DateTimeField(blank=True, null=True)),
                ("confirmed_at", models.DateTimeField(blank=True, null=True)),
                ("cancelled_at", models.DateTimeField(blank=True, null=True)),
                ("last_status_at", models.DateTimeField(blank=True, null=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("customer", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_store_orders", to=settings.AUTH_USER_MODEL)),
                ("quoted_by", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT,
                    null=True, blank=True, related_name="plantupia_store_shipping_quotes",
                    to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ("-created_at", "id")},
        ),
        migrations.CreateModel(
            name="CommerceOrderLine",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("product_name_snapshot", models.CharField(max_length=160)),
                ("sku_code_snapshot", models.CharField(max_length=64)),
                ("option_snapshot", models.CharField(max_length=120, blank=True)),
                ("unit_price_snapshot", models.DecimalField(max_digits=11, decimal_places=2)),
                ("quantity", models.PositiveSmallIntegerField()),
                ("line_subtotal_snapshot", models.DecimalField(max_digits=12, decimal_places=2)),
                ("order", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT,
                    related_name="lines", to="plants.commerceorder")),
                ("sku", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT,
                    to="plants.commercesku")),
            ],
        ),
        migrations.CreateModel(
            name="CommerceStockReservation",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("quantity", models.PositiveIntegerField(validators=[
                    django.core.validators.MinValueValidator(1)])),
                ("released_at", models.DateTimeField(null=True, blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("order", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT,
                    related_name="stock_reservations", to="plants.commerceorder")),
                ("sku", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT,
                    related_name="active_reservations", to="plants.commercesku")),
                ("location", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT,
                    related_name="stock_reservations", to="plants.commercestocklocation")),
            ],
        ),
        migrations.AddConstraint(
            model_name="commerceorder",
            constraint=models.UniqueConstraint(fields=("customer", "client_id"),
                name="plant_store_order_retry_unique"),
        ),
        migrations.AddConstraint(
            model_name="commerceorderline",
            constraint=models.UniqueConstraint(fields=("order", "sku"),
                name="plant_store_order_line_unique"),
        ),
        migrations.AddConstraint(
            model_name="commerceorderline",
            constraint=models.CheckConstraint(condition=models.Q(quantity__gte=1, quantity__lte=10),
                name="plant_store_order_line_qty"),
        ),
        migrations.AddConstraint(
            model_name="commercestockreservation",
            constraint=models.UniqueConstraint(fields=("order", "sku", "location"),
                name="plant_store_reservation_unique"),
        ),
        migrations.AddConstraint(
            model_name="commercestockreservation",
            constraint=models.CheckConstraint(condition=models.Q(quantity__gte=1),
                name="plant_store_reservation_positive"),
        ),
    ]
