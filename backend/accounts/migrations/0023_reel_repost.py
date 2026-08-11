from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0022_reels"),
    ]

    operations = [
        migrations.CreateModel(
            name="ReelRepost",
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
                    "reel",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="reposts",
                        to="accounts.reel",
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="reel_reposts",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "ordering": ("-created_at", "-id"),
                "indexes": [
                    models.Index(
                        fields=["reel", "-created_at"],
                        name="reel_repost_time_idx",
                    ),
                    models.Index(
                        fields=["user", "-created_at"],
                        name="reel_reposter_time_idx",
                    ),
                ],
                "constraints": [
                    models.UniqueConstraint(
                        fields=("reel", "user"),
                        name="unique_reel_repost",
                    ),
                ],
            },
        ),
    ]
