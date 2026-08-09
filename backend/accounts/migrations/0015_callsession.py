# Generated manually for Nova Calls V11.

import uuid

import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0014_conversation_preference"),
    ]

    operations = [
        migrations.CreateModel(
            name="CallSession",
            fields=[
                (
                    "id",
                    models.UUIDField(
                        default=uuid.uuid4,
                        editable=False,
                        primary_key=True,
                        serialize=False,
                    ),
                ),
                (
                    "kind",
                    models.CharField(
                        choices=[("audio", "Audio"), ("video", "Video")],
                        max_length=8,
                    ),
                ),
                (
                    "status",
                    models.CharField(
                        choices=[
                            ("ringing", "Ringing"),
                            ("active", "Active"),
                            ("declined", "Declined"),
                            ("canceled", "Canceled"),
                            ("ended", "Ended"),
                            ("missed", "Missed"),
                            ("failed", "Failed"),
                        ],
                        default="ringing",
                        max_length=16,
                    ),
                ),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("answered_at", models.DateTimeField(blank=True, null=True)),
                ("ended_at", models.DateTimeField(blank=True, null=True)),
                ("end_reason", models.CharField(blank=True, max_length=32)),
                (
                    "callee",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="incoming_calls",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
                (
                    "caller",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="outgoing_calls",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
                (
                    "conversation",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="calls",
                        to="accounts.conversation",
                    ),
                ),
                (
                    "ended_by",
                    models.ForeignKey(
                        blank=True,
                        null=True,
                        on_delete=django.db.models.deletion.SET_NULL,
                        related_name="ended_calls",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "ordering": ("-created_at",),
                "indexes": [
                    models.Index(
                        fields=["caller", "status", "-created_at"],
                        name="call_caller_status_idx",
                    ),
                    models.Index(
                        fields=["callee", "status", "-created_at"],
                        name="call_callee_status_idx",
                    ),
                    models.Index(
                        fields=["conversation", "-created_at"],
                        name="call_conv_created_idx",
                    ),
                ],
                "constraints": [
                    models.CheckConstraint(
                        condition=models.Q(("caller", models.F("callee")), _negated=True),
                        name="prevent_self_call",
                    ),
                ],
            },
        ),
    ]
