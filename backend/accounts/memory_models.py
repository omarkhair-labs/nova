from django.db import models, transaction
from django.db.models.signals import post_delete, pre_save
from django.dispatch import receiver

from .models import User


class MemoryDraft(models.Model):
    class Kind(models.TextChoices):
        RECAP = "recap", "Recap"
        FILM = "film", "Film"

    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="memory_drafts",
    )
    kind = models.CharField(max_length=8, choices=Kind.choices, default=Kind.RECAP)
    title = models.CharField(max_length=120)
    note = models.CharField(max_length=500, blank=True)
    media = models.FileField(upload_to="memories/drafts/%Y/%m/", blank=True)
    media_type = models.CharField(max_length=16, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-updated_at", "-id")
        indexes = [
            models.Index(fields=("user", "-updated_at"), name="memory_draft_user_updated_idx"),
        ]


@receiver(pre_save, sender=MemoryDraft)
def delete_replaced_memory_draft_media(sender, instance, **kwargs):
    if not instance.pk:
        return
    previous = sender.objects.filter(pk=instance.pk).only("media").first()
    if previous is None or not previous.media:
        return
    old_name = previous.media.name
    new_name = instance.media.name if instance.media else ""
    if old_name and old_name != new_name:
        transaction.on_commit(lambda: previous.media.storage.delete(old_name))


@receiver(post_delete, sender=MemoryDraft)
def delete_removed_memory_draft_media(sender, instance, **kwargs):
    if instance.media and instance.media.name:
        storage = instance.media.storage
        name = instance.media.name
        transaction.on_commit(lambda: storage.delete(name))

