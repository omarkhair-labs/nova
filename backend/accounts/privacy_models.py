from django.db import models
from django.db.models import F, Q
from django.db.models.signals import post_delete
from django.dispatch import receiver

from .models import Follow, User


class AccountPrivacy(models.Model):
    class StoryAudience(models.TextChoices):
        FOLLOWERS = "followers", "Followers"
        CLOSE_FRIENDS = "close_friends", "Close friends"

    user = models.OneToOneField(
        User,
        on_delete=models.CASCADE,
        related_name="account_privacy",
    )
    is_private = models.BooleanField(default=False)
    show_activity_status = models.BooleanField(default=True)
    send_read_receipts = models.BooleanField(default=True)
    story_audience = models.CharField(
        max_length=16,
        choices=StoryAudience.choices,
        default=StoryAudience.FOLLOWERS,
    )
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"

    def __str__(self):
        return f"Privacy for @{self.user.username}"


class NotificationPreference(models.Model):
    user = models.OneToOneField(
        User,
        on_delete=models.CASCADE,
        related_name="notification_preferences",
    )
    likes_comments_shares = models.BooleanField(default=True)
    mentions_tags = models.BooleanField(default=True)
    followers = models.BooleanField(default=True)
    messages = models.BooleanField(default=True)
    live_sessions = models.BooleanField(default=True)
    reels_stories = models.BooleanField(default=True)
    events_spaces = models.BooleanField(default=True)
    product_updates = models.BooleanField(default=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        app_label = "accounts"

    def __str__(self):
        return f"Notification preferences for @{self.user.username}"


class FollowRequest(models.Model):
    requester = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="follow_requests_sent",
    )
    target = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="follow_requests_received",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("requester", "target"),
                name="unique_follow_request",
            ),
            models.CheckConstraint(
                condition=~Q(requester=F("target")),
                name="prevent_self_follow_request",
            ),
        ]
        indexes = [
            models.Index(fields=("target", "-created_at"), name="follow_req_target_time_idx"),
            models.Index(fields=("requester", "target"), name="follow_req_pair_idx"),
        ]

    def __str__(self):
        return f"@{self.requester.username} requested @{self.target.username}"


class CloseFriend(models.Model):
    owner = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="close_friends_created",
    )
    member = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="close_friend_memberships",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        app_label = "accounts"
        ordering = ("-created_at", "-id")
        constraints = [
            models.UniqueConstraint(
                fields=("owner", "member"),
                name="unique_close_friend",
            ),
            models.CheckConstraint(
                condition=~Q(owner=F("member")),
                name="prevent_self_close_friend",
            ),
        ]
        indexes = [
            models.Index(fields=("owner", "-created_at"), name="close_friend_owner_time_idx"),
            models.Index(fields=("owner", "member"), name="close_friend_pair_idx"),
        ]

    def __str__(self):
        return f"@{self.member.username} is close friend of @{self.owner.username}"


@receiver(post_delete, sender=Follow)
def remove_close_friend_when_follow_ends(sender, instance, **kwargs):
    CloseFriend.objects.filter(
        owner_id=instance.following_id,
        member_id=instance.follower_id,
    ).delete()
