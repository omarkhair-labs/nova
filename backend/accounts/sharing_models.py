from django.db import models
from django.db.models import Q

from .models import Message, Post, User


class Repost(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="reposts",
    )
    post = models.ForeignKey(
        Post,
        on_delete=models.CASCADE,
        related_name="reposts",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("user", "post"),
                name="unique_user_post_repost",
            ),
        ]
        indexes = [
            models.Index(fields=("user", "-created_at"), name="repost_user_created_idx"),
            models.Index(fields=("post", "-created_at"), name="repost_post_created_idx"),
        ]

    def __str__(self):
        return f"@{self.user.username} reposted post {self.post_id}"


class MessageShare(models.Model):
    class Kind(models.TextChoices):
        POST = "post", "Post"
        PROFILE = "profile", "Profile"

    message = models.OneToOneField(
        Message,
        on_delete=models.CASCADE,
        related_name="shared_content",
    )
    kind = models.CharField(max_length=16, choices=Kind.choices)
    post = models.ForeignKey(
        Post,
        on_delete=models.SET_NULL,
        related_name="message_shares",
        null=True,
        blank=True,
    )
    profile = models.ForeignKey(
        User,
        on_delete=models.SET_NULL,
        related_name="profile_message_shares",
        null=True,
        blank=True,
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        constraints = [
            # The referenced object may later be deleted and SET_NULL must be
            # able to preserve the message history. Application creation paths
            # require a live target; the DB only prevents cross-kind targets.
            models.CheckConstraint(
                condition=(
                    Q(kind="post", profile__isnull=True)
                    | Q(kind="profile", post__isnull=True)
                ),
                name="message_share_matches_kind",
            ),
        ]
        indexes = [
            models.Index(fields=("kind", "-created_at"), name="msg_share_kind_created_idx"),
        ]

    def __str__(self):
        return f"{self.kind} share on message {self.message_id}"
