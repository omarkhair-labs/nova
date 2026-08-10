from datetime import timedelta

from django.db import models
from django.db.models import Q
from django.utils import timezone

from .models import Post, User


class Story(models.Model):
    class MediaType(models.TextChoices):
        IMAGE = "image", "Image"
        VIDEO = "video", "Video"
        POST = "post", "Shared post"

    class Audience(models.TextChoices):
        FOLLOWERS = "followers", "Followers"
        CLOSE_FRIENDS = "close_friends", "Close friends"

    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="stories",
    )
    media = models.FileField(upload_to="stories/%Y/%m/", blank=True)
    media_type = models.CharField(max_length=8, choices=MediaType.choices)
    shared_post = models.ForeignKey(
        Post,
        on_delete=models.CASCADE,
        related_name="story_shares",
        null=True,
        blank=True,
    )
    audience = models.CharField(
        max_length=16,
        choices=Audience.choices,
        default=Audience.FOLLOWERS,
    )
    caption = models.CharField(max_length=240, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()

    class Meta:
        app_label = "accounts"
        ordering = ("created_at", "id")
        indexes = [
            models.Index(fields=("author", "-created_at"), name="story_author_created_idx"),
            models.Index(fields=("expires_at", "-created_at"), name="story_expiry_created_idx"),
            models.Index(fields=("author", "audience", "-created_at"), name="story_author_audience_idx"),
        ]
        constraints = [
            models.CheckConstraint(
                condition=~Q(media="") | Q(shared_post__isnull=False),
                name="story_has_media_or_post",
            ),
            models.CheckConstraint(
                condition=~Q(media_type="post") | Q(shared_post__isnull=False),
                name="story_post_type_has_target",
            ),
        ]

    def save(self, *args, **kwargs):
        # A shared post is still linked through shared_post, while the Android
        # viewer can treat its visual media as a normal image Story.
        if self.shared_post_id and self.media_type == self.MediaType.POST:
            self.media_type = self.MediaType.IMAGE
        if self.expires_at is None:
            self.expires_at = timezone.now() + timedelta(hours=24)
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
        return f"Story {self.pk} by @{self.author.username}"


class StoryView(models.Model):
    story = models.ForeignKey(
        Story,
        on_delete=models.CASCADE,
        related_name="views",
    )
    viewer = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="story_views",
    )
    viewed_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-viewed_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("story", "viewer"),
                name="unique_story_viewer",
            ),
        ]
        indexes = [
            models.Index(fields=("story", "-viewed_at"), name="story_view_story_time_idx"),
            models.Index(fields=("viewer", "-viewed_at"), name="story_view_user_time_idx"),
        ]

    def __str__(self):
        return f"@{self.viewer.username} viewed story {self.story_id}"


class StoryReaction(models.Model):
    story = models.ForeignKey(
        Story,
        on_delete=models.CASCADE,
        related_name="reactions",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="story_reactions",
    )
    emoji = models.CharField(max_length=16)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-updated_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("story", "user"),
                name="unique_story_user_reaction",
            ),
        ]
        indexes = [
            models.Index(fields=("story", "-updated_at"), name="story_react_story_idx"),
        ]

    def __str__(self):
        return f"{self.emoji} by @{self.user.username} on story {self.story_id}"
