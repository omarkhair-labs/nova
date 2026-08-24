from django.db.models import Q

from .models import Follow, User
from .privacy_models import AccountPrivacy, CloseFriend, FollowRequest
from .trust_safety import blocked_user_ids, users_blocked


def is_private_account(user):
    try:
        return bool(user.account_privacy.is_private)
    except AccountPrivacy.DoesNotExist:
        return False


def can_view_user_content(viewer, owner):
    if not viewer or not owner or not getattr(owner, "is_active", False):
        return False
    if viewer.pk == owner.pk:
        return True
    if users_blocked(viewer, owner):
        return False
    if not is_private_account(owner):
        return True
    return Follow.objects.filter(follower=viewer, following=owner).exists()


def accessible_content_owner_filter(viewer):
    followed_ids = Follow.objects.filter(follower=viewer).values_list("following_id", flat=True)
    return (
        Q(author=viewer)
        | Q(author__account_privacy__isnull=True)
        | Q(author__account_privacy__is_private=False)
        | Q(author_id__in=followed_ids)
    )


def pending_follow_request(viewer, target):
    if not viewer or not target or viewer.pk == target.pk:
        return False
    return FollowRequest.objects.filter(requester=viewer, target=target).exists()


def visible_close_friend_ids(owner):
    blocked_ids = blocked_user_ids(owner)
    follower_ids = Follow.objects.filter(following=owner).values_list("follower_id", flat=True)
    return CloseFriend.objects.filter(
        owner=owner,
        member__is_active=True,
        member_id__in=follower_ids,
    ).exclude(member_id__in=blocked_ids).values_list("member_id", flat=True)


def close_friend_story_visible_to(viewer, owner):
    if viewer.pk == owner.pk:
        return True
    if not Follow.objects.filter(follower=viewer, following=owner).exists():
        return False
    return CloseFriend.objects.filter(owner=owner, member=viewer).exists()


def privacy_payload(user):
    privacy, _ = AccountPrivacy.objects.get_or_create(user=user)
    return {
        "is_private": privacy.is_private,
        "show_activity_status": privacy.show_activity_status,
        "send_read_receipts": privacy.send_read_receipts,
        "story_audience": privacy.story_audience,
        "pending_follow_requests": FollowRequest.objects.filter(target=user).exclude(
            requester_id__in=blocked_user_ids(user)
        ).count(),
        "close_friends_count": CloseFriend.objects.filter(
            owner=user,
            member_id__in=visible_close_friend_ids(user),
        ).count(),
    }
