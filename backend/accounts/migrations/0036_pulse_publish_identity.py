from django.db import migrations, models
import django.db.models


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0035_story_media_pipeline"),
    ]

    operations = [
        migrations.AddField(
            model_name="pulse",
            name="client_publish_id",
            field=models.UUIDField(blank=True, null=True),
        ),
        migrations.AddConstraint(
            model_name="pulse",
            constraint=models.UniqueConstraint(
                condition=django.db.models.Q(("client_publish_id__isnull", False)),
                fields=("author", "client_publish_id"),
                name="unique_pulse_author_publish_id",
            ),
        ),
    ]
