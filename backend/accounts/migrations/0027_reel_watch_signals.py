from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0026_stories_v2_text_and_reels"),
    ]

    operations = [
        migrations.CreateModel(
            name="ReelWatch",
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
                ("sessions", models.PositiveIntegerField(default=0)),
                ("total_watch_ms", models.PositiveBigIntegerField(default=0)),
                ("max_completion_permille", models.PositiveSmallIntegerField(default=0)),
                ("completion_count", models.PositiveIntegerField(default=0)),
                ("replay_count", models.PositiveIntegerField(default=0)),
                ("quick_skip_count", models.PositiveIntegerField(default=0)),
                ("last_session_id", models.UUIDField(blank=True, null=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("last_watched_at", models.DateTimeField(auto_now=True)),
                (
                    "reel",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="watch_summaries",
                        to="accounts.reel",
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="reel_watch_summaries",
                        to="accounts.user",
                    ),
                ),
            ],
            options={
                "indexes": [
                    models.Index(
                        fields=["user", "-last_watched_at"],
                        name="reel_watch_user_time_idx",
                    ),
                    models.Index(
                        fields=["reel", "-last_watched_at"],
                        name="reel_watch_reel_time_idx",
                    ),
                ],
                "constraints": [
                    models.UniqueConstraint(
                        fields=("reel", "user"),
                        name="unique_reel_watch_summary",
                    ),
                    models.CheckConstraint(
                        condition=models.Q(("max_completion_permille__lte", 1000)),
                        name="reel_watch_completion_lte_1000",
                    ),
                ],
            },
        ),
    ]
