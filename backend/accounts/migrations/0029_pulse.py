from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0028_conversation_preference_theme"),
    ]

    operations = [
        migrations.CreateModel(
            name="Pulse",
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
                ("media", models.FileField(blank=True, upload_to="pulses/%Y/%m/")),
                (
                    "media_type",
                    models.CharField(
                        choices=[
                            ("image", "Image"),
                            ("video", "Video"),
                            ("text", "Text"),
                        ],
                        max_length=8,
                    ),
                ),
                (
                    "audience",
                    models.CharField(
                        choices=[
                            ("followers", "Followers"),
                            ("close_friends", "Close friends"),
                        ],
                        default="followers",
                        max_length=16,
                    ),
                ),
                ("note", models.CharField(blank=True, max_length=180)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("expires_at", models.DateTimeField()),
                (
                    "author",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="pulses",
                        to="accounts.user",
                    ),
                ),
            ],
            options={"ordering": ("-created_at", "-id")},
        ),
        migrations.AddIndex(
            model_name="pulse",
            index=models.Index(
                fields=["author", "-created_at"],
                name="pulse_author_created_idx",
            ),
        ),
        migrations.AddIndex(
            model_name="pulse",
            index=models.Index(
                fields=["expires_at", "-created_at"],
                name="pulse_expiry_created_idx",
            ),
        ),
        migrations.AddIndex(
            model_name="pulse",
            index=models.Index(
                fields=["author", "audience", "-created_at"],
                name="pulse_author_audience_idx",
            ),
        ),
        migrations.AddConstraint(
            model_name="pulse",
            constraint=models.CheckConstraint(
                condition=models.Q(("media_type", "text"), ("media", ""), _connector="OR", _negated=True),
                name="pulse_media_kind_has_file",
            ),
        ),
        migrations.AddConstraint(
            model_name="pulse",
            constraint=models.CheckConstraint(
                condition=models.Q(("media_type", "text"), ("note", ""), _negated=True),
                name="pulse_text_has_note",
            ),
        ),
        migrations.AddConstraint(
            model_name="pulse",
            constraint=models.CheckConstraint(
                condition=models.Q(models.Q(("media_type", "text"), _negated=True), ("media", ""), _connector="OR"),
                name="pulse_text_has_no_file",
            ),
        ),
    ]
