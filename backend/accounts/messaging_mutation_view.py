from .messaging_views import MessageDetailView


class MessageMutationView(MessageDetailView):
    """Keep PATCH semantics while allowing Android's HttpURLConnection to POST edits safely."""

    def post(self, request, message_id):
        return self.patch(request, message_id)
