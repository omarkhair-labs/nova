import base64
import json

from django.db.models import Q
from django.utils.dateparse import parse_datetime
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ..models import Comment, Follow, Like, Post
from ..privacy import accessible_content_owner_filter
from ..pulse.views import visible_pulses_for
from ..sharing_models import Repost
from ..trust_safety import blocked_user_ids


ORBIT_PAGE_SIZE = 24
ORBIT_SOURCE_WINDOW = 80
ORBIT_KIND_RANK = {
    "pulse_reply": 5,
    "comment": 4,
    "repost": 3,
    "like": 2,
    "follow": 1,
}


def _media_url(request, field):
    if not field:
        return ""
    url = field.url
    return request.build_absolute_uri(url)


def _person_payload(request, user):
    return {
        "id": user.pk,
        "username": user.username,
        "name": user.name,
        "avatar_url": _media_url(request, user.avatar),
    }


def _post_payload(request, post):
    return {
        "id": post.pk,
        "author": _person_payload(request, post.author),
        "image_url": _media_url(request, post.image),
        "caption": post.caption,
    }


def _pulse_payload(request, pulse):
    return {
        "id": pulse.pk,
        "author": _person_payload(request, pulse.author),
        "media_url": _media_url(request, pulse.media),
        "media_type": pulse.media_type,
        "note": pulse.note,
        "reply_to_id": pulse.reply_to_id,
        "chain_root_id": pulse.chain_root_id,
    }


def _event(kind, source_id, actor, created_at, request, **payload):
    return {
        "id": f"{kind}:{source_id}",
        "kind": kind,
        "actor": _person_payload(request, actor),
        "created_at": created_at.isoformat(),
        "post": payload.get("post"),
        "person": payload.get("person"),
        "pulse": payload.get("pulse"),
        "comment_preview": payload.get("comment_preview", ""),
        "_created_at": created_at,
        "_rank": ORBIT_KIND_RANK[kind],
        "_source_id": source_id,
    }


def _sort_key(event):
    return (event["_created_at"], event["_rank"], event["_source_id"])


def _encode_cursor(event):
    raw = json.dumps(
        {
            "at": event["_created_at"].isoformat(),
            "rank": event["_rank"],
            "id": event["_source_id"],
        },
        separators=(",", ":"),
    ).encode("utf-8")
    return base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")


def _decode_cursor(raw):
    try:
        padded = raw + "=" * (-len(raw) % 4)
        data = json.loads(base64.urlsafe_b64decode(padded.encode("ascii")).decode("utf-8"))
        created_at = parse_datetime(str(data["at"]))
        rank = int(data["rank"])
        source_id = int(data["id"])
        if created_at is None or rank not in ORBIT_KIND_RANK.values() or source_id <= 0:
            return None
        return created_at, rank, source_id
    except (KeyError, TypeError, ValueError, json.JSONDecodeError):
        return None


def _collect_orbit_events(request, before=None):
    viewer = request.user
    blocked_ids = blocked_user_ids(viewer)
    actor_ids = list(
        Follow.objects.filter(
            follower=viewer,
            following__is_active=True,
        )
        .exclude(following_id__in=blocked_ids)
        .values_list("following_id", flat=True)
    )
    if not actor_ids:
        return []

    visible_post_ids = (
        Post.objects.filter(author__is_active=True)
        .exclude(author_id__in=blocked_ids)
        .filter(accessible_content_owner_filter(viewer))
        .values_list("pk", flat=True)
    )

    events = []

    likes = Like.objects.select_related("user", "post__author").filter(
        user_id__in=actor_ids,
        post_id__in=visible_post_ids,
    )
    comments = Comment.objects.select_related("author", "post__author").filter(
        author_id__in=actor_ids,
        post_id__in=visible_post_ids,
    )
    reposts = Repost.objects.select_related("user", "post__author").filter(
        user_id__in=actor_ids,
        post_id__in=visible_post_ids,
    )

    follows = (
        Follow.objects.select_related("follower", "following")
        .filter(
            follower_id__in=actor_ids,
            following__is_active=True,
        )
        .exclude(following_id__in=blocked_ids)
        .filter(
            Q(following=viewer)
            | Q(following__account_privacy__isnull=True)
            | Q(following__account_privacy__is_private=False)
            | Q(following_id__in=actor_ids)
        )
    )

    visible_pulses = visible_pulses_for(viewer)
    visible_pulse_ids = visible_pulses.values_list("pk", flat=True)
    pulse_replies = (
        visible_pulses.select_related("author", "reply_to__author")
        .filter(
            author_id__in=actor_ids,
            reply_to__isnull=False,
            reply_to_id__in=visible_pulse_ids,
        )
    )

    if before is not None:
        created_at = before[0]
        likes = likes.filter(created_at__lte=created_at)
        comments = comments.filter(created_at__lte=created_at)
        reposts = reposts.filter(created_at__lte=created_at)
        follows = follows.filter(created_at__lte=created_at)
        pulse_replies = pulse_replies.filter(created_at__lte=created_at)

    for like in likes.order_by("-created_at", "-id")[:ORBIT_SOURCE_WINDOW]:
        events.append(
            _event(
                "like",
                like.pk,
                like.user,
                like.created_at,
                request,
                post=_post_payload(request, like.post),
            )
        )

    for comment in comments.order_by("-created_at", "-id")[:ORBIT_SOURCE_WINDOW]:
        events.append(
            _event(
                "comment",
                comment.pk,
                comment.author,
                comment.created_at,
                request,
                post=_post_payload(request, comment.post),
                comment_preview=comment.body[:120],
            )
        )

    for repost in reposts.order_by("-created_at", "-id")[:ORBIT_SOURCE_WINDOW]:
        events.append(
            _event(
                "repost",
                repost.pk,
                repost.user,
                repost.created_at,
                request,
                post=_post_payload(request, repost.post),
            )
        )

    for follow in follows.order_by("-created_at", "-id")[:ORBIT_SOURCE_WINDOW]:
        events.append(
            _event(
                "follow",
                follow.pk,
                follow.follower,
                follow.created_at,
                request,
                person=_person_payload(request, follow.following),
            )
        )

    for pulse in pulse_replies.order_by("-created_at", "-id")[:ORBIT_SOURCE_WINDOW]:
        events.append(
            _event(
                "pulse_reply",
                pulse.pk,
                pulse.author,
                pulse.created_at,
                request,
                pulse=_pulse_payload(request, pulse),
                person=_person_payload(request, pulse.reply_to.author),
            )
        )

    if before is not None:
        events = [event for event in events if _sort_key(event) < before]

    events.sort(key=_sort_key, reverse=True)
    return events


def _public_event(event):
    return {key: value for key, value in event.items() if not key.startswith("_")}


class OrbitFeedView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        raw_cursor = request.query_params.get("cursor", "").strip()
        before = None
        if raw_cursor:
            before = _decode_cursor(raw_cursor)
            if before is None:
                return Response(
                    {"detail": "Invalid Orbit cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        events = _collect_orbit_events(request, before=before)
        page_with_extra = events[: ORBIT_PAGE_SIZE + 1]
        has_more = len(page_with_extra) > ORBIT_PAGE_SIZE
        page = page_with_extra[:ORBIT_PAGE_SIZE]
        next_cursor = _encode_cursor(page[-1]) if has_more and page else None

        return Response(
            {
                "results": [_public_event(event) for event in page],
                "next_cursor": next_cursor,
            }
        )
