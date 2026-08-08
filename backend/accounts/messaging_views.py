from django.contrib.auth import get_user_model
from django.db import IntegrityError, transaction
from django.db.models import Count, Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .messaging_serializers import ConversationSerializer, MessageSerializer
from .models import Conversation, Message

User = get_user_model()
MESSAGE_PAGE_SIZE = 50


def conversations_for(user):
    return Conversation.objects.filter(
        Q(participant_one=user) | Q(participant_two=user)
    ).select_related("participant_one", "participant_two")


def conversation_for_request(request, conversation_id):
    return get_object_or_404(conversations_for(request.user), pk=conversation_id)


def other_participant(conversation, user):
    if conversation.participant_one_id == user.id:
        return conversation.participant_two
    return conversation.participant_one


class ConversationsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        query = request.query_params.get("q", "").strip()
        conversations = conversations_for(request.user)

        if query:
            conversations = conversations.filter(
                Q(
                    participant_one=request.user,
                    participant_two__username__icontains=query,
                )
                | Q(
                    participant_one=request.user,
                    participant_two__name__icontains=query,
                )
                | Q(
                    participant_two=request.user,
                    participant_one__username__icontains=query,
                )
                | Q(
                    participant_two=request.user,
                    participant_one__name__icontains=query,
                )
            )

        conversations = conversations.annotate(
            unread_count_value=Count(
                "messages",
                filter=Q(messages__recipient=request.user, messages__read_at__isnull=True),
            )
        ).order_by("-updated_at", "-id")[:50]

        items = list(conversations)
        total_unread = Message.objects.filter(
            recipient=request.user,
            read_at__isnull=True,
        ).count()
        return Response(
            {
                "results": ConversationSerializer(
                    items,
                    many=True,
                    context={"request": request},
                ).data,
                "unread_count": total_unread,
            }
        )

    def post(self, request):
        username = str(request.data.get("username", "")).strip().lower()
        if not username:
            return Response(
                {"detail": "Username is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        target = get_object_or_404(User.objects.filter(is_active=True), username=username)
        if target.pk == request.user.pk:
            return Response(
                {"detail": "You can't message yourself."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        first_id, second_id = sorted((request.user.pk, target.pk))
        try:
            with transaction.atomic():
                conversation, created = Conversation.objects.get_or_create(
                    participant_one_id=first_id,
                    participant_two_id=second_id,
                )
        except IntegrityError:
            conversation = Conversation.objects.get(
                participant_one_id=first_id,
                participant_two_id=second_id,
            )
            created = False

        conversation = conversations_for(request.user).get(pk=conversation.pk)
        return Response(
            ConversationSerializer(conversation, context={"request": request}).data,
            status=status.HTTP_201_CREATED if created else status.HTTP_200_OK,
        )


class ConversationMessagesView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        conversation = conversation_for_request(request, conversation_id)
        messages = conversation.messages.select_related("sender", "recipient").order_by("-id")

        cursor = request.query_params.get("cursor", "").strip()
        if cursor:
            try:
                cursor_id = int(cursor)
            except ValueError:
                return Response(
                    {"detail": "Invalid message cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            messages = messages.filter(id__lt=cursor_id)

        page_with_extra = list(messages[: MESSAGE_PAGE_SIZE + 1])
        has_more = len(page_with_extra) > MESSAGE_PAGE_SIZE
        newest_first = page_with_extra[:MESSAGE_PAGE_SIZE]
        page = list(reversed(newest_first))
        next_cursor = str(newest_first[-1].id) if has_more and newest_first else None

        return Response(
            {
                "results": MessageSerializer(
                    page,
                    many=True,
                    context={"request": request},
                ).data,
                "next_cursor": next_cursor,
            }
        )

    def post(self, request, conversation_id):
        conversation = conversation_for_request(request, conversation_id)
        body = str(request.data.get("body", "")).strip()
        client_id = str(request.data.get("client_id", "")).strip()

        if not body:
            return Response(
                {"detail": "Message can't be empty."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(body) > 2000:
            return Response(
                {"detail": "Message must be 2000 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if not client_id or len(client_id) > 64:
            return Response(
                {"detail": "A valid client_id is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        recipient = other_participant(conversation, request.user)

        try:
            with transaction.atomic():
                message, created = Message.objects.get_or_create(
                    sender=request.user,
                    client_id=client_id,
                    defaults={
                        "conversation": conversation,
                        "recipient": recipient,
                        "body": body,
                    },
                )
        except IntegrityError:
            message = Message.objects.get(sender=request.user, client_id=client_id)
            created = False

        if message.conversation_id != conversation.pk:
            return Response(
                {"detail": "That client_id was already used in another conversation."},
                status=status.HTTP_409_CONFLICT,
            )

        if created:
            Conversation.objects.filter(pk=conversation.pk).update(updated_at=timezone.now())

        return Response(
            MessageSerializer(message, context={"request": request}).data,
            status=status.HTTP_201_CREATED if created else status.HTTP_200_OK,
        )


class ConversationReadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, conversation_id):
        conversation = conversation_for_request(request, conversation_id)
        marked_read = conversation.messages.filter(
            recipient=request.user,
            read_at__isnull=True,
        ).update(read_at=timezone.now())

        return Response(
            {
                "marked_read": marked_read,
                "unread_count": 0,
            }
        )
