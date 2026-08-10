from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0019_privacy_close_friends"),
    ]

    operations = [
        migrations.AddField(
            model_name="conversation",
            name="kind",
            field=models.CharField(
                choices=[("direct", "Direct"), ("group", "Group")],
                default="direct",
                max_length=8,
            ),
        ),
        migrations.AddField(
            model_name="conversation",
            name="title",
            field=models.CharField(blank=True, max_length=80),
        ),
        migrations.AddField(
            model_name="conversation",
            name="created_by",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.SET_NULL,
                related_name="created_conversations",
                to=settings.AUTH_USER_MODEL,
            ),
        ),
        migrations.RemoveConstraint(
            model_name="conversation",
            name="unique_direct_conversation",
        ),
        migrations.RemoveConstraint(
            model_name="conversation",
            name="ordered_direct_conversation_users",
        ),
        migrations.AlterField(
            model_name="conversation",
            name="participant_one",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="conversations_as_one",
                to=settings.AUTH_USER_MODEL,
            ),
        ),
        migrations.AlterField(
            model_name="conversation",
            name="participant_two",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="conversations_as_two",
                to=settings.AUTH_USER_MODEL,
            ),
        ),
        migrations.AddConstraint(
            model_name="conversation",
            constraint=models.UniqueConstraint(
                condition=models.Q(("kind", "direct")),
                fields=("participant_one", "participant_two"),
                name="unique_direct_conversation",
            ),
        ),
        migrations.AddConstraint(
            model_name="conversation",
            constraint=models.CheckConstraint(
                condition=(
                    models.Q(
                        ("kind", "direct"),
                        ("participant_one__isnull", False),
                        ("participant_two__isnull", False),
                    )
                    | models.Q(
                        ("kind", "group"),
                        ("participant_one__isnull", True),
                        ("participant_two__isnull", True),
                    )
                ),
                name="conversation_participants_match_kind",
            ),
        ),
        migrations.AddConstraint(
            model_name="conversation",
            constraint=models.CheckConstraint(
                condition=(
                    ~models.Q(("kind", "direct"))
                    | models.Q(participant_one__lt=models.F("participant_two"))
                ),
                name="ordered_direct_conversation_users",
            ),
        ),
        migrations.AddIndex(
            model_name="conversation",
            index=models.Index(
                fields=["kind", "-updated_at"],
                name="conv_kind_updated_idx",
            ),
        ),
        migrations.AlterField(
            model_name="message",
            name="recipient",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="received_messages",
                to=settings.AUTH_USER_MODEL,
            ),
        ),
        migrations.CreateModel(
            name="GroupMembership",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                (
                    "role",
                    models.CharField(
                        choices=[("owner", "Owner"), ("admin", "Admin"), ("member", "Member")],
                        default="member",
                        max_length=8,
                    ),
                ),
                ("joined_at", models.DateTimeField(auto_now_add=True)),
                (
                    "conversation",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="group_memberships",
                        to="accounts.conversation",
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="group_memberships",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "ordering": ("joined_at", "id"),
            },
        ),
        migrations.AddConstraint(
            model_name="groupmembership",
            constraint=models.UniqueConstraint(
                fields=("conversation", "user"),
                name="unique_group_membership",
            ),
        ),
        migrations.AddIndex(
            model_name="groupmembership",
            index=models.Index(
                fields=["user", "conversation"],
                name="group_member_user_conv_idx",
            ),
        ),
        migrations.AddIndex(
            model_name="groupmembership",
            index=models.Index(
                fields=["conversation", "role"],
                name="group_member_conv_role_idx",
            ),
        ),
        migrations.CreateModel(
            name="GroupReadState",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("updated_at", models.DateTimeField(auto_now=True)),
                (
                    "conversation",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="group_read_states",
                        to="accounts.conversation",
                    ),
                ),
                (
                    "last_read_message",
                    models.ForeignKey(
                        blank=True,
                        null=True,
                        on_delete=django.db.models.deletion.SET_NULL,
                        related_name="group_read_markers",
                        to="accounts.message",
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="group_read_states",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
        ),
        migrations.AddConstraint(
            model_name="groupreadstate",
            constraint=models.UniqueConstraint(
                fields=("conversation", "user"),
                name="unique_group_read_state",
            ),
        ),
        migrations.AddIndex(
            model_name="groupreadstate",
            index=models.Index(
                fields=["user", "conversation"],
                name="group_read_user_conv_idx",
            ),
        ),
    ]
