from django.db import models

from .models import Comment, User
from .reels_models import ReelComment


class PostCommentReply(models.Model):
    comment = models.ForeignKey(
        Comment,
        on_delete=models.CASCADE,
        related_name="thread_replies",
    )
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="post_comment_replies",
    )
    body = models.CharField(max_length=300)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("created_at", "id")
        indexes = [
            models.Index(fields=("comment", "created_at", "id"), name="post_reply_thread_idx"),
        ]

    def __str__(self):
        return f"Reply {self.pk} to comment {self.comment_id} by @{self.author.username}"


class PostCommentLike(models.Model):
    comment = models.ForeignKey(
        Comment,
        on_delete=models.CASCADE,
        related_name="thread_likes",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="post_comment_likes",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        constraints = [
            models.UniqueConstraint(
                fields=("comment", "user"),
                name="unique_post_comment_like",
            ),
        ]


class PostCommentReplyLike(models.Model):
    reply = models.ForeignKey(
        PostCommentReply,
        on_delete=models.CASCADE,
        related_name="thread_likes",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="post_comment_reply_likes",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        constraints = [
            models.UniqueConstraint(
                fields=("reply", "user"),
                name="unique_post_comment_reply_like",
            ),
        ]


class ReelCommentReply(models.Model):
    comment = models.ForeignKey(
        ReelComment,
        on_delete=models.CASCADE,
        related_name="thread_replies",
    )
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="reel_comment_replies",
    )
    body = models.CharField(max_length=300)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("created_at", "id")
        indexes = [
            models.Index(fields=("comment", "created_at", "id"), name="reel_reply_thread_idx"),
        ]

    def __str__(self):
        return f"Reply {self.pk} to Reel comment {self.comment_id} by @{self.author.username}"
