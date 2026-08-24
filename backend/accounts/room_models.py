from django.db import models, transaction
from django.db.models.signals import post_delete, pre_save
from django.dispatch import receiver

from .models import Conversation, User


class RoomProfile(models.Model):
    """Room-specific metadata layered on top of an existing group conversation."""

    conversation = models.OneToOneField(
        Conversation,
        on_delete=models.CASCADE,
        related_name="room_profile",
    )
    description = models.CharField(max_length=240, blank=True)
    is_public = models.BooleanField(default=False)
    topics = models.JSONField(default=list, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"

    def __str__(self):
        return f"Room profile for conversation {self.conversation_id}"


class RoomFollow(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="followed_rooms",
    )
    room = models.ForeignKey(
        RoomProfile,
        on_delete=models.CASCADE,
        related_name="followers",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("user", "room"),
                name="unique_room_follow",
            ),
        ]


class RoomItem(models.Model):
    class Kind(models.TextChoices):
        NOTE = "note", "Note"
        PHOTO = "photo", "Photo"
        VIDEO = "video", "Video"
        MUSIC = "music", "Music"
        PLAN = "plan", "Plan"
        SAVED = "saved", "Saved"

    conversation = models.ForeignKey(
        Conversation,
        on_delete=models.CASCADE,
        related_name="room_items",
    )
    created_by = models.ForeignKey(
        User,
        on_delete=models.SET_NULL,
        related_name="room_items",
        null=True,
        blank=True,
    )
    kind = models.CharField(max_length=12, choices=Kind.choices)
    title = models.CharField(max_length=120, blank=True)
    body = models.CharField(max_length=500, blank=True)
    url = models.URLField(max_length=700, blank=True)
    media = models.FileField(upload_to="rooms/items/%Y/%m/", blank=True)
    scheduled_for = models.DateTimeField(null=True, blank=True)
    pinned = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-pinned", "-created_at", "-id")
        indexes = [
            models.Index(
                fields=("conversation", "-created_at"),
                name="room_item_conv_created_idx",
            ),
            models.Index(
                fields=("conversation", "kind", "-created_at"),
                name="room_item_conv_kind_idx",
            ),
        ]

    def __str__(self):
        return f"Room item {self.pk} ({self.kind}) in {self.conversation_id}"


class RoomReminder(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="room_reminders",
    )
    item = models.ForeignKey(
        RoomItem,
        on_delete=models.CASCADE,
        related_name="reminders",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("item__scheduled_for", "item_id")
        constraints = [
            models.UniqueConstraint(
                fields=("user", "item"),
                name="unique_room_reminder",
            ),
        ]

    def __str__(self):
        return f"Room reminder {self.item_id} for @{self.user.username}"


@receiver(pre_save, sender=RoomItem)
def delete_replaced_room_item_media(sender, instance, **kwargs):
    if not instance.pk:
        return
    previous = sender.objects.filter(pk=instance.pk).only("media").first()
    if previous is None or not previous.media:
        return
    old_name = previous.media.name
    new_name = instance.media.name if instance.media else ""
    if old_name and old_name != new_name:
        storage = previous.media.storage
        transaction.on_commit(lambda storage=storage, name=old_name: storage.delete(name))


@receiver(post_delete, sender=RoomItem)
def delete_removed_room_item_media(sender, instance, **kwargs):
    if instance.media and instance.media.name:
        storage = instance.media.storage
        name = instance.media.name
        transaction.on_commit(lambda storage=storage, name=name: storage.delete(name))
