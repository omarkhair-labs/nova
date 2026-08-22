from django.db.models import Count, Max, OuterRef, Q, Subquery
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ..messaging.group_management import serialize_managed_group_detail
from ..messaging.group_messaging import group_conversation_for, group_membership
from ..messaging.messaging_models import GroupMembership, group_avatar_url
from ..messaging.messaging_serializers import ConversationSerializer
from ..models import Conversation
from ..room_models import RoomItem, RoomProfile
from ..tonight.window import parse_utc_offset, tonight_window
from ..trust_safety import blocked_user_ids
from .serializers import RoomItemCreateSerializer, RoomItemSerializer


ROOM_LIST_LIMIT = 100
ROOM_ITEM_DEFAULT_LIMIT = 30
ROOM_ITEM_MAX_LIMIT = 50
ROOM_TONIGHT_MAX_ROWS = 50


def _room_profile_for(conversation):
    try:
        return conversation.room_profile
    except RoomProfile.DoesNotExist:
        return None


def _room_summary(request, conversation):
    conversation_data = dict(
        ConversationSerializer(conversation, context={"request": request}).data
    )
    conversation_data["group_avatar_url"] = group_avatar_url(request, conversation)
    profile = _room_profile_for(conversation)
    return {
        "conversation": conversation_data,
        "description": profile.description if profile else "",
    }


def _visible_room_items(request, conversation):
    hidden_ids = blocked_user_ids(request.user)
    queryset = conversation.room_items.select_related("created_by")
    if hidden_ids:
        queryset = queryset.exclude(created_by_id__in=hidden_ids)
    return queryset


def _section_counts(request, conversation):
    queryset = _visible_room_items(request, conversation)
    counts = {kind: 0 for kind, _ in RoomItem.Kind.choices}
    for row in queryset.values("kind"):
        counts[row["kind"]] = counts.get(row["kind"], 0) + 1
    return {
        "all": sum(counts.values()),
        **counts,
    }


def _parse_limit(raw):
    if raw in (None, ""):
        return ROOM_ITEM_DEFAULT_LIMIT
    try:
        value = int(raw)
    except (TypeError, ValueError):
        return None
    if value < 1 or value > ROOM_ITEM_MAX_LIMIT:
        return None
    return value


def _parse_before(raw):
    if raw in (None, ""):
        return 0
    try:
        value = int(raw)
    except (TypeError, ValueError):
        return None
    return value if value > 0 else None


def _parse_boolean(raw):
    if isinstance(raw, bool):
        return raw
    value = str(raw or "").strip().lower()
    if value in {"1", "true", "yes", "on"}:
        return True
    if value in {"0", "false", "no", "off"}:
        return False
    return None


class RoomListView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        rooms = list(
            Conversation.objects.filter(
                kind=Conversation.Kind.GROUP,
                group_memberships__user=request.user,
            )
            .select_related("group_profile", "room_profile")
            .distinct()
            .order_by("-updated_at", "-id")[:ROOM_LIST_LIMIT]
        )
        return Response({"rooms": [_room_summary(request, room) for room in rooms]})


class RoomTonightView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        utc_offset_minutes = parse_utc_offset(
            request.query_params.get("utc_offset_minutes", "0")
        )
        if utc_offset_minutes is None:
            return Response(
                {"detail": "Invalid UTC offset for Room Tonight."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        window = tonight_window(timezone.now(), utc_offset_minutes)
        base_payload = {
            "is_tonight": window["is_tonight"],
            "local_hour": window["local_hour"],
            "utc_offset_minutes": utc_offset_minutes,
            "starts_at": window["starts_at"].isoformat(),
            "ends_at": window["ends_at"].isoformat(),
        }
        if not window["is_tonight"]:
            return Response(
                {
                    **base_payload,
                    "rooms_count": 0,
                    "moments_count": 0,
                    "rooms": [],
                }
            )

        hidden_ids = blocked_user_ids(request.user)
        visible_items = RoomItem.objects.filter(
            conversation__kind=Conversation.Kind.GROUP,
            conversation__group_memberships__user=request.user,
            created_at__gte=window["starts_at"],
            created_at__lt=window["ends_at"],
        ).order_by()
        if hidden_ids:
            visible_items = visible_items.exclude(created_by_id__in=hidden_ids)

        totals = visible_items.aggregate(
            rooms_count=Count("conversation_id", distinct=True),
            moments_count=Count("id", distinct=True),
        )
        latest_item_id = Subquery(
            visible_items.filter(
                conversation_id=OuterRef("conversation_id")
            )
            .order_by("-created_at", "-id")
            .values("id")[:1]
        )
        activity_rows = list(
            visible_items.values("conversation_id")
            .annotate(
                moments_count=Count("id", distinct=True),
                my_moments_count=Count(
                    "id",
                    filter=Q(created_by_id=request.user.pk),
                    distinct=True,
                ),
                latest_at=Max("created_at"),
                latest_item_id=latest_item_id,
            )
            .order_by("-latest_at", "-conversation_id")[:ROOM_TONIGHT_MAX_ROWS]
        )

        conversation_ids = [row["conversation_id"] for row in activity_rows]
        latest_item_ids = [
            row["latest_item_id"]
            for row in activity_rows
            if row["latest_item_id"] is not None
        ]
        conversations = {
            conversation.pk: conversation
            for conversation in Conversation.objects.filter(pk__in=conversation_ids)
            .select_related("group_profile", "room_profile")
        }
        latest_items = {
            item.pk: item
            for item in RoomItem.objects.filter(pk__in=latest_item_ids).select_related(
                "created_by"
            )
        }

        context = {"request": request}
        rooms = []
        for row in activity_rows:
            conversation = conversations.get(row["conversation_id"])
            latest_item = latest_items.get(row["latest_item_id"])
            if conversation is None or latest_item is None:
                continue
            rooms.append(
                {
                    **_room_summary(request, conversation),
                    "moments_count": row["moments_count"],
                    "my_moments_count": row["my_moments_count"],
                    "latest_item": RoomItemSerializer(
                        latest_item,
                        context=context,
                    ).data,
                }
            )

        return Response(
            {
                **base_payload,
                "rooms_count": totals["rooms_count"] or 0,
                "moments_count": totals["moments_count"] or 0,
                "rooms": rooms,
            }
        )


class RoomDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        group_detail = serialize_managed_group_detail(request, conversation)
        profile = _room_profile_for(conversation)
        return Response(
            {
                **group_detail,
                "room": {
                    "description": profile.description if profile else "",
                    "sections": _section_counts(request, conversation),
                },
            }
        )

    def patch(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        membership = group_membership(conversation, request.user)
        if membership is None or membership.role not in {
            GroupMembership.Role.OWNER,
            GroupMembership.Role.ADMIN,
        }:
            return Response(
                {"detail": "Only Room admins can edit the Room."},
                status=status.HTTP_403_FORBIDDEN,
            )

        if "description" not in request.data:
            return Response(
                {"detail": "Nothing to update."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        description = str(request.data.get("description") or "").strip()
        if len(description) > 240:
            return Response(
                {"detail": "Room description must be 240 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        profile, _ = RoomProfile.objects.get_or_create(conversation=conversation)
        if profile.description != description:
            profile.description = description
            profile.save(update_fields=("description", "updated_at"))

        return self.get(request, conversation_id)


class RoomItemsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        kind = str(request.query_params.get("kind") or "").strip().lower()
        if kind and kind not in RoomItem.Kind.values:
            return Response(
                {"detail": "Unknown Room section."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        limit = _parse_limit(request.query_params.get("limit"))
        if limit is None:
            return Response(
                {"detail": f"limit must be between 1 and {ROOM_ITEM_MAX_LIMIT}."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        before = _parse_before(request.query_params.get("before"))
        if request.query_params.get("before") not in (None, "") and before is None:
            return Response(
                {"detail": "before must be a positive Room item id."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        queryset = _visible_room_items(request, conversation)
        if kind:
            queryset = queryset.filter(kind=kind)

        pinned = list(queryset.filter(pinned=True).order_by("-created_at", "-id")[:20])
        timeline = queryset.filter(pinned=False)
        if before:
            timeline = timeline.filter(id__lt=before)
        page = list(timeline.order_by("-id")[: limit + 1])
        has_more = len(page) > limit
        page = page[:limit]
        next_before = page[-1].id if has_more and page else None
        context = {"request": request}
        return Response(
            {
                "pinned": RoomItemSerializer(pinned, many=True, context=context).data,
                "items": RoomItemSerializer(page, many=True, context=context).data,
                "next_before": next_before,
            }
        )

    def post(self, request, conversation_id):
        conversation = group_conversation_for(request.user, conversation_id)
        serializer = RoomItemCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        item = RoomItem.objects.create(
            conversation=conversation,
            created_by=request.user,
            **serializer.validated_data,
        )
        return Response(
            RoomItemSerializer(item, context={"request": request}).data,
            status=status.HTTP_201_CREATED,
        )


class RoomItemDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def _item(self, request, conversation_id, item_id):
        conversation = group_conversation_for(request.user, conversation_id)
        item = RoomItem.objects.filter(
            pk=item_id,
            conversation=conversation,
        ).select_related("created_by").first()
        return conversation, item

    def patch(self, request, conversation_id, item_id):
        conversation, item = self._item(request, conversation_id, item_id)
        if item is None:
            return Response(status=status.HTTP_404_NOT_FOUND)
        membership = group_membership(conversation, request.user)
        if membership is None or membership.role not in {
            GroupMembership.Role.OWNER,
            GroupMembership.Role.ADMIN,
        }:
            return Response(
                {"detail": "Only Room admins can pin Room items."},
                status=status.HTTP_403_FORBIDDEN,
            )
        if "pinned" not in request.data:
            return Response(
                {"detail": "Nothing to update."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        pinned = _parse_boolean(request.data.get("pinned"))
        if pinned is None:
            return Response(
                {"detail": "pinned must be true or false."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if item.pinned != pinned:
            item.pinned = pinned
            item.save(update_fields=("pinned", "updated_at"))
        return Response(RoomItemSerializer(item, context={"request": request}).data)

    def delete(self, request, conversation_id, item_id):
        conversation, item = self._item(request, conversation_id, item_id)
        if item is None:
            return Response(status=status.HTTP_404_NOT_FOUND)
        membership = group_membership(conversation, request.user)
        can_moderate = membership is not None and membership.role in {
            GroupMembership.Role.OWNER,
            GroupMembership.Role.ADMIN,
        }
        if item.created_by_id != request.user.pk and not can_moderate:
            return Response(
                {"detail": "You can only remove your own Room items."},
                status=status.HTTP_403_FORBIDDEN,
            )
        item.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
