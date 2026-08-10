import uuid

from django.contrib.auth import get_user_model
from django.db import IntegrityError, transaction
from django.db.models import (
    Case,
    DateTimeField,
    Exists,
    F,
    OuterRef,
    Q,
    Subquery,
    When,
)
from django.db.models.functions import Greatest
from django.shortcuts import get_object_or_404
from django.utils import timezone
from django.utils.dateparse import parse_datetime
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .messaging_realtime import broadcast_message_created
from .messaging_serializers import ConversationSerializer, MessageSerializer
from .messaging_views import conversations_for
from .models import Conversation, Follow, Message
from .privacy import can_view_user_content
from .push import send_message_push
from .serializers import PostSerializer
from .sharing_models import MessageShare, Repost
from .trust_safety import active_person_for, blocked_user_ids, users_blocked
from .views import public_post_queryset

User = get_user_model()
SHARING_FEED_PAGE_SIZE = 20


def _conversation_between(first_user, second_user):
    first_id, second_id = sorted((first_user.pk, second_user.pk))
    try:
        with transaction.atomic():
            conversation, _ = Conversation.objects.get_or_create(
                participant_one_id=first_id,
                participant_two_id=second_id,
            )
    except IntegrityError:
        conversation = Conversation.objects.get(
            participant_one_id=first_id,
            participant_two_id=second_id,
        )
    return conversations_for(first_user).get(pk=conversation.pk)


def _feed_audience_ids(user):
    followed = Follow.objects.filter(follower=user).values_list("following_id", flat=True)
    return [user.pk, *followed]


def _repost_state(request, post):
    blocked_ids = blocked_user_ids(request.user)
    audience_ids = _feed_audience_ids(request.user)
    visible_reposts = post.reposts.select_related("user").filter(user__is_active=True).exclude(
        user_id__in=blocked_ids
    )
    audience_reposts = visible_reposts.filter(user_id__in=audience_ids).order_by("-created_at", "-id")
    latest = audience_reposts.first()
    direct_visible = post.author_id in audience_ids
    return {
        "post_id": post.pk,
        "reposts_count": visible_reposts.count(),
        "is_reposted": Repost.objects.filter(user=request.user, post=post).exists(),
        "still_in_feed": direct_visible or audience_reposts.exists(),
        "feed_reposted_by": (
            {
                "id": latest.user.pk,
                "username": latest.user.username,
                "name": latest.user.name,
                "avatar_url": latest.user.avatar.url if latest.user.avatar else "",
            }
            if latest is not None and (not direct_visible or latest.created_at > post.created_at)
            else None
        ),
    }


class SharingFeedView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        audience_ids = _feed_audience_ids(request.user)
        latest_repost = Repost.objects.filter(
            post_id=OuterRef("pk"),
            user_id__in=audience_ids,
            user__is_active=True,
        ).exclude(user_id__in=blocked_user_ids(request.user)).order_by("-created_at", "-id")

        queryset = (
            public_post_queryset(request)
            .annotate(
                latest_repost_at=Subquery(latest_repost.values("created_at")[:1]),
                latest_reposter_id=Subquery(latest_repost.values("user_id")[:1]),
                has_audience_repost=Exists(latest_repost),
            )
            .filter(Q(author_id__in=audience_ids) | Q(has_audience_repost=True))
            .annotate(
                feed_event_at=Case(
                    When(
                        author_id__in=audience_ids,
                        latest_repost_at__isnull=False,
                        then=Greatest(F("created_at"), F("latest_repost_at")),
                    ),
                    When(author_id__in=audience_ids, then=F("created_at")),
                    default=F("latest_repost_at"),
                    output_field=DateTimeField(),
                )
            )
        )

        raw_cursor = request.query_params.get("cursor", "").strip()
        if raw_cursor:
            if "|" not in raw_cursor:
                try:
                    legacy_post_id = int(raw_cursor)
                except ValueError:
                    legacy_post_id = 0
                if legacy_post_id <= 0:
                    return Response(
                        {"detail": "Invalid feed cursor."},
                        status=status.HTTP_400_BAD_REQUEST,
                    )
                queryset = queryset.filter(id__lt=legacy_post_id)
            else:
                try:
                    raw_time, raw_id = raw_cursor.rsplit("|", 1)
                    cursor_time = parse_datetime(raw_time)
                    cursor_id = int(raw_id)
                except (TypeError, ValueError):
                    cursor_time = None
                    cursor_id = 0
                if cursor_time is None or cursor_id <= 0:
                    return Response(
                        {"detail": "Invalid feed cursor."},
                        status=status.HTTP_400_BAD_REQUEST,
                    )
                queryset = queryset.filter(
                    Q(feed_event_at__lt=cursor_time)
                    | Q(feed_event_at=cursor_time, id__lt=cursor_id)
                )

        page_with_extra = list(
            queryset.select_related("author").order_by("-feed_event_at", "-id")[: SHARING_FEED_PAGE_SIZE + 1]
        )
        has_more = len(page_with_extra) > SHARING_FEED_PAGE_SIZE
        page = page_with_extra[:SHARING_FEED_PAGE_SIZE]

        reposter_ids = {
            post.latest_reposter_id
            for post in page
            if post.latest_reposter_id
            and (
                post.author_id not in audience_ids
                or (post.latest_repost_at and post.latest_repost_at > post.created_at)
            )
        }
        reposters = User.objects.filter(pk__in=reposter_ids, is_active=True).in_bulk()
        for post in page:
            reposter_id = post.latest_reposter_id
            should_show_reposter = bool(
                reposter_id
                and (
                    post.author_id not in audience_ids
                    or (post.latest_repost_at and post.latest_repost_at > post.created_at)
                )
            )
            post.feed_reposted_by_value = reposters.get(reposter_id) if should_show_reposter else None

        next_cursor = (
            f"{page[-1].feed_event_at.isoformat()}|{page[-1].id}"
            if has_more and page
            else None
        )
        return Response(
            {
                "results": PostSerializer(page, many=True, context={"request": request}).data,
                "next_cursor": next_cursor,
            }
        )


class PostRepostView(APIView):
    permission_classes = [IsAuthenticated]

    def _post(self, request, post_id):
        return get_object_or_404(public_post_queryset(request), pk=post_id)

    def get(self, request, post_id):
        return Response(_repost_state(request, self._post(request, post_id)))

    def post(self, request, post_id):
        post = self._post(request, post_id)
        Repost.objects.get_or_create(user=request.user, post=post)
        return Response(_repost_state(request, post))

    def delete(self, request, post_id):
        post = self._post(request, post_id)
        Repost.objects.filter(user=request.user, post=post).delete()
        return Response(_repost_state(request, post))


class MessageShareView(APIView):
    permission_classes = [IsAuthenticated]

    def _destination(self, request):
        recipient_username = str(request.data.get("recipient_username") or "").strip().lower()
        raw_conversation_id = request.data.get("conversation_id")
        try:
            conversation_id = int(raw_conversation_id) if raw_conversation_id not in (None, "") else 0
        except (TypeError, ValueError):
            conversation_id = 0

        if bool(recipient_username) == bool(conversation_id):
            return None, None, Response(
                {"detail": "Choose exactly one person or group to share with."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if recipient_username:
            recipient = get_object_or_404(
                User.objects.filter(is_active=True),
                username=recipient_username,
            )
            if recipient.pk == request.user.pk:
                return None, None, Response(
                    {"detail": "You can't message yourself."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if users_blocked(request.user, recipient):
                return None, None, Response(
                    {"detail": "You can't interact with this account."},
                    status=status.HTTP_403_FORBIDDEN,
                )
            return _conversation_between(request.user, recipient), recipient, None

        conversation = get_object_or_404(
            conversations_for(request.user),
            pk=conversation_id,
            kind=Conversation.Kind.GROUP,
        )
        return conversation, None, None

    def _visible_group_recipients(self, request, conversation):
        if conversation.kind != Conversation.Kind.GROUP:
            return []
        return [
            membership.user
            for membership in conversation.group_memberships.select_related("user")
            .filter(user__is_active=True)
            .exclude(user=request.user)
            if not users_blocked(request.user, membership.user)
        ]

    def post(self, request):
        conversation, direct_recipient, destination_error = self._destination(request)
        if destination_error is not None:
            return destination_error

        group_recipients = self._visible_group_recipients(request, conversation)
        effective_recipients = [direct_recipient] if direct_recipient is not None else group_recipients

        kind = str(request.data.get("kind") or "").strip().lower()
        shared_post = None
        shared_profile = None

        if kind == MessageShare.Kind.POST:
            try:
                post_id = int(request.data.get("post_id"))
            except (TypeError, ValueError):
                post_id = 0
            shared_post = get_object_or_404(public_post_queryset(request), pk=post_id)

            unavailable = [
                recipient
                for recipient in effective_recipients
                if recipient is not None
                and (
                    users_blocked(recipient, shared_post.author)
                    or not can_view_user_content(recipient, shared_post.author)
                )
            ]
            if unavailable:
                detail = (
                    "That post isn't available to everyone in this group."
                    if conversation.kind == Conversation.Kind.GROUP
                    else "That post isn't available to this recipient."
                )
                return Response({"detail": detail}, status=status.HTTP_403_FORBIDDEN)
            marker = f"↗ Shared post by @{shared_post.author.username}"

        elif kind == MessageShare.Kind.PROFILE:
            profile_username = str(request.data.get("profile_username") or "").strip().lower()
            if not profile_username:
                return Response(
                    {"detail": "Choose a profile to share."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            shared_profile = active_person_for(request.user, profile_username)
            unavailable = [
                recipient
                for recipient in effective_recipients
                if recipient is not None and users_blocked(recipient, shared_profile)
            ]
            if unavailable:
                detail = (
                    "That profile isn't available to everyone in this group."
                    if conversation.kind == Conversation.Kind.GROUP
                    else "That profile isn't available to this recipient."
                )
                return Response({"detail": detail}, status=status.HTTP_403_FORBIDDEN)
            marker = f"↗ Shared profile · @{shared_profile.username}"

        else:
            return Response(
                {"detail": "Unsupported share type."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        with transaction.atomic():
            message = Message.objects.create(
                conversation=conversation,
                sender=request.user,
                recipient=direct_recipient,
                body=marker,
                client_id=f"share-{uuid.uuid4().hex}",
            )
            MessageShare.objects.create(
                message=message,
                kind=kind,
                post=shared_post,
                profile=shared_profile,
            )
            Conversation.objects.filter(pk=conversation.pk).update(updated_at=timezone.now())

        message = Message.objects.select_related(
            "sender",
            "recipient",
            "shared_content",
            "shared_content__post__author",
            "shared_content__profile",
        ).get(pk=message.pk)
        broadcast_message_created(message)
        send_message_push(message)
        return Response(
            {
                "conversation": ConversationSerializer(
                    conversation,
                    context={"request": request},
                ).data,
                "message": MessageSerializer(
                    message,
                    context={"request": request},
                ).data,
            },
            status=status.HTTP_201_CREATED,
        )
