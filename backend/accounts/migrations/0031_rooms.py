from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ("accounts", "0030_pulse_moment_chains"),
    ]

    operations = [
        migrations.CreateModel(
            name="RoomProfile",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("description", models.CharField(blank=True, max_length=240)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                (
                    "conversation",
                    models.OneToOneField(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="room_profile",
                        to="accounts.conversation",
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name="RoomItem",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                (
                    "kind",
                    models.CharField(
                        choices=[
                            ("note", "Note"),
                            ("photo", "Photo"),
                            ("video", "Video"),
                            ("music", "Music"),
                            ("plan", "Plan"),
                            ("saved", "Saved"),
                        ],
                        max_length=12,
                    ),
                ),
                ("title", models.CharField(blank=True, max_length=120)),
                ("body", models.CharField(blank=True, max_length=500)),
                ("url", models.URLField(blank=True, max_length=700)),
                ("media", models.FileField(blank=True, upload_to="rooms/items/%Y/%m/")),
                ("scheduled_for", models.DateTimeField(blank=True, null=True)),
                ("pinned", models.BooleanField(default=False)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                (
                    "conversation",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="room_items",
                        to="accounts.conversation",
                    ),
                ),
                (
                    "created_by",
                    models.ForeignKey(
                        blank=True,
                        null=True,
                        on_delete=django.db.models.deletion.SET_NULL,
                        related_name="room_items",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "ordering": ("-pinned", "-created_at", "-id"),
            },
        ),
        migrations.AddIndex(
            model_name="roomitem",
            index=models.Index(
                fields=["conversation", "-created_at"],
                name="room_item_conv_created_idx",
            ),
        ),
        migrations.AddIndex(
            model_name="roomitem",
            index=models.Index(
                fields=["conversation", "kind", "-created_at"],
                name="room_item_conv_kind_idx",
            ),
        ),
    ]
