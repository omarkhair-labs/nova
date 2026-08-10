# Generated for Nova V2 stories.

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0016_trust_safety"),
    ]

    operations = [
        migrations.CreateModel(
            name="Story",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("media", models.FileField(upload_to="stories/%Y/%m/")),
                ("media_type", models.CharField(choices=[("image", "Image"), ("video", "Video")], max_length=8)),
                ("caption", models.CharField(blank=True, max_length=240)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("expires_at", models.DateTimeField()),
                ("author", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="stories", to="accounts.user")),
            ],
            options={"ordering": ("created_at", "id")},
        ),
        migrations.CreateModel(
            name="StoryView",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("viewed_at", models.DateTimeField(auto_now_add=True)),
                ("story", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="views", to="accounts.story")),
                ("viewer", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="story_views", to="accounts.user")),
            ],
            options={"ordering": ("-viewed_at", "-id")},
        ),
        migrations.CreateModel(
            name="StoryReaction",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("emoji", models.CharField(max_length=16)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("story", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="reactions", to="accounts.story")),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="story_reactions", to="accounts.user")),
            ],
            options={"ordering": ("-updated_at", "-id")},
        ),
        migrations.AddIndex(
            model_name="story",
            index=models.Index(fields=["author", "-created_at"], name="story_author_created_idx"),
        ),
        migrations.AddIndex(
            model_name="story",
            index=models.Index(fields=["expires_at", "-created_at"], name="story_expiry_created_idx"),
        ),
        migrations.AddConstraint(
            model_name="storyview",
            constraint=models.UniqueConstraint(fields=("story", "viewer"), name="unique_story_viewer"),
        ),
        migrations.AddIndex(
            model_name="storyview",
            index=models.Index(fields=["story", "-viewed_at"], name="story_view_story_time_idx"),
        ),
        migrations.AddIndex(
            model_name="storyview",
            index=models.Index(fields=["viewer", "-viewed_at"], name="story_view_user_time_idx"),
        ),
        migrations.AddConstraint(
            model_name="storyreaction",
            constraint=models.UniqueConstraint(fields=("story", "user"), name="unique_story_user_reaction"),
        ),
        migrations.AddIndex(
            model_name="storyreaction",
            index=models.Index(fields=["story", "-updated_at"], name="story_react_story_idx"),
        ),
    ]
