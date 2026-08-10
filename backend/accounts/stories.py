import uuid
from collections import OrderedDict
from datetime import timedelta

from django.db import IntegrityError, transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .messaging_realtime import broadcast_message_created
from .messaging_serializers import ConversationSerializer, MessageSerializer
from .messaging_views import conversations_for
from .models import Conversation, Follow, Message
from .push import send_message_push
from .story_models import Story, StoryReaction, StoryView
from .trust_safety import blocked_user_ids, users_blocked

STORY_DURATION = timedelta(hours=24)
MAX_STORY_IMAGE_BYTES = 15 * 1024 * 1024
MAX_STORY_VIDEO_BYTES = 60 * 1024 * 1024
MAX_STORIES_PER_AUTHOR_IN_FEED = 20
MAX_STORY_GROUPS = 100
ALLOWED_STORY_REACTIONS = {"❤️", "😂", "😮", "😢", "🔥", "👏"}


def _absolute_media_url(request, field):
    if not field:
        return ""
    url = field.url
    return request.build_absolute_uri(url)


def _author_payload(request, user):
    return {
        "id": user.pk,
        "username": user.username,
        "name": user.name,
        "avatar_url": _absolute_media_url(request, user.avatar),
    }


def _allowed_story_author_ids(user):
    followed_ids = Follow.objects.filter(follower=user).values_list("following_id", flat=True)
    return [user.pk, *followed_ids]


def visible_stories_for(user):
    blocked_ids = blocked_user_ids(user)
    return Story.objects.select_related("author").filter(
        expires_at__gt=timezone.now(),
        author__is_active=True,
        author_id__in=_allowed_story_author_ids(user),
    ).exclude(author_id__in=blocked_ids)


def visible_story_for_request(request, story_id):
    return get_object_or_404(visible_stories_for(request.user), pk=story_id)


def _story_payload(request, story, viewed_story_ids=None, reaction_by_story=None):
    viewed_story_ids = viewed_story_ids or set()
    reaction_by_story = reaction_by_story or {}
    mine = story.author_id == request.user.pk
    return {
        "id": story.pk,
        "author": _author_payload(request, story.author),
        "media_url": _absolute_media_url(request, story.media),
        "media_type": story.media_type,
        "caption": story.caption,
        "created_at": story.created_at.isoformat(),
        "expires_at": story.expires_at.isoformat(),
        "is_mine": mine,
        "is_viewed": mine or story.pk in viewed_story_ids,
        "my_reaction": reaction_by_story.get(story.pk, ""),
        "views_count": story.views.count() if mine else None,
    }


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


class StoryFeedView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        stories = list(
            visible_stories_for(request.user)
            .order_by("author_id", "created_at", "id")[:2000]
        )
        story_ids = [story.pk for story in stories]
        viewed_story_ids = set(
            StoryView.objects.filter(
                viewer=request.user,
                story_id__in=story_ids,
            ).values_list("story_id", flat=True)
        )
        reaction_by_story = dict(
            StoryReaction.objects.filter(
                user=request.user,
                story_id__in=story_ids,
            ).values_list("story_id", "emoji")
        )

        grouped = OrderedDict()
        for story in stories:
            bucket = grouped.setdefault(story.author_id, [])
            if len(bucket) < MAX_STORIES_PER_AUTHOR_IN_FEED:
                bucket.append(story)

        groups = []
        for author_id, author_stories in grouped.items():
            if not author_stories:
                continue
            latest = author_stories[-1]
            payloads = [
                _story_payload(request, story, viewed_story_ids, reaction_by_story)
                for story in author_stories
            ]
            groups.append(
                {
                    "author": _author_payload(request, latest.author),
                    "stories": payloads,
                    "has_unseen": any(not item["is_viewed"] for item in payloads),
                    "latest_at": latest.created_at.isoformat(),
                    "is_mine": author_id == request.user.pk,
                }
            )

        groups.sort(
            key=lambda item: (
                0 if item["is_mine"] else 1,
                0 if item["has_unseen"] else 1,
                -timezone.datetime.fromisoformat(item["latest_at"]).timestamp(),
            )
        )
        return Response({"results": groups[:MAX_STORY_GROUPS]})

    def post(self, request):
        media = request.FILES.get("media")
        caption = str(request.data.get("caption") or "").strip()
        if media is None:
            return Response(
                {"detail": "Choose a photo or video for your story."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(caption) > 240:
            return Response(
                {"detail": "Story caption must be 240 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        content_type = str(getattr(media, "content_type", "") or "").lower()
        if content_type.startswith("image/"):
            media_type = Story.MediaType.IMAGE
            max_bytes = MAX_STORY_IMAGE_BYTES
            size_message = "Story photo must be 15 MB or smaller."
        elif content_type.startswith("video/"):
            media_type = Story.MediaType.VIDEO
            max_bytes = MAX_STORY_VIDEO_BYTES
            size_message = "Story video must be 60 MB or smaller."
        else:
            return Response(
                {"detail": "Stories support photos and videos only."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if media.size > max_bytes:
            return Response({"detail": size_message}, status=status.HTTP_400_BAD_REQUEST)

        story = Story.objects.create(
            author=request.user,
            media=media,
            media_type=media_type,
            caption=caption,
            expires_at=timezone.now() + STORY_DURATION,
        )
        return Response(
            _story_payload(request, story),
            status=status.HTTP_201_CREATED,
        )


class StoryDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, story_id):
        story = get_object_or_404(Story, pk=story_id, author=request.user)
        story.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class StoryViewedView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, story_id):
        story = visible_story_for_request(request, story_id)
        if story.author_id != request.user.pk:
            StoryView.objects.get_or_create(story=story, viewer=request.user)
        return Response({"story_id": story.pk, "viewed": True})


class StoryViewersView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, story_id):
        story = get_object_or_404(
            Story.objects.select_related("author"),
            pk=story_id,
            author=request.user,
            expires_at__gt=timezone.now(),
        )
        blocked_ids = blocked_user_ids(request.user)
        views = story.views.select_related("viewer").filter(
            viewer__is_active=True,
        ).exclude(viewer_id__in=blocked_ids).order_by("-viewed_at", "-id")[:250]
        reactions = dict(
            StoryReaction.objects.filter(story=story).values_list("user_id", "emoji")
        )
        return Response(
            {
                "story_id": story.pk,
                "views_count": len(views),
                "results": [
                    {
                        "user": _author_payload(request, view.viewer),
                        "viewed_at": view.viewed_at.isoformat(),
                        "reaction": reactions.get(view.viewer_id, ""),
                    }
                    for view in views
                ],
            }
        )


class StoryReactionView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, story_id):
        story = visible_story_for_request(request, story_id)
        if story.author_id == request.user.pk:
            return Response(
                {"detail": "You can't react to your own story."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        emoji = str(request.data.get("emoji") or "").strip()
        if emoji not in ALLOWED_STORY_REACTIONS:
            return Response(
                {"detail": "Unsupported story reaction."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        StoryView.objects.get_or_create(story=story, viewer=request.user)
        reaction, _ = StoryReaction.objects.update_or_create(
            story=story,
            user=request.user,
            defaults={"emoji": emoji},
        )
        return Response({"story_id": story.pk, "reaction": reaction.emoji})

    def delete(self, request, story_id):
        story = visible_story_for_request(request, story_id)
        StoryReaction.objects.filter(story=story, user=request.user).delete()
        return Response({"story_id": story.pk, "reaction": ""})


class StoryReplyView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, story_id):
        story = visible_story_for_request(request, story_id)
        if story.author_id == request.user.pk:
            return Response(
                {"detail": "You can't reply to your own story."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if users_blocked(request.user, story.author) or not story.author.is_active:
            return Response(
                {"detail": "You can't interact with this account."},
                status=status.HTTP_403_FORBIDDEN,
            )

        body = str(request.data.get("body") or "").strip()
        if not body:
            return Response(
                {"detail": "Story reply can't be empty."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(body) > 1000:
            return Response(
                {"detail": "Story reply must be 1000 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        StoryView.objects.get_or_create(story=story, viewer=request.user)
        conversation = _conversation_between(request.user, story.author)
        message = Message.objects.create(
            conversation=conversation,
            sender=request.user,
            recipient=story.author,
            body=f"↳ Story reply\n{body}",
            client_id=f"story-{story.pk}-{uuid.uuid4().hex}",
        )
        Conversation.objects.filter(pk=conversation.pk).update(updated_at=timezone.now())
        message = Message.objects.select_related("sender", "reply_to", "reply_to__sender").get(pk=message.pk)
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
