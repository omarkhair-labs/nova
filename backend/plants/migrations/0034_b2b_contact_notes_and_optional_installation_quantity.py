import uuid
import django.db.models.deletion
from django.conf import settings
from django.core.validators import MinValueValidator
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("plants", "0033_commerce_delivery_city_rate"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AlterField(
            model_name="commercebusinessinquiry",
            name="quantity",
            field=models.PositiveIntegerField(
                null=True, blank=True, validators=[MinValueValidator(1)]),
        ),
        migrations.AddField(
            model_name="commercebusinessinquiry",
            name="followup_state",
            field=models.CharField(max_length=16, default="needs_staff", choices=[
                ("needs_staff", "Staff follow-up needed"),
                ("waiting_outside", "Waiting for external response"),
                ("recorded_done", "Staff recorded follow-up complete"),
            ]),
        ),
        migrations.CreateModel(
            name="CommerceBusinessContactNote",
            fields=[
                ("id", models.UUIDField(primary_key=True, default=uuid.uuid4,
                                         editable=False, serialize=False)),
                ("channel", models.CharField(max_length=16, choices=[
                    ("phone", "Phone"), ("whatsapp", "WhatsApp"),
                    ("meeting", "In-person meeting"),
                    ("email", "Email outside Plantupia"),
                    ("other", "Other offline conversation"),
                ])),
                ("summary", models.TextField(max_length=1200)),
                ("followup_state_after", models.CharField(max_length=16, choices=[
                    ("needs_staff", "Staff follow-up needed"),
                    ("waiting_outside", "Waiting for external response"),
                    ("recorded_done", "Staff recorded follow-up complete"),
                ])),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("inquiry", models.ForeignKey(
                    to="plants.commercebusinessinquiry", on_delete=django.db.models.deletion.PROTECT,
                    related_name="staff_contact_notes",
                )),
                ("recorded_by", models.ForeignKey(
                    to=settings.AUTH_USER_MODEL, on_delete=django.db.models.deletion.PROTECT,
                    related_name="plantupia_b2b_contact_notes",
                )),
            ],
            options={"ordering": ("-created_at", "-id")},
        ),
    ]
