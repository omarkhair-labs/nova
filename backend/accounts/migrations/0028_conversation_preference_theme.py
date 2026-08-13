from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0027_reel_watch_signals"),
    ]

    operations = [
        migrations.AddField(
            model_name="conversationpreference",
            name="theme_key",
            field=models.CharField(default="nova", max_length=24),
        ),
    ]
