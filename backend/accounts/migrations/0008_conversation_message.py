from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0007_devicepushtoken"),
    ]

    operations = [
        migrations.CreateModel(
            name="Conversation",
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
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                (
                    "participant_one",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="conversations_as_one",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
                (
                    "participant_two",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="conversations_as_two",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "ordering": ("-updated_at", "-id"),
                "indexes": [
                    models.Index(
                        fields=["participant_one", "-updated_at"],
                        name="conv_user1_updated_idx",
                    ),
                    models.Index(
                        fields=["participant_two", "-updated_at"],
                        name="conv_user2_updated_idx",
                    ),
                ],
                "constraints": [
                    models.UniqueConstraint(
                        fields=("participant_one", "participant_two"),
                        name="unique_direct_conversation",
                    ),
                    models.CheckConstraint(
                        condition=models.Q(
                            participant_one__lt=models.F("participant_two")
                        ),
                        name="ordered_direct_conversation_users",
                    ),
                ],
            },
        ),
        migrations.CreateModel(
            name="Message",
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
                ("body", models.CharField(max_length=2000)),
                ("client_id", models.CharField(max_length=64)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("read_at", models.DateTimeField(blank=True, null=True)),
                (
                    "conversation",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="messages",
                        to="accounts.conversation",
                    ),
                ),
                (
                    "recipient",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="received_messages",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
                (
                    "sender",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="sent_messages",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "ordering": ("created_at", "id"),
                "indexes": [
                    models.Index(
                        fields=["conversation", "-id"],
                        name="msg_conv_created_idx",
                    ),
                    models.Index(
                        fields=["recipient", "read_at", "-id"],
                        name="msg_recipient_read_idx",
                    ),
                ],
                "constraints": [
                    models.UniqueConstraint(
                        fields=("sender", "client_id"),
                        name="unique_sender_client_message",
                    )
                ],
            },
        ),
    ]
