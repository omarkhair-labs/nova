import uuid

from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import (
    Comment,
    DevicePushToken,
    Follow,
    Like,
    Notification,
    Post,
    User,
    UserBlock,
    UserReport,
)


def users_blocked(first, second):
    first_id = getattr(first, "pk", first)
    second_id = getattr(second, "pk", second)
    if not first_id or not second_id or first_id == second_id:
        return False
    return UserBlock.objects.filter(
        Q(blocker_id=first_id, blocked_id=second_id)
        | Q(blocker_id=second_id, blocked_id=first_id)
    ).exists()


def blocked_user_ids(user):
    outbound = UserBlock.objects.filter(blocker=user).values_list("blocked_id", flat=True)
    inbound = UserBlock.objects.filter(blocked=user).values_list("blocker_id", flat=True)
    return set(outbound).union(inbound)


def visible_active_users_for(user):
    return User.objects.filter(is_active=True).exclude(pk=user.pk).exclude(
        pk__in=blocked_user_ids(user)
    )


def active_person_for(user, username):
    return get_object_or_404(
        User.objects.filter(is_active=True).exclude(pk__in=blocked_user_ids(user)),
        username=username.strip().lower(),
    )


class BlockedUsersView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        from .serializers import PersonSerializer

        blocked = User.objects.filter(
            is_active=True,
            blocks_received__blocker=request.user,
        ).order_by("username")
        return Response(
            {
                "results": PersonSerializer(
                    blocked,
                    many=True,
                    context={"request": request},
                ).data
            }
        )


class UserBlockView(APIView):
    permission_classes = [IsAuthenticated]

    def _target(self, request, username):
        return get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.strip().lower(),
        )

    def post(self, request, username):
        from .privacy_models import CloseFriend, FollowRequest

        target = self._target(request, username)
        if target.pk == request.user.pk:
            return Response(
                {"detail": "You can't block yourself."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        with transaction.atomic():
            block, created = UserBlock.objects.get_or_create(
                blocker=request.user,
                blocked=target,
            )
            Follow.objects.filter(
                Q(follower=request.user, following=target)
                | Q(follower=target, following=request.user)
            ).delete()
            FollowRequest.objects.filter(
                Q(requester=request.user, target=target)
                | Q(requester=target, target=request.user)
            ).delete()
            CloseFriend.objects.filter(
                Q(owner=request.user, member=target)
                | Q(owner=target, member=request.user)
            ).delete()
            Notification.objects.filter(
                Q(recipient=request.user, actor=target)
                | Q(recipient=target, actor=request.user)
            ).delete()

        return Response(
            {"blocked": True},
            status=status.HTTP_201_CREATED if created else status.HTTP_200_OK,
        )

    def delete(self, request, username):
        target = self._target(request, username)
        deleted, _ = UserBlock.objects.filter(
            blocker=request.user,
            blocked=target,
        ).delete()
        return Response({"blocked": False, "removed": bool(deleted)})


class UserReportView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, username):
        target = get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.strip().lower(),
        )
        if target.pk == request.user.pk:
            return Response(
                {"detail": "You can't report yourself."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        reason = str(request.data.get("reason") or "").strip().lower()
        details = str(request.data.get("details") or "").strip()
        if reason not in UserReport.Reason.values:
            return Response(
                {"detail": "Choose a valid report reason."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(details) > 500:
            return Response(
                {"detail": "Report details must be 500 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        report = UserReport.objects.filter(
            reporter=request.user,
            reported=target,
            status=UserReport.Status.OPEN,
        ).first()
        created = report is None
        if report is None:
            report = UserReport.objects.create(
                reporter=request.user,
                reported=target,
                reason=reason,
                details=details,
            )
        else:
            report.reason = reason
            report.details = details
            report.save(update_fields=("reason", "details", "updated_at"))

        return Response(
            {"detail": "Report submitted for review."},
            status=status.HTTP_201_CREATED if created else status.HTTP_200_OK,
        )


class DeleteAccountView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        from ..pulse_models import Pulse
        from .group_messaging import remove_user_from_all_groups
        from .sharing_models import Repost
        from .story_models import Story

        user = request.user
        current_password = str(request.data.get("current_password") or "")
        if not current_password or not user.check_password(current_password):
            return Response(
                {"detail": "Current password is incorrect."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        avatar_name = getattr(user.avatar, "name", "")
        avatar_storage = getattr(user.avatar, "storage", None)
        post_files = [
            (post.image.storage, post.image.name)
            for post in Post.objects.filter(author=user)
            if post.image and post.image.name
        ]
        story_files = [
            (story.media.storage, story.media.name)
            for story in Story.objects.filter(author=user)
            if story.media and story.media.name
        ]
        pulse_files = [
            (pulse.media.storage, pulse.media.name)
            for pulse in Pulse.objects.filter(author=user)
            if pulse.media and pulse.media.name
        ]

        with transaction.atomic():
            remove_user_from_all_groups(user)
            DevicePushToken.objects.filter(user=user).delete()
            Follow.objects.filter(Q(follower=user) | Q(following=user)).delete()
            UserBlock.objects.filter(Q(blocker=user) | Q(blocked=user)).delete()
            Notification.objects.filter(Q(recipient=user) | Q(actor=user)).delete()
            Like.objects.filter(user=user).delete()
            Comment.objects.filter(author=user).delete()
            Repost.objects.filter(user=user).delete()
            Post.objects.filter(author=user).delete()
            Story.objects.filter(author=user).delete()
            Pulse.objects.filter(author=user).delete()

            user.email = f"deleted+{uuid.uuid4().hex}@deleted.nova.invalid"
            user.username = f"deleted_{uuid.uuid4().hex[:16]}"
            user.name = "Deleted user"
            user.first_name = ""
            user.last_name = ""
            user.avatar = ""
            user.last_seen_at = None
            user.is_active = False
            user.set_unusable_password()
            user.save(
                update_fields=(
                    "email",
                    "username",
                    "name",
                    "first_name",
                    "last_name",
                    "avatar",
                    "last_seen_at",
                    "is_active",
                    "password",
                )
            )

            for storage, name in post_files:
                transaction.on_commit(lambda storage=storage, name=name: storage.delete(name))
            for storage, name in story_files:
                transaction.on_commit(lambda storage=storage, name=name: storage.delete(name))
            for storage, name in pulse_files:
                transaction.on_commit(lambda storage=storage, name=name: storage.delete(name))
            if avatar_name and avatar_storage is not None:
                transaction.on_commit(lambda: avatar_storage.delete(avatar_name))

        return Response(
            {
                "detail": (
                    "Your Nova account was deleted. Shared message history is retained "
                    "for other conversation participants without your profile identity."
                )
            }
        )
