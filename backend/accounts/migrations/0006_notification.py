# Generated for Nova's persistent activity notifications.

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0005_like_comment"),
    ]

    operations = [
        migrations.CreateModel(
            name="Notification",
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
                (
                    "kind",
                    models.CharField(
                        choices=[
                            ("follow", "Follow"),
                            ("like", "Like"),
                            ("comment", "Comment"),
                        ],
                        max_length=16,
                    ),
                ),
                ("dedupe_key", models.CharField(max_length=100, unique=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("read_at", models.DateTimeField(blank=True, null=True)),
                (
                    "actor",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="notification_actions",
                        to="accounts.user",
                    ),
                ),
                (
                    "comment",
                    models.ForeignKey(
                        blank=True,
                        null=True,
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="notifications",
                        to="accounts.comment",
                    ),
                ),
                (
                    "post",
                    models.ForeignKey(
                        blank=True,
                        null=True,
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="notifications",
                        to="accounts.post",
                    ),
                ),
                (
                    "recipient",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="notifications",
                        to="accounts.user",
                    ),
                ),
            ],
            options={
                "ordering": ("-created_at", "-id"),
                "indexes": [
                    models.Index(
                        fields=["recipient", "-created_at"],
                        name="notif_recipient_created_idx",
                    ),
                    models.Index(
                        fields=["recipient", "read_at"],
                        name="notif_recipient_read_idx",
                    ),
                ],
            },
        ),
    ]
