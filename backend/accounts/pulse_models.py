from datetime import timedelta

from django.db import models
from django.db.models import Q
from django.utils import timezone

from .models import User


PULSE_DURATION = timedelta(hours=12)


class Pulse(models.Model):
    class MediaType(models.TextChoices):
        IMAGE = "image", "Image"
        VIDEO = "video", "Video"
        TEXT = "text", "Text"

    class Audience(models.TextChoices):
        FOLLOWERS = "followers", "Followers"
        CLOSE_FRIENDS = "close_friends", "Close friends"

    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="pulses",
    )
    reply_to = models.ForeignKey(
        "self",
        on_delete=models.SET_NULL,
        related_name="moment_replies",
        null=True,
        blank=True,
    )
    chain_root = models.ForeignKey(
        "self",
        on_delete=models.SET_NULL,
        related_name="chain_members",
        null=True,
        blank=True,
    )
    media = models.FileField(upload_to="pulses/%Y/%m/", blank=True)
    media_type = models.CharField(max_length=8, choices=MediaType.choices)
    audience = models.CharField(
        max_length=16,
        choices=Audience.choices,
        default=Audience.FOLLOWERS,
    )
    note = models.CharField(max_length=180, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        indexes = [
            models.Index(fields=("author", "-created_at"), name="pulse_author_created_idx"),
            models.Index(fields=("expires_at", "-created_at"), name="pulse_expiry_created_idx"),
            models.Index(fields=("author", "audience", "-created_at"), name="pulse_author_audience_idx"),
            models.Index(fields=("chain_root", "created_at"), name="pulse_chain_created_idx"),
        ]
        constraints = [
            models.CheckConstraint(
                condition=Q(media_type="text") | ~Q(media=""),
                name="pulse_media_kind_has_file",
            ),
            models.CheckConstraint(
                condition=~Q(media_type="text") | ~Q(note=""),
                name="pulse_text_has_note",
            ),
            models.CheckConstraint(
                condition=~Q(media_type="text") | Q(media=""),
                name="pulse_text_has_no_file",
            ),
        ]

    def save(self, *args, **kwargs):
        if self.expires_at is None:
            self.expires_at = timezone.now() + PULSE_DURATION
        if self.reply_to_id and self.chain_root_id is None:
            self.chain_root_id = self.reply_to.chain_root_id or self.reply_to_id
        super().save(*args, **kwargs)

    @property
    def is_expired(self):
        return self.expires_at <= timezone.now()

    def delete(self, *args, **kwargs):
        media_name = self.media.name
        storage = self.media.storage
        result = super().delete(*args, **kwargs)
        if media_name:
            storage.delete(media_name)
        return result

    def __str__(self):
        return f"Pulse {self.pk} by @{self.author.username}"
