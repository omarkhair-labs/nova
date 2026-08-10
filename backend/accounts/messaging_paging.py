from django.db.models import Q
from django.utils.dateparse import parse_datetime
from rest_framework import status
from rest_framework.response import Response

from .messaging_serializers import ConversationSerializer
from .messaging_views import conversations_for, total_unread_for
from .models import Conversation

CONVERSATION_PAGE_SIZE = 30


class PaginatedConversationsView:
    pass
