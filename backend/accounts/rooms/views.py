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
from ..trust_safety import blocked_user_ids
from .serializers import RoomItemCreateSerializer, RoomItemSerializer


ROOM_LIST_LIMIT = 100
ROOM_ITEM_DEFAULT_LIMIT = 30
ROOM_ITEM_MAX_LIMIT = 50


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
