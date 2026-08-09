import logging

from django.contrib.auth import get_user_model
from django.db import IntegrityError, transaction
from django.db.models import Count, Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .messaging_realtime import (
    broadcast_conversation_read,
    broadcast_message_created,
    broadcast_message_deleted,
    broadcast_message_reaction,
    broadcast_message_updated,
    broadcast_messages_delivered,
)
from .messaging_serializers import ConversationSerializer, MessageSerializer
from .models import Conversation, Message, MessageReaction
from .push import send_message_push

logger = logging.getLogger(__name__)
User = get_user_model()
MESSAGE_PAGE_SIZE = 50
MAX_MESSAGE_IMAGE_BYTES = 10 * 1024 * 1024
MAX_MESSAGE_AUDIO_BYTES = 15 * 1024 * 1024
MAX_VOICE_DURATION_MS = 5 * 60 * 1000
ALLOWED_REACTIONS = {"❤️", "😂", "😮", "😢", "😡", "👍"}


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


def message_for_request(request, message_id):
    return get_object_or_404(
        Message.objects.select_related(
            "conversation",
            "sender",
            "recipient",
            "reply_to",
            "reply_to__sender",
        ).filter(
            Q(conversation__participant_one=request.user)
            | Q(conversation__participant_two=request.user)
        ),
        pk=message_id,
    )


def reaction_payload(message, request):
    return MessageSerializer(message, context={"request": request}).data["reactions"]


def broadcast_reaction_state(message, user_id, emoji, active):
    broadcast_message_reaction(
        conversation_id=message.conversation_id,
        message_id=message.pk,
        user_id=user_id,
        emoji=emoji,
        active=active,
        count=message.reactions.filter(emoji=emoji).count(),
    )


def schedule_stored_file_delete(field):
    name = getattr(field, "name", "")
    if not name:
        return
    storage = field.storage

    def delete_file():
        try:
            storage.delete(name)
        except Exception:
            logger.exception("Nova could not delete message attachment %s", name)

    transaction.on_commit(delete_file)


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
        username = str(request.data.get("username") or "").strip().lower()
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
        messages = conversation.messages.select_related(
            "sender",
            "recipient",
            "reply_to",
            "reply_to__sender",
        ).prefetch_related("reactions").order_by("-id")

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

        delivery_ids = [
            message.pk
            for message in page
            if message.recipient_id == request.user.pk and message.delivered_at is None
        ]
        if delivery_ids:
            delivered_at = timezone.now()
            Message.objects.filter(
                pk__in=delivery_ids,
                recipient=request.user,
                delivered_at__isnull=True,
            ).update(delivered_at=delivered_at)

            delivery_state = dict(
                Message.objects.filter(pk__in=delivery_ids).values_list("id", "delivered_at")
            )
            changed_ids = [
                message_id
                for message_id, value in delivery_state.items()
                if value == delivered_at
            ]
            delivery_id_set = set(delivery_ids)
            for message in page:
                if message.pk in delivery_id_set:
                    message.delivered_at = delivery_state.get(message.pk)

            if changed_ids:
                broadcast_messages_delivered(
                    conversation_id=conversation.pk,
                    recipient_id=request.user.pk,
                    delivered_at=delivered_at,
                    message_ids=changed_ids,
                )

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
        body = str(request.data.get("body") or "").strip()
        client_id = str(request.data.get("client_id") or "").strip()
        image = request.FILES.get("image")
        audio = request.FILES.get("audio")
        raw_audio_duration = request.data.get("audio_duration_ms")
        raw_reply_to = request.data.get("reply_to_id")

        if not body and image is None and audio is None:
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
        if image is not None and audio is not None:
            return Response(
                {"detail": "Send either a photo or a voice message, not both at once."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if image is not None:
            content_type = str(getattr(image, "content_type", "") or "")
            if not content_type.startswith("image/"):
                return Response(
                    {"detail": "Message attachment must be an image."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if image.size > MAX_MESSAGE_IMAGE_BYTES:
                return Response(
                    {"detail": "Message image must be 10 MB or smaller."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        audio_duration_ms = None
        if audio is not None:
            content_type = str(getattr(audio, "content_type", "") or "")
            if not content_type.startswith("audio/"):
                return Response(
                    {"detail": "Voice attachment must be an audio file."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if audio.size > MAX_MESSAGE_AUDIO_BYTES:
                return Response(
                    {"detail": "Voice message must be 15 MB or smaller."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            try:
                audio_duration_ms = int(raw_audio_duration)
            except (TypeError, ValueError):
                return Response(
                    {"detail": "Voice message duration is required."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if audio_duration_ms <= 0 or audio_duration_ms > MAX_VOICE_DURATION_MS:
                return Response(
                    {"detail": "Voice message must be between 1 second and 5 minutes."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        reply_to = None
        if raw_reply_to not in (None, ""):
            try:
                reply_to_id = int(raw_reply_to)
            except (TypeError, ValueError):
                return Response(
                    {"detail": "Invalid reply_to_id."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            reply_to = conversation.messages.select_related("sender").filter(
                pk=reply_to_id,
                deleted_at__isnull=True,
            ).first()
            if reply_to is None:
                return Response(
                    {"detail": "You can only reply to an available message in this conversation."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        existing = Message.objects.filter(sender=request.user, client_id=client_id).first()
        if existing is not None:
            if existing.conversation_id != conversation.pk:
                return Response(
                    {"detail": "That client_id was already used in another conversation."},
                    status=status.HTTP_409_CONFLICT,
                )
            return Response(
                MessageSerializer(existing, context={"request": request}).data,
                status=status.HTTP_200_OK,
            )

        recipient = other_participant(conversation, request.user)

        try:
            with transaction.atomic():
                message = Message.objects.create(
                    conversation=conversation,
                    sender=request.user,
                    recipient=recipient,
                    reply_to=reply_to,
                    image=image or "",
                    audio=audio or "",
                    audio_duration_ms=audio_duration_ms,
                    body=body,
                    client_id=client_id,
                )
                created = True
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
            message = Message.objects.select_related("sender", "reply_to", "reply_to__sender").get(pk=message.pk)
            broadcast_message_created(message)
            send_message_push(message)

        return Response(
            MessageSerializer(message, context={"request": request}).data,
            status=status.HTTP_201_CREATED if created else status.HTTP_200_OK,
        )


class MessageDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request, message_id):
        message = message_for_request(request, message_id)
        if message.sender_id != request.user.pk:
            return Response(
                {"detail": "Only the sender can edit this message."},
                status=status.HTTP_403_FORBIDDEN,
            )
        if message.deleted_at is not None:
            return Response(
                {"detail": "A deleted message can't be edited."},
                status=status.HTTP_409_CONFLICT,
            )

        body = str(request.data.get("body") or "").strip()
        if len(body) > 2000:
            return Response(
                {"detail": "Message must be 2000 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if not body and not message.image and not message.audio:
            return Response(
                {"detail": "Message can't be empty."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if body == message.body:
            return Response(MessageSerializer(message, context={"request": request}).data)

        edited_at = timezone.now()
        message.body = body
        message.edited_at = edited_at
        message.save(update_fields=("body", "edited_at"))
        broadcast_message_updated(
            conversation_id=message.conversation_id,
            message_id=message.pk,
            body=message.body,
            edited_at=edited_at,
        )
        return Response(MessageSerializer(message, context={"request": request}).data)

    def delete(self, request, message_id):
        message = message_for_request(request, message_id)
        if message.sender_id != request.user.pk:
            return Response(
                {"detail": "Only the sender can delete this message for everyone."},
                status=status.HTTP_403_FORBIDDEN,
            )
        if message.deleted_at is not None:
            return Response(
                {
                    "message_id": message.pk,
                    "deleted_at": message.deleted_at.isoformat(),
                }
            )

        deleted_at = timezone.now()
        with transaction.atomic():
            schedule_stored_file_delete(message.image)
            schedule_stored_file_delete(message.audio)
            MessageReaction.objects.filter(message=message).delete()
            message.body = ""
            message.image = ""
            message.audio = ""
            message.audio_duration_ms = None
            message.reply_to = None
            message.edited_at = None
            message.deleted_at = deleted_at
            message.save(
                update_fields=(
                    "body",
                    "image",
                    "audio",
                    "audio_duration_ms",
                    "reply_to",
                    "edited_at",
                    "deleted_at",
                )
            )

        broadcast_message_deleted(
            conversation_id=message.conversation_id,
            message_id=message.pk,
            deleted_at=deleted_at,
        )
        return Response(
            {
                "message_id": message.pk,
                "deleted_at": deleted_at.isoformat(),
            }
        )


class MessageReactionView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, message_id):
        message = message_for_request(request, message_id)
        if message.deleted_at is not None:
            return Response(
                {"detail": "A deleted message can't be reacted to."},
                status=status.HTTP_409_CONFLICT,
            )

        emoji = str(request.data.get("emoji") or "").strip()
        if emoji not in ALLOWED_REACTIONS:
            return Response(
                {"detail": "Unsupported reaction."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        previous = MessageReaction.objects.filter(
            message=message,
            user=request.user,
        ).first()
        previous_emoji = previous.emoji if previous else None

        reaction, _ = MessageReaction.objects.update_or_create(
            message=message,
            user=request.user,
            defaults={"emoji": emoji},
        )

        if previous_emoji and previous_emoji != reaction.emoji:
            broadcast_reaction_state(
                message,
                request.user.pk,
                previous_emoji,
                False,
            )
        broadcast_reaction_state(
            message,
            request.user.pk,
            reaction.emoji,
            True,
        )

        return Response({"reactions": reaction_payload(message, request)})

    def delete(self, request, message_id):
        message = message_for_request(request, message_id)
        if message.deleted_at is not None:
            return Response(
                {"detail": "A deleted message can't be reacted to."},
                status=status.HTTP_409_CONFLICT,
            )

        reaction = MessageReaction.objects.filter(
            message=message,
            user=request.user,
        ).first()
        if reaction is None:
            return Response({"reactions": reaction_payload(message, request)})

        emoji = reaction.emoji
        reaction.delete()
        broadcast_reaction_state(
            message,
            request.user.pk,
            emoji,
            False,
        )
        return Response({"reactions": reaction_payload(message, request)})


class ConversationReadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, conversation_id):
        conversation = conversation_for_request(request, conversation_id)
        unread = conversation.messages.filter(
            recipient=request.user,
            read_at__isnull=True,
        )
        message_ids = list(unread.values_list("id", flat=True))
        if not message_ids:
            return Response(
                {
                    "marked_read": 0,
                    "unread_count": 0,
                    "read_at": None,
                }
            )

        read_at = timezone.now()
        delivery_ids = list(
            unread.filter(delivered_at__isnull=True).values_list("id", flat=True)
        )
        if delivery_ids:
            Message.objects.filter(
                pk__in=delivery_ids,
                delivered_at__isnull=True,
            ).update(delivered_at=read_at)
            changed_delivery_ids = list(
                Message.objects.filter(
                    pk__in=delivery_ids,
                    delivered_at=read_at,
                ).values_list("id", flat=True)
            )
            if changed_delivery_ids:
                broadcast_messages_delivered(
                    conversation_id=conversation.pk,
                    recipient_id=request.user.pk,
                    delivered_at=read_at,
                    message_ids=changed_delivery_ids,
                )

        marked_read = unread.update(read_at=read_at)
        if marked_read:
            broadcast_conversation_read(
                conversation_id=conversation.pk,
                reader_id=request.user.pk,
                read_at=read_at,
                message_ids=message_ids,
            )

        return Response(
            {
                "marked_read": marked_read,
                "unread_count": 0,
                "read_at": read_at.isoformat(),
            }
        )
