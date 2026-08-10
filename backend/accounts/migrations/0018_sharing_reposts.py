# Generated for Nova V2 sharing and reposts.

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0017_stories"),
    ]

    operations = [
        migrations.CreateModel(
            name="Repost",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("post", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="reposts", to="accounts.post")),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="reposts", to="accounts.user")),
            ],
            options={"ordering": ("-created_at", "-id")},
        ),
        migrations.CreateModel(
            name="MessageShare",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("kind", models.CharField(choices=[("post", "Post"), ("profile", "Profile")], max_length=16)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("message", models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name="shared_content", to="accounts.message")),
                ("post", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="message_shares", to="accounts.post")),
                ("profile", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="profile_message_shares", to="accounts.user")),
            ],
        ),
        migrations.AlterField(
            model_name="story",
            name="media",
            field=models.FileField(blank=True, upload_to="stories/%Y/%m/"),
        ),
        migrations.AlterField(
            model_name="story",
            name="media_type",
            field=models.CharField(choices=[("image", "Image"), ("video", "Video"), ("post", "Shared post")], max_length=8),
        ),
        migrations.AddField(
            model_name="story",
            name="shared_post",
            field=models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.CASCADE, related_name="story_shares", to="accounts.post"),
        ),
        migrations.AddConstraint(
            model_name="repost",
            constraint=models.UniqueConstraint(fields=("user", "post"), name="unique_user_post_repost"),
        ),
        migrations.AddIndex(
            model_name="repost",
            index=models.Index(fields=["user", "-created_at"], name="repost_user_created_idx"),
        ),
        migrations.AddIndex(
            model_name="repost",
            index=models.Index(fields=["post", "-created_at"], name="repost_post_created_idx"),
        ),
        migrations.AddConstraint(
            model_name="messageshare",
            constraint=models.CheckConstraint(
                condition=(
                    models.Q(kind="post", profile__isnull=True)
                    | models.Q(kind="profile", post__isnull=True)
                ),
                name="message_share_matches_kind",
            ),
        ),
        migrations.AddIndex(
            model_name="messageshare",
            index=models.Index(fields=["kind", "-created_at"], name="msg_share_kind_created_idx"),
        ),
        migrations.AddConstraint(
            model_name="story",
            constraint=models.CheckConstraint(
                condition=~models.Q(media="") | models.Q(shared_post__isnull=False),
                name="story_has_media_or_post",
            ),
        ),
        migrations.AddConstraint(
            model_name="story",
            constraint=models.CheckConstraint(
                condition=~models.Q(media_type="post") | models.Q(shared_post__isnull=False),
                name="story_post_type_has_target",
            ),
        ),
    ]
