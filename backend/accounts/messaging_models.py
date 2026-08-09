from django.db import models

from .models import Conversation, User


class ConversationPreference(models.Model):
    """Per-user preferences for a direct conversation.

    Kept outside models.py so the messaging feature can evolve independently
    while still registering as part of the accounts Django app.
    """

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
