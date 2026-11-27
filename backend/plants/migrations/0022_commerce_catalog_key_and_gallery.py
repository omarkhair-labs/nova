from django.db import migrations, models
import django.db.models.deletion
import uuid


class Migration(migrations.Migration):

    dependencies = [
        ("plants", "0021_commerce_business_rfq_qa"),
    ]

    operations = [
        migrations.AddField(
            model_name="commerceproduct",
            name="catalog_key",
            field=models.SlugField(
                blank=True, null=True, unique=True, max_length=64,
                help_text="Stable identity for reviewed catalogue imports; not a retail SKU.",
            ),
        ),
        migrations.AddField(
            model_name="commerceproduct",
            name="qa_listed",
            field=models.BooleanField(default=False,
                help_text="Explicitly available in the isolated QA app only; never commercial publication."),
        ),
        migrations.CreateModel(
            name="CommerceProductPhoto",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("image", models.ImageField(upload_to="plantupia/commerce/gallery/%Y/%m/")),
                ("alt_text", models.CharField(max_length=180)),
                ("position", models.PositiveSmallIntegerField()),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("product", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,
                    related_name="gallery", to="plants.commerceproduct")),
            ],
            options={
                "ordering": ("position", "id"),
            },
        ),
        migrations.AddConstraint(
            model_name="commerceproductphoto",
            constraint=models.UniqueConstraint(
                fields=("product", "position"),
                name="plant_commerce_gallery_position_unique",
            ),
        ),
    ]
