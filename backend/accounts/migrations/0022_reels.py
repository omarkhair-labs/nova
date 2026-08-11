# Generated for Nova V3 Reels V1.

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0021_group_conversation_profile"),
    ]

    operations = [
        migrations.CreateModel(
            name="Reel",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("video", models.FileField(upload_to="reels/%Y/%m/")),
                ("caption", models.CharField(blank=True, max_length=500)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("author", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="reels", to=settings.AUTH_USER_MODEL)),
            ],
            options={
                "ordering": ("-created_at", "-id"),
                "indexes": [
                    models.Index(fields=["-created_at", "-id"], name="reel_created_idx"),
                    models.Index(fields=["author", "-created_at"], name="reel_author_created_idx"),
                ],
            },
        ),
        migrations.CreateModel(
            name="ReelLike",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("reel", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="likes", to="accounts.reel")),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="reel_likes", to=settings.AUTH_USER_MODEL)),
            ],
            options={
                "ordering": ("-created_at", "-id"),
                "indexes": [models.Index(fields=["reel", "-created_at"], name="reel_like_created_idx")],
                "constraints": [models.UniqueConstraint(fields=("reel", "user"), name="unique_reel_like")],
            },
        ),
        migrations.CreateModel(
            name="ReelComment",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("body", models.CharField(max_length=300)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("author", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="reel_comments", to=settings.AUTH_USER_MODEL)),
                ("reel", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="comments", to="accounts.reel")),
            ],
            options={
                "ordering": ("created_at", "id"),
                "indexes": [models.Index(fields=["reel", "created_at"], name="reel_comment_time_idx")],
            },
        ),
    ]
