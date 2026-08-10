from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0015_callsession"),
    ]

    operations = [
        migrations.CreateModel(
            name="UserBlock",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("blocked", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="blocks_received", to="accounts.user")),
                ("blocker", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="blocks_created", to="accounts.user")),
            ],
            options={"ordering": ("-created_at", "-id")},
        ),
        migrations.CreateModel(
            name="UserReport",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("reason", models.CharField(choices=[("spam", "Spam"), ("harassment", "Harassment"), ("impersonation", "Impersonation"), ("sexual_content", "Sexual content"), ("violence", "Violence"), ("other", "Other")], max_length=32)),
                ("details", models.CharField(blank=True, max_length=500)),
                ("status", models.CharField(choices=[("open", "Open"), ("reviewed", "Reviewed"), ("dismissed", "Dismissed")], default="open", max_length=16)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("reported", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="reports_received", to="accounts.user")),
                ("reporter", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="reports_created", to="accounts.user")),
            ],
            options={"ordering": ("-created_at", "-id")},
        ),
        migrations.AddConstraint(
            model_name="userblock",
            constraint=models.UniqueConstraint(fields=("blocker", "blocked"), name="unique_user_block"),
        ),
        migrations.AddConstraint(
            model_name="userblock",
            constraint=models.CheckConstraint(condition=~models.Q(("blocker", models.F("blocked"))), name="prevent_self_block"),
        ),
        migrations.AddIndex(
            model_name="userblock",
            index=models.Index(fields=["blocker", "blocked"], name="block_pair_idx"),
        ),
        migrations.AddIndex(
            model_name="userblock",
            index=models.Index(fields=["blocked", "blocker"], name="blocked_pair_idx"),
        ),
        migrations.AddConstraint(
            model_name="userreport",
            constraint=models.CheckConstraint(condition=~models.Q(("reporter", models.F("reported"))), name="prevent_self_report"),
        ),
        migrations.AddIndex(
            model_name="userreport",
            index=models.Index(fields=["reported", "status", "-created_at"], name="report_target_status_idx"),
        ),
        migrations.AddIndex(
            model_name="userreport",
            index=models.Index(fields=["reporter", "reported", "status"], name="report_pair_status_idx"),
        ),
    ]
