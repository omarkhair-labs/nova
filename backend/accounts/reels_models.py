from django.db import models

from .models import User


REEL_NOTIFICATION_KINDS = ("reel_like", "reel_comment", "reel_repost", "reel_reply")


class Reel(models.Model):
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="reels",
    )
    video = models.FileField(upload_to="reels/%Y/%m/")
    caption = models.CharField(max_length=500, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        indexes = [
            models.Index(fields=("-created_at", "-id"), name="reel_created_idx"),
            models.Index(fields=("author", "-created_at"), name="reel_author_created_idx"),
        ]

    def delete(self, *args, **kwargs):
        # Reel Activity rows intentionally reuse Nova's existing Notification
        # table without a cross-module FK. Clear their durable targets before
        # removing the Reel so Activity never points at deleted video content.
        from .models import Notification

        Notification.objects.filter(
            kind__in=REEL_NOTIFICATION_KINDS,
            dedupe_key__endswith=f":{self.pk}",
        ).delete()
        video_name = self.video.name
        storage = self.video.storage
        result = super().delete(*args, **kwargs)
        if video_name:
            storage.delete(video_name)
        return result

    def __str__(self):
        return f"Reel {self.pk} by @{self.author.username}"


class ReelLike(models.Model):
    reel = models.ForeignKey(
        Reel,
        on_delete=models.CASCADE,
        related_name="likes",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="reel_likes",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("reel", "user"),
                name="unique_reel_like",
            ),
        ]
        indexes = [
            models.Index(fields=("reel", "-created_at"), name="reel_like_created_idx"),
        ]

    def __str__(self):
        return f"@{self.user.username} likes reel {self.reel_id}"


class ReelComment(models.Model):
    reel = models.ForeignKey(
        Reel,
        on_delete=models.CASCADE,
        related_name="comments",
    )
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="reel_comments",
    )
    body = models.CharField(max_length=300)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("created_at", "id")
        indexes = [
            models.Index(fields=("reel", "created_at"), name="reel_comment_time_idx"),
        ]

    def delete(self, *args, **kwargs):
        from .models import Notification

        Notification.objects.filter(
            kind="reel_comment",
            dedupe_key=f"reel_comment:{self.pk}:{self.reel_id}",
        ).delete()
        return super().delete(*args, **kwargs)

    def __str__(self):
        return f"Comment {self.pk} on reel {self.reel_id} by @{self.author.username}"


class ReelRepost(models.Model):
    reel = models.ForeignKey(
        Reel,
        on_delete=models.CASCADE,
        related_name="reposts",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="reel_reposts",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("reel", "user"),
                name="unique_reel_repost",
            ),
        ]
        indexes = [
            models.Index(fields=("reel", "-created_at"), name="reel_repost_time_idx"),
            models.Index(fields=("user", "-created_at"), name="reel_reposter_time_idx"),
        ]

    def __str__(self):
        return f"@{self.user.username} reposts reel {self.reel_id}"


class ReelWatch(models.Model):
    """Aggregated, viewer-specific playback signals used by Reels ranking."""

    reel = models.ForeignKey(
        Reel,
        on_delete=models.CASCADE,
        related_name="watch_summaries",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="reel_watch_summaries",
    )
    sessions = models.PositiveIntegerField(default=0)
    total_watch_ms = models.PositiveBigIntegerField(default=0)
    max_completion_permille = models.PositiveSmallIntegerField(default=0)
    completion_count = models.PositiveIntegerField(default=0)
    replay_count = models.PositiveIntegerField(default=0)
    quick_skip_count = models.PositiveIntegerField(default=0)
    last_session_id = models.UUIDField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    last_watched_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"
        constraints = [
            models.UniqueConstraint(
                fields=("reel", "user"),
                name="unique_reel_watch_summary",
            ),
            models.CheckConstraint(
                condition=models.Q(max_completion_permille__lte=1000),
                name="reel_watch_completion_lte_1000",
            ),
        ]
        indexes = [
            models.Index(fields=("user", "-last_watched_at"), name="reel_watch_user_time_idx"),
            models.Index(fields=("reel", "-last_watched_at"), name="reel_watch_reel_time_idx"),
        ]

    def __str__(self):
        return f"@{self.user.username} watched reel {self.reel_id} ({self.sessions} sessions)"
