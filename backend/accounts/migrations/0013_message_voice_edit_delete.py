from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("accounts", "0012_message_rich_content"),
    ]

    operations = [
        migrations.AddField(
            model_name="message",
            name="audio",
            field=models.FileField(blank=True, upload_to="messages/audio/%Y/%m/"),
        ),
        migrations.AddField(
            model_name="message",
            name="audio_duration_ms",
            field=models.PositiveIntegerField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="message",
            name="deleted_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="message",
            name="edited_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
    ]
