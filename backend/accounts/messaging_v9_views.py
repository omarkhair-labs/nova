from django.db.models import Q
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .messaging_models import ConversationPreference
from .messaging_serializers import MessageSerializer
from .messaging_views import conversation_for_request


SEARCH_LIMIT = 50
MEDIA_PAGE_SIZE = 30
CONTEXT_SIDE_SIZE = 20


def _message_queryset(conversation):
    return conversation.messages.select_related(
        "sender",
        "recipient",
        "reply_to",
        "reply_to__sender",
    ).prefetch_related("reactions")


class ConversationMessageSearchView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        conversation = conversation_for_request(request, conversation_id)
        query = request.query_params.get("q", "").strip()
        if not query:
            return Response({"results": [], "query": ""})
        if len(query) > 200:
            return Response(
                {"detail": "Search query must be 200 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        messages = (
            _message_queryset(conversation)
            .filter(deleted_at__isnull=True, body__icontains=query)
            .order_by("-id")[:SEARCH_LIMIT]
        )
        return Response(
            {
                "results": MessageSerializer(
                    messages,
                    many=True,
                    context={"request": request},
                ).data,
                "query": query,
            }
        )


class ConversationMessageContextView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        conversation = conversation_for_request(request, conversation_id)
        raw_message_id = request.query_params.get("message_id", "").strip()
        try:
            message_id = int(raw_message_id)
        except (TypeError, ValueError):
            return Response(
                {"detail": "A valid message_id is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        base = _message_queryset(conversation)
        target = base.filter(pk=message_id).first()
        if target is None:
            return Response(
                {"detail": "That message is no longer available in this conversation."},
                status=status.HTTP_404_NOT_FOUND,
            )

        earlier_desc = list(base.filter(id__lt=message_id).order_by("-id")[:CONTEXT_SIDE_SIZE])
        later = list(base.filter(id__gt=message_id).order_by("id")[:CONTEXT_SIDE_SIZE])
        items = list(reversed(earlier_desc)) + [target] + later

        return Response(
            {
                "results": MessageSerializer(
                    items,
                    many=True,
                    context={"request": request},
                ).data,
                "target_message_id": target.pk,
                "has_earlier": base.filter(id__lt=items[0].pk).exists() if items else False,
                "has_later": base.filter(id__gt=items[-1].pk).exists() if items else False,
            }
        )


class ConversationMediaView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        conversation = conversation_for_request(request, conversation_id)
        media_type = request.query_params.get("type", "all").strip().lower()
        if media_type not in {"all", "image", "audio"}:
            return Response(
                {"detail": "Media type must be all, image, or audio."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        messages = _message_queryset(conversation).filter(deleted_at__isnull=True)
        if media_type == "image":
            messages = messages.exclude(image="")
        elif media_type == "audio":
            messages = messages.exclude(audio="")
        else:
            messages = messages.filter(~Q(image="") | ~Q(audio=""))

        cursor = request.query_params.get("cursor", "").strip()
        if cursor:
            try:
                cursor_id = int(cursor)
            except ValueError:
                return Response(
                    {"detail": "Invalid media cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            messages = messages.filter(id__lt=cursor_id)

        page_with_extra = list(messages.order_by("-id")[: MEDIA_PAGE_SIZE + 1])
        has_more = len(page_with_extra) > MEDIA_PAGE_SIZE
        page = page_with_extra[:MEDIA_PAGE_SIZE]
        next_cursor = str(page[-1].pk) if has_more and page else None

        return Response(
            {
                "results": MessageSerializer(
                    page,
                    many=True,
                    context={"request": request},
                ).data,
                "next_cursor": next_cursor,
                "type": media_type,
            }
        )


class ConversationPreferenceView(APIView):
    permission_classes = [IsAuthenticated]

    def _preference(self, request, conversation_id):
        conversation = conversation_for_request(request, conversation_id)
        preference, _ = ConversationPreference.objects.get_or_create(
            conversation=conversation,
            user=request.user,
        )
        return conversation, preference

    def get(self, request, conversation_id):
        conversation, preference = self._preference(request, conversation_id)
        return Response(
            {
                "conversation_id": conversation.pk,
                "muted": preference.muted,
            }
        )

    def post(self, request, conversation_id):
        conversation, preference = self._preference(request, conversation_id)
        muted = request.data.get("muted")
        if not isinstance(muted, bool):
            return Response(
                {"detail": "muted must be true or false."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if preference.muted != muted:
            preference.muted = muted
            preference.save(update_fields=("muted", "updated_at"))

        return Response(
            {
                "conversation_id": conversation.pk,
                "muted": preference.muted,
            }
        )
