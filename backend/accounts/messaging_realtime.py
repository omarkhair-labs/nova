from asgiref.sync import async_to_sync
from channels.layers import get_channel_layer

from .messaging_serializers import MessageSerializer
from .realtime import conversation_group_name


def broadcast_message_created(message):
    channel_layer = get_channel_layer()
    if channel_layer is None:
        return

    payload = dict(MessageSerializer(message).data)
    payload.pop("is_mine", None)
    async_to_sync(channel_layer.group_send)(
        conversation_group_name(message.conversation_id),
        {
            "type": "message.created",
            "message": payload,
        },
    )


def broadcast_message_reaction(*, conversation_id, message_id, user_id, emoji, active, count):
    channel_layer = get_channel_layer()
    if channel_layer is None:
        return

    async_to_sync(channel_layer.group_send)(
        conversation_group_name(conversation_id),
        {
            "type": "message.reaction",
            "message_id": message_id,
            "user_id": user_id,
            "emoji": emoji,
            "active": bool(active),
            "count": max(int(count), 0),
        },
    )


def broadcast_messages_delivered(*, conversation_id, recipient_id, delivered_at, message_ids):
    channel_layer = get_channel_layer()
    if channel_layer is None or not message_ids:
        return

    async_to_sync(channel_layer.group_send)(
        conversation_group_name(conversation_id),
        {
            "type": "messages.delivered",
            "recipient_id": recipient_id,
            "delivered_at": delivered_at.isoformat(),
            "message_ids": list(message_ids),
        },
    )


def broadcast_conversation_read(*, conversation_id, reader_id, read_at, message_ids):
    channel_layer = get_channel_layer()
    if channel_layer is None:
        return

    async_to_sync(channel_layer.group_send)(
        conversation_group_name(conversation_id),
        {
            "type": "conversation.read",
            "reader_id": reader_id,
            "read_at": read_at.isoformat(),
            "message_ids": list(message_ids),
        },
    )
