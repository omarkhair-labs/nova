"""Optional customer-confirmed delivery pin on isolated QA order requests."""
from django.db import migrations, models

class Migration(migrations.Migration):
    dependencies = [("plants", "0018_commerce_order_requests_qa")]

    operations = [
        migrations.AddField(
            model_name="commerceorderrequest", name="delivery_latitude",
            field=models.DecimalField(max_digits=10, decimal_places=7, null=True, blank=True),
        ),
        migrations.AddField(
            model_name="commerceorderrequest", name="delivery_longitude",
            field=models.DecimalField(max_digits=10, decimal_places=7, null=True, blank=True),
        ),
        migrations.AddField(
            model_name="commerceorderrequest", name="delivery_pin_confirmed",
            field=models.BooleanField(default=False),
        ),
        migrations.AddField(
            model_name="commerceorderrequest", name="building_details",
            field=models.CharField(max_length=180, blank=True),
        ),
        migrations.AddField(
            model_name="commerceorderrequest", name="landmark",
            field=models.CharField(max_length=180, blank=True),
        ),
    ]
