from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ("accounts", "0031_rooms"),
    ]

    operations = [
        migrations.AddField(
            model_name="user",
            name="bio",
            field=models.CharField(blank=True, max_length=160),
        ),
        migrations.AddField(
            model_name="user",
            name="interests",
            field=models.JSONField(blank=True, default=list),
        ),
        migrations.AddField(
            model_name="user",
            name="is_verified",
            field=models.BooleanField(default=False),
        ),
        migrations.AddField(
            model_name="user",
            name="link",
            field=models.URLField(blank=True, max_length=300),
        ),
        migrations.AddField(
            model_name="user",
            name="location",
            field=models.CharField(blank=True, max_length=80),
        ),
        migrations.AddField(
            model_name="user",
            name="profile_theme",
            field=models.CharField(default="violet", max_length=16),
        ),
        migrations.AddField(
            model_name="user",
            name="show_orbit",
            field=models.BooleanField(default=True),
        ),
        migrations.AddField(
            model_name="accountprivacy",
            name="send_read_receipts",
            field=models.BooleanField(default=True),
        ),
        migrations.AddField(
            model_name="accountprivacy",
            name="show_activity_status",
            field=models.BooleanField(default=True),
        ),
        migrations.AddField(
            model_name="accountprivacy",
            name="story_audience",
            field=models.CharField(
                choices=[
                    ("followers", "Followers"),
                    ("close_friends", "Close friends"),
                ],
                default="followers",
                max_length=16,
            ),
        ),
        migrations.AddField(
            model_name="pulse",
            name="category",
            field=models.CharField(
                choices=[
                    ("live", "Live"),
                    ("music", "Music"),
                    ("talks", "Talks"),
                    ("vibes", "Vibes"),
                ],
                default="vibes",
                max_length=8,
            ),
        ),
        migrations.CreateModel(
            name="NotificationPreference",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("likes_comments_shares", models.BooleanField(default=True)),
                ("mentions_tags", models.BooleanField(default=True)),
                ("followers", models.BooleanField(default=True)),
                ("messages", models.BooleanField(default=True)),
                ("live_sessions", models.BooleanField(default=True)),
                ("reels_stories", models.BooleanField(default=True)),
                ("events_spaces", models.BooleanField(default=True)),
                ("product_updates", models.BooleanField(default=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                (
                    "user",
                    models.OneToOneField(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="notification_preferences",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name="PostCommentLike",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                (
                    "comment",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="thread_likes",
                        to="accounts.comment",
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="post_comment_likes",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name="PostCommentReplyLike",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                (
                    "reply",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="thread_likes",
                        to="accounts.postcommentreply",
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="post_comment_reply_likes",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name="RoomReminder",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                (
                    "item",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="reminders",
                        to="accounts.roomitem",
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="room_reminders",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "ordering": ("item__scheduled_for", "item_id"),
            },
        ),
        migrations.AddConstraint(
            model_name="postcommentlike",
            constraint=models.UniqueConstraint(
                fields=("comment", "user"),
                name="unique_post_comment_like",
            ),
        ),
        migrations.AddConstraint(
            model_name="postcommentreplylike",
            constraint=models.UniqueConstraint(
                fields=("reply", "user"),
                name="unique_post_comment_reply_like",
            ),
        ),
        migrations.AddConstraint(
            model_name="roomreminder",
            constraint=models.UniqueConstraint(
                fields=("user", "item"),
                name="unique_room_reminder",
            ),
        ),
    ]
