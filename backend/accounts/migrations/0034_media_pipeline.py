from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0033_roomprofile_is_public_roomprofile_topics_and_more"),
    ]

    operations = [
        migrations.AlterField(
            model_name="post",
            name="image",
            field=models.ImageField(blank=True, upload_to="posts/%Y/%m/"),
        ),
        migrations.AddField(
            model_name="post",
            name="video",
            field=models.FileField(blank=True, upload_to="posts/video/%Y/%m/"),
        ),
        migrations.AddField(
            model_name="post",
            name="thumbnail",
            field=models.ImageField(blank=True, upload_to="posts/thumbnails/%Y/%m/"),
        ),
        migrations.AddField(
            model_name="post",
            name="media_type",
            field=models.CharField(
                choices=[("image", "Image"), ("video", "Video")],
                default="image",
                max_length=8,
            ),
        ),
        migrations.AddField(
            model_name="post",
            name="client_publish_id",
            field=models.UUIDField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="pulse",
            name="thumbnail",
            field=models.ImageField(blank=True, upload_to="pulses/thumbnails/%Y/%m/"),
        ),
        migrations.AddField(
            model_name="reel",
            name="thumbnail",
            field=models.ImageField(blank=True, upload_to="reels/thumbnails/%Y/%m/"),
        ),
        migrations.AddField(
            model_name="reel",
            name="client_publish_id",
            field=models.UUIDField(blank=True, null=True),
        ),
        migrations.AddConstraint(
            model_name="post",
            constraint=models.UniqueConstraint(
                condition=models.Q(("client_publish_id__isnull", False)),
                fields=("author", "client_publish_id"),
                name="unique_post_author_publish_id",
            ),
        ),
        migrations.AddConstraint(
            model_name="reel",
            constraint=models.UniqueConstraint(
                condition=models.Q(("client_publish_id__isnull", False)),
                fields=("author", "client_publish_id"),
                name="unique_reel_author_publish_id",
            ),
        ),
    ]
