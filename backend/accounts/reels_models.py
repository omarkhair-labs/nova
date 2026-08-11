from django.db import models

from .models import User


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

    def __str__(self):
        return f"Comment {self.pk} on reel {self.reel_id} by @{self.author.username}"
