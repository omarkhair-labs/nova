from django.db.models import Count, Max
from django.utils import timezone

from ..messaging.messaging_models import group_avatar_url
from ..models import Post, User
from ..pulse_models import Pulse
from ..room_models import RoomItem
from ..serializers import PostAuthorSerializer
from ..trust_safety import blocked_user_ids
from .window import completed_week_window, local_night_key


MEMORY_SOURCE_LIMIT = 80
MEMORY_HIGHLIGHT_LIMIT = 120
MEMORY_PEOPLE_LIMIT = 20
MEMORY_ROOMS_LIMIT = 20


def _media_url(request, field):
    if not field or not getattr(field, "name", ""):
        return ""
    try:
        url = field.url
    except Exception:
        return ""
    return request.build_absolute_uri(url)


def _person_payload(request, person):
    if person is None:
        return None
    return PostAuthorSerializer(person, context={"request": request}).data


def _room_payload(request, conversation):
    return {
        "id": conversation.id,
        "title": conversation.title or "Nova Room",
        "avatar_url": group_avatar_url(request, conversation),
    }


def _pulse_highlight(request, pulse):
    return {
        "source": "pulse",
        "id": pulse.id,
        "occurred_at": pulse.created_at.isoformat(),
        "title": "Moment reply" if pulse.reply_to_id else "Pulse",
        "text": pulse.note,
        "url": "",
        "media_type": pulse.media_type,
        "media_url": _media_url(request, pulse.media),
        "person": _person_payload(request, pulse.author),
        "room": None,
    }


def _post_highlight(request, post):
    return {
        "source": "post",
        "id": post.id,
        "occurred_at": post.created_at.isoformat(),
        "title": "Post",
        "text": post.caption,
        "url": "",
        "media_type": "image",
        "media_url": _media_url(request, post.image),
        "person": _person_payload(request, post.author),
        "room": None,
    }


def _room_item_highlight(request, item):
    media_type = {
        RoomItem.Kind.PHOTO: "image",
        RoomItem.Kind.VIDEO: "video",
    }.get(item.kind, "none")
    return {
        "source": "room_item",
        "id": item.id,
        "occurred_at": item.created_at.isoformat(),
        "title": item.title or item.get_kind_display(),
        "text": item.body,
        "url": item.url,
        "media_type": media_type,
        "media_url": _media_url(request, item.media),
        "person": _person_payload(request, item.created_by),
        "room": _room_payload(request, item.conversation),
    }


def build_weekly_memory(request, utc_offset_minutes, weeks_ago=0, now=None):
    now = now or timezone.now()
    window = completed_week_window(now, utc_offset_minutes, weeks_ago)
    starts_at = window["starts_at"]
    ends_at = window["ends_at"]

    pulse_qs = Pulse.objects.filter(
        author=request.user,
        created_at__gte=starts_at,
        created_at__lt=ends_at,
    ).select_related("author")
    post_qs = Post.objects.filter(
        author=request.user,
        created_at__gte=starts_at,
        created_at__lt=ends_at,
    ).select_related("author")

    hidden_ids = blocked_user_ids(request.user)
    room_qs = RoomItem.objects.filter(
        conversation__group_memberships__user=request.user,
        created_at__gte=starts_at,
        created_at__lt=ends_at,
    ).select_related(
        "created_by",
        "conversation",
        "conversation__group_profile",
    ).distinct()
    if hidden_ids:
        room_qs = room_qs.exclude(created_by_id__in=hidden_ids)

    pulse_count = pulse_qs.count()
    post_count = post_qs.count()
    room_item_count = room_qs.count()
    room_count = room_qs.values("conversation_id").distinct().count()
    people_count = (
        room_qs.exclude(created_by__isnull=True)
        .exclude(created_by=request.user)
        .values("created_by_id")
        .distinct()
        .count()
    )

    activity_times = list(pulse_qs.values_list("created_at", flat=True))
    activity_times.extend(post_qs.values_list("created_at", flat=True))
    activity_times.extend(room_qs.values_list("created_at", flat=True))
    night_keys = {
        key
        for timestamp in activity_times
        if (key := local_night_key(timestamp, utc_offset_minutes)) is not None
    }

    highlights = []
    for pulse in pulse_qs.order_by("-created_at", "-id")[:MEMORY_SOURCE_LIMIT]:
        row = _pulse_highlight(request, pulse)
        row["_timestamp"] = pulse.created_at
        highlights.append(row)
    for post in post_qs.order_by("-created_at", "-id")[:MEMORY_SOURCE_LIMIT]:
        row = _post_highlight(request, post)
        row["_timestamp"] = post.created_at
        highlights.append(row)
    for item in room_qs.order_by("-created_at", "-id")[:MEMORY_SOURCE_LIMIT]:
        row = _room_item_highlight(request, item)
        row["_timestamp"] = item.created_at
        highlights.append(row)

    highlights.sort(key=lambda row: row["_timestamp"], reverse=True)
    highlights = highlights[:MEMORY_HIGHLIGHT_LIMIT]
    highlights.sort(key=lambda row: row["_timestamp"])
    for row in highlights:
        row.pop("_timestamp", None)

    people_rows = list(
        room_qs.exclude(created_by__isnull=True)
        .exclude(created_by=request.user)
        .values("created_by_id")
        .annotate(shared_count=Count("id"), latest_at=Max("created_at"))
        .order_by("-shared_count", "-latest_at")[:MEMORY_PEOPLE_LIMIT]
    )
    people_by_id = User.objects.in_bulk([row["created_by_id"] for row in people_rows])
    people = [
        {
            "person": _person_payload(request, people_by_id.get(row["created_by_id"])),
            "shared_count": row["shared_count"],
        }
        for row in people_rows
        if people_by_id.get(row["created_by_id"]) is not None
    ]

    room_rows = list(
        room_qs.values("conversation_id")
        .annotate(shared_count=Count("id"), latest_at=Max("created_at"))
        .order_by("-shared_count", "-latest_at")[:MEMORY_ROOMS_LIMIT]
    )
    room_ids = [row["conversation_id"] for row in room_rows]
    room_by_id = {
        item.conversation_id: item.conversation
        for item in room_qs.filter(conversation_id__in=room_ids).order_by("conversation_id")
    }
    rooms = [
        {
            "room": _room_payload(request, room_by_id[row["conversation_id"]]),
            "shared_count": row["shared_count"],
        }
        for row in room_rows
        if row["conversation_id"] in room_by_id
    ]

    return {
        "starts_at": starts_at.isoformat(),
        "ends_at": ends_at.isoformat(),
        "utc_offset_minutes": utc_offset_minutes,
        "weeks_ago": weeks_ago,
        "generated_at": now.isoformat(),
        "stats": {
            "pulses": pulse_count,
            "posts": post_count,
            "room_items": room_item_count,
            "rooms": room_count,
            "people": people_count,
            "nights": len(night_keys),
            "highlights": pulse_count + post_count + room_item_count,
        },
        "highlights": highlights,
        "people": people,
        "rooms": rooms,
    }
