"""Catalog identity and approved staff-owned image galleries; existing carts remain valid."""
import django.db.models.deletion
import uuid
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [('plants', '0011_qa_catalog_currency_guard')]
    operations = [
        migrations.AddField(model_name='catalogproduct', name='sku',
            field=models.CharField(blank=True, max_length=40)),
        migrations.AddField(model_name='catalogproduct', name='family_key',
            field=models.SlugField(blank=True, max_length=60)),
        migrations.AddField(model_name='catalogproduct', name='variant_label',
            field=models.CharField(blank=True, max_length=100)),
        migrations.AddField(model_name='catalogproduct', name='image_description',
            field=models.CharField(blank=True, max_length=180)),
        migrations.AddField(model_name='demoorderline', name='sku_snapshot',
            field=models.CharField(blank=True, max_length=40)),
        migrations.AddField(model_name='demoorderline', name='variant_snapshot',
            field=models.CharField(blank=True, max_length=100)),
        migrations.AddConstraint(model_name='catalogproduct',
            constraint=models.UniqueConstraint(fields=('sku',),
                condition=~models.Q(sku=''), name='plant_catalog_unique_nonblank_sku')),
        migrations.CreateModel(name='CatalogProductPhoto', fields=[
            ('id', models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False, serialize=False)),
            ('image', models.ImageField(upload_to='plantupia/catalog/gallery/%Y/%m/')),
            ('alt_text', models.CharField(max_length=180)),
            ('position', models.PositiveSmallIntegerField(default=0)),
            ('uploaded_at', models.DateTimeField(auto_now_add=True)),
            ('product', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,
                related_name='gallery', to='plants.catalogproduct')),
        ], options={'ordering': ('position', 'uploaded_at', 'id')}),
    ]
