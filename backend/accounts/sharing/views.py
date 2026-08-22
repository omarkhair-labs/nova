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

from ..messaging.messaging_realtime import broadcast_message_created
from ..messaging.messaging_serializers import ConversationSerializer, MessageSerializer
from ..messaging.messaging_views import conversations_for
from ..models import Conversation, Follow, Message
from ..posts.views import public_post_queryset
from ..privacy import can_view_user_content
from ..push import send_message_push
from ..reels import visible_reels_for
from ..serializers import PostSerializer
from ..sharing_models import MessageShare, Repost
from ..trust_safety import active_person_for, blocked_user_ids, users_blocked

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


def _shared_post_payload(request, post):
    if post is None:
        return None
    return PostSerializer(post, context={"request": request}).data


def _shared_reel_payload(request, reel):
    if reel is None:
        return None
    return {
        "id": reel.pk,
        "author": {
            "id": reel.author_id,
            "username": reel.author.username,
            "name": reel.author.name,
            "avatar_url": request.build_absolute_uri(reel.author.avatar.url)
            if reel.author.avatar
            else "",
        },
        "video_url": request.build_absolute_uri(reel.video.url) if reel.video else "",
        "caption": reel.caption,
    }


def _message_payload(request, message, viewer):
    return MessageSerializer(
        message,
        context={"request": request, "viewer": viewer},
    ).data


def _ensure_message_access(user, conversation):
    return conversations_for(user).filter(pk=conversation.pk).exists()


class PostRepostView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, post_id):
        post = get_object_or_404(public_post_queryset(request.user), pk=post_id)
        repost, created = Repost.objects.get_or_create(user=request.user, post=post)
        if not created:
            return Response({"detail": "Already reposted."}, status=status.HTTP_200_OK)
        return Response({"detail": "Reposted."}, status=status.HTTP_201_CREATED)

    def delete(self, request, post_id):
        deleted, _ = Repost.objects.filter(user=request.user, post_id=post_id).delete()
        if deleted == 0:
            return Response({"detail": "Repost not found."}, status=status.HTTP_404_NOT_FOUND)
        return Response(status=status.HTTP_204_NO_CONTENT)


class SharingFeedView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        cursor = parse_datetime(request.query_params.get("cursor", ""))
        if cursor is not None and timezone.is_naive(cursor):
            cursor = timezone.make_aware(cursor, timezone.get_current_timezone())

        following_ids = request.user.following_relations.values_list("following_id", flat=True)
        visible_author_ids = [request.user.pk, *following_ids]
        blocked_ids = blocked_user_ids(request.user)
        visible_author_ids = [user_id for user_id in visible_author_ids if user_id not in blocked_ids]

        reposts = (
            Repost.objects.filter(user_id__in=visible_author_ids)
            .select_related("user", "post", "post__author")
            .annotate(item_time=F("created_at"))
        )
        if cursor:
            reposts = reposts.filter(item_time__lt=cursor)

        rows = []
        for repost in reposts.order_by("-item_time")[:SHARING_FEED_PAGE_SIZE]:
            if not active_person_for(repost.user):
                continue
            if repost.post.author_id in blocked_ids:
                continue
            if not can_view_user_content(request.user, repost.post.author):
                continue
            rows.append(
                {
                    "kind": "repost",
                    "created_at": repost.item_time,
                    "actor": {
                        "id": repost.user_id,
                        "username": repost.user.username,
                        "name": repost.user.name,
                    },
                    "post": _shared_post_payload(request, repost.post),
                }
            )

        rows.sort(key=lambda row: row["created_at"], reverse=True)
        rows = rows[:SHARING_FEED_PAGE_SIZE]
        next_cursor = rows[-1]["created_at"].isoformat() if len(rows) == SHARING_FEED_PAGE_SIZE else None
        for row in rows:
            row["created_at"] = row["created_at"].isoformat()
        return Response({"results": rows, "next_cursor": next_cursor})


class MessageShareView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, conversation_id):
        conversation = get_object_or_404(Conversation, pk=conversation_id)
        if not _ensure_message_access(request.user, conversation):
            return Response({"detail": "Conversation not found."}, status=status.HTTP_404_NOT_FOUND)

        post = None
        reel = None
        post_id = request.data.get("post_id")
        reel_id = request.data.get("reel_id")
        text = str(request.data.get("text", "")).strip()
        if bool(post_id) == bool(reel_id):
            return Response(
                {"detail": "Exactly one of post_id or reel_id is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if post_id:
            post = get_object_or_404(public_post_queryset(request.user), pk=post_id)
        else:
            reel = get_object_or_404(visible_reels_for(request.user), pk=reel_id)

        with transaction.atomic():
            message = Message.objects.create(
                conversation=conversation,
                sender=request.user,
                client_message_id=uuid.uuid4(),
                text=text,
                shared_post=post,
                shared_reel=reel,
            )
            MessageShare.objects.create(
                message=message,
                shared_by=request.user,
                post=post,
                reel=reel,
            )

        payload = _message_payload(request, message, request.user)
        broadcast_message_created(conversation.pk, payload)
        send_message_push(message)
        return Response(payload, status=status.HTTP_201_CREATED)
