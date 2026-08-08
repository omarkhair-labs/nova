# Generated for Nova push-notification device registration.

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0006_notification"),
    ]

    operations = [
        migrations.CreateModel(
            name="DevicePushToken",
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
                ("token", models.CharField(max_length=512, unique=True)),
                ("platform", models.CharField(default="android", max_length=16)),
                ("active", models.BooleanField(default=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="push_tokens",
                        to="accounts.user",
                    ),
                ),
            ],
            options={
                "ordering": ("-updated_at", "-id"),
                "indexes": [
                    models.Index(
                        fields=["user", "active"],
                        name="push_user_active_idx",
                    ),
                ],
            },
        ),
    ]
