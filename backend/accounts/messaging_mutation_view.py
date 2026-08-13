from rest_framework import status
from rest_framework.response import Response

from .messaging_views import MessageDetailView, message_for_request
from .sharing_models import MessageShare


CALL_HISTORY_CLIENT_PREFIX = "call:"


class MessageMutationView(MessageDetailView):
    """Keep PATCH semantics while allowing Android's HttpURLConnection to POST edits safely."""

    def patch(self, request, message_id):
        message = message_for_request(request, message_id)
        if message.sender_id == request.user.pk and message.client_id.startswith(CALL_HISTORY_CLIENT_PREFIX):
            return Response(
                {"detail": "Call history can't be edited."},
                status=status.HTTP_409_CONFLICT,
            )
        if message.sender_id == request.user.pk and MessageShare.objects.filter(message=message).exists():
            return Response(
                {"detail": "Shared posts and profiles can't be edited."},
                status=status.HTTP_409_CONFLICT,
            )
        return super().patch(request, message_id)

    def post(self, request, message_id):
        return self.patch(request, message_id)
