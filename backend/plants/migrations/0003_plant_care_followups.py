from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [("plants", "0002_plantdiscussion")]

    operations = [
        migrations.AddField(
            model_name="plantevent",
            name="follow_up_on",
            field=models.DateField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="plantevent",
            name="follow_up_completed_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="plantevent",
            name="follow_up_for",
            field=models.ForeignKey(
                blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL,
                related_name="documented_outcomes", to="plants.plantevent",
            ),
        ),
    ]
