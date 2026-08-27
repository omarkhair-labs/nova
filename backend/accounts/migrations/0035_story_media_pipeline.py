from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0034_media_pipeline"),
    ]

    operations = [
        migrations.AddField(
            model_name="story",
            name="client_publish_id",
            field=models.UUIDField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="story",
            name="thumbnail",
            field=models.ImageField(blank=True, upload_to="stories/thumbnails/%Y/%m/"),
        ),
        migrations.AddConstraint(
            model_name="story",
            constraint=models.UniqueConstraint(
                condition=models.Q(("client_publish_id__isnull", False)),
                fields=("author", "client_publish_id"),
                name="unique_story_author_publish_id",
            ),
        ),
    ]
