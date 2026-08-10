from django.db.models import Q
from django.utils.dateparse import parse_datetime
from rest_framework import status
from rest_framework.response import Response

from .messaging_serializers import ConversationSerializer
from .messaging_views import ConversationsView, conversations_for, total_unread_for
from .models import Conversation

CONVERSATION_PAGE_SIZE = 30


class PaginatedConversationsView(ConversationsView):
    def get(self, request):
        query = request.query_params.get("q", "").strip()
        conversations = conversations_for(request.user)

        if query:
            conversations = conversations.filter(
                Q(
                    kind=Conversation.Kind.DIRECT,
                    participant_one=request.user,
                    participant_two__username__icontains=query,
                )
                | Q(
                    kind=Conversation.Kind.DIRECT,
                    participant_one=request.user,
                    participant_two__name__icontains=query,
                )
                | Q(
                    kind=Conversation.Kind.DIRECT,
                    participant_two=request.user,
                    participant_one__username__icontains=query,
                )
                | Q(
                    kind=Conversation.Kind.DIRECT,
                    participant_two=request.user,
                    participant_one__name__icontains=query,
                )
                | Q(
                    kind=Conversation.Kind.GROUP,
                    title__icontains=query,
                )
            ).distinct()

        raw_cursor = request.query_params.get("cursor", "").strip()
        if raw_cursor:
            try:
                raw_time, raw_id = raw_cursor.rsplit("|", 1)
                cursor_time = parse_datetime(raw_time)
                cursor_id = int(raw_id)
            except (TypeError, ValueError):
                cursor_time = None
                cursor_id = 0
            if cursor_time is None or cursor_id <= 0:
                return Response(
                    {"detail": "Invalid conversation cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            conversations = conversations.filter(
                Q(updated_at__lt=cursor_time)
                | Q(updated_at=cursor_time, id__lt=cursor_id)
            )

        conversations = conversations.order_by("-updated_at", "-id")
        page_with_extra = list(conversations[: CONVERSATION_PAGE_SIZE + 1])
        has_more = len(page_with_extra) > CONVERSATION_PAGE_SIZE
        page = page_with_extra[:CONVERSATION_PAGE_SIZE]
        next_cursor = (
            f"{page[-1].updated_at.isoformat()}|{page[-1].id}"
            if has_more and page
            else None
        )

        return Response(
            {
                "results": ConversationSerializer(
                    page,
                    many=True,
                    context={"request": request},
                ).data,
                "next_cursor": next_cursor,
                "unread_count": total_unread_for(request.user),
            }
        )
