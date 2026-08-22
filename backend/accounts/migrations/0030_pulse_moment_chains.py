from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0029_pulse"),
    ]

    operations = [
        migrations.AddField(
            model_name="pulse",
            name="reply_to",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="moment_replies",
                to="accounts.pulse",
            ),
        ),
        migrations.AddField(
            model_name="pulse",
            name="chain_root",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="chain_members",
                to="accounts.pulse",
            ),
        ),
        migrations.AddIndex(
            model_name="pulse",
            index=models.Index(
                fields=["chain_root", "created_at"],
                name="pulse_chain_created_idx",
            ),
        ),
    ]
