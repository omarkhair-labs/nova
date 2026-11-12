from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0022_commerce_catalog_key_and_gallery"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AddField(
            model_name="commerceproduct",
            name="catalog_review_status",
            field=models.CharField(
                max_length=12, default="draft",
                choices=[
                    ("draft", "Needs product review"),
                    ("approved", "Product data approved for future storefront"),
                ],
                help_text="Staff content approval only. Does not publish to Android or authorize taking orders.",
            ),
        ),
        migrations.AddField(
            model_name="commerceproduct",
            name="catalog_reviewed_by",
            field=models.ForeignKey(
                to=settings.AUTH_USER_MODEL, null=True, blank=True,
                on_delete=django.db.models.deletion.PROTECT,
                related_name="plantupia_catalog_products_reviewed",
            ),
        ),
        migrations.AddField(
            model_name="commerceproduct",
            name="catalog_reviewed_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
    ]
