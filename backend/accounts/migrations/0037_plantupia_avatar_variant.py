from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0036_pulse_publish_identity"),
    ]

    operations = [
        migrations.AddField(
            model_name="user",
            name="plantupia_avatar_variant",
            field=models.SmallIntegerField(default=-1),
        ),
    ]
