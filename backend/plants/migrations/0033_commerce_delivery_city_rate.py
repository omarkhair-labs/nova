import uuid
import django.db.models.deletion
from decimal import Decimal
from django.conf import settings
from django.core.validators import MinValueValidator
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0032_website_enquiry"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]
    operations = [
        migrations.CreateModel(
            name="CommerceDeliveryCityRate",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
                ("city_key", models.CharField(max_length=120, unique=True)),
                ("city_label", models.CharField(max_length=120)),
                ("fee", models.DecimalField(max_digits=10, decimal_places=2,
                    validators=[MinValueValidator(Decimal("0.00"))])),
                ("active", models.BooleanField(default=True)),
                ("revision", models.PositiveIntegerField(default=1)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_by", models.ForeignKey(
                    to=settings.AUTH_USER_MODEL,
                    on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_delivery_city_rates_updated",
                )),
            ],
            options={"ordering": ("city_label",)},
        ),
    ]
