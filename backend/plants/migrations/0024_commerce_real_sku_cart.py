from django.conf import settings
from django.db import migrations, models
import django.core.validators
import django.db.models.deletion
import uuid


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0023_commerce_catalog_review_status"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="CommerceCartLine",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("quantity", models.PositiveSmallIntegerField(validators=[django.core.validators.MinValueValidator(1)])),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("customer", models.ForeignKey(
                    to=settings.AUTH_USER_MODEL, on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_commerce_cart",
                )),
                ("sku", models.ForeignKey(
                    to="plants.commercesku", on_delete=django.db.models.deletion.PROTECT,
                    related_name="cart_lines",
                )),
            ],
            options={"ordering": ("sku__code", "id")},
        ),
        migrations.AddConstraint(
            model_name="commercecartline",
            constraint=models.UniqueConstraint(fields=("customer", "sku"),
                name="plant_commerce_cart_customer_sku_unique"),
        ),
        migrations.AddConstraint(
            model_name="commercecartline",
            constraint=models.CheckConstraint(condition=models.Q(quantity__gte=1, quantity__lte=10),
                name="plant_commerce_cart_qty_1_10"),
        ),
    ]
