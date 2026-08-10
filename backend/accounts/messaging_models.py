from django.db import models

from .models import Conversation, Message, User


class ConversationPreference(models.Model):
    """Per-user preferences for any Nova conversation."""

    conversation = models.ForeignKey(
        Conversation,
        on_delete=models.CASCADE,
        related_name="user_preferences",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="conversation_preferences",
    )
    muted = models.BooleanField(default=False)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"
        constraints = [
            models.UniqueConstraint(
                fields=("conversation", "user"),
                name="unique_conversation_user_preference",
            ),
        ]
        indexes = [
            models.Index(
                fields=("user", "muted"),
                name="conv_pref_user_muted_idx",
            ),
        ]

    def __str__(self):
        return f"Conversation {self.conversation_id} preferences for @{self.user.username}"


class GroupMembership(models.Model):
    class Role(models.TextChoices):
        OWNER = "owner", "Owner"
        ADMIN = "admin", "Admin"
        MEMBER = "member", "Member"

    conversation = models.ForeignKey(
        Conversation,
        on_delete=models.CASCADE,
        related_name="group_memberships",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="group_memberships",
    )
    role = models.CharField(max_length=8, choices=Role.choices, default=Role.MEMBER)
    joined_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("joined_at", "id")
        constraints = [
            models.UniqueConstraint(
                fields=("conversation", "user"),
                name="unique_group_membership",
            ),
        ]
        indexes = [
            models.Index(fields=("user", "conversation"), name="group_member_user_conv_idx"),
            models.Index(fields=("conversation", "role"), name="group_member_conv_role_idx"),
        ]

    def __str__(self):
        return f"@{self.user.username} in group {self.conversation_id} ({self.role})"


class GroupReadState(models.Model):
    conversation = models.ForeignKey(
        Conversation,
        on_delete=models.CASCADE,
        related_name="group_read_states",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="group_read_states",
    )
    last_read_message = models.ForeignKey(
        Message,
        on_delete=models.SET_NULL,
        related_name="group_read_markers",
        null=True,
        blank=True,
    )
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"
        constraints = [
            models.UniqueConstraint(
                fields=("conversation", "user"),
                name="unique_group_read_state",
            ),
        ]
        indexes = [
            models.Index(fields=("user", "conversation"), name="group_read_user_conv_idx"),
        ]

    def __str__(self):
        return f"Group {self.conversation_id} read state for @{self.user.username}"
