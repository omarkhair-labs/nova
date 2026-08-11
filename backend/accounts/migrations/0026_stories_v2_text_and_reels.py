from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0025_message_share_reels"),
    ]

    operations = [
        migrations.AddField(
            model_name="story",
            name="background_style",
            field=models.CharField(
                choices=[
                    ("midnight", "Midnight"),
                    ("sunset", "Sunset"),
                    ("ocean", "Ocean"),
                    ("forest", "Forest"),
                ],
                default="midnight",
                max_length=16,
            ),
        ),
        migrations.AddField(
            model_name="story",
            name="shared_reel",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="story_shares",
                to="accounts.reel",
            ),
        ),
        migrations.AlterField(
            model_name="story",
            name="media_type",
            field=models.CharField(
                choices=[
                    ("image", "Image"),
                    ("video", "Video"),
                    ("post", "Shared post"),
                    ("text", "Text"),
                ],
                max_length=8,
            ),
        ),
        migrations.RemoveConstraint(
            model_name="story",
            name="story_has_media_or_post",
        ),
        migrations.AddConstraint(
            model_name="story",
            constraint=models.CheckConstraint(
                condition=(
                    ~models.Q(("media", ""))
                    | models.Q(("shared_post__isnull", False))
                    | models.Q(("shared_reel__isnull", False))
                    | models.Q(("media_type", "text"))
                ),
                name="story_has_content",
            ),
        ),
        migrations.AddConstraint(
            model_name="story",
            constraint=models.CheckConstraint(
                condition=~models.Q(("media_type", "text")) | ~models.Q(("caption", "")),
                name="story_text_has_caption",
            ),
        ),
        migrations.AddConstraint(
            model_name="story",
            constraint=models.CheckConstraint(
                condition=~models.Q(
                    ("shared_post__isnull", False),
                    ("shared_reel__isnull", False),
                ),
                name="story_single_shared_target",
            ),
        ),
    ]
