from rest_framework import serializers

from .models import Conversation, Message
from .serializers import PostAuthorSerializer


class MessageSerializer(serializers.ModelSerializer):
    sender = PostAuthorSerializer(read_only=True)
    body = serializers.SerializerMethodField()
    image_url = serializers.SerializerMethodField()
    audio_url = serializers.SerializerMethodField()
    audio_duration_ms = serializers.SerializerMethodField()
    reply_to = serializers.SerializerMethodField()
    reactions = serializers.SerializerMethodField()
    is_mine = serializers.SerializerMethodField()
    is_deleted = serializers.SerializerMethodField()

    class Meta:
        model = Message
        fields = (
            "id",
            "client_id",
            "sender",
            "body",
            "image_url",
            "audio_url",
            "audio_duration_ms",
            "reply_to",
            "reactions",
            "created_at",
            "delivered_at",
            "read_at",
            "edited_at",
            "deleted_at",
            "is_deleted",
            "is_mine",
        )
        read_only_fields = fields

    def get_body(self, obj):
        return "" if obj.deleted_at else obj.body

    def get_image_url(self, obj):
        if obj.deleted_at or not obj.image:
            return ""
        try:
            return obj.image.url
        except Exception:
            return ""

    def get_audio_url(self, obj):
        if obj.deleted_at or not obj.audio:
            return ""
        try:
            return obj.audio.url
        except Exception:
            return ""

    def get_audio_duration_ms(self, obj):
        return None if obj.deleted_at else obj.audio_duration_ms

    def get_reply_to(self, obj):
        if obj.deleted_at:
            return None

        reply = obj.reply_to
        if reply is None:
            return None

        deleted = reply.deleted_at is not None
        return {
            "id": reply.pk,
            "sender": PostAuthorSerializer(reply.sender, context=self.context).data,
            "body": "Message deleted" if deleted else reply.body,
            "image_url": "" if deleted else self.get_image_url(reply),
            "audio_url": "" if deleted else self.get_audio_url(reply),
            "audio_duration_ms": None if deleted else reply.audio_duration_ms,
            "is_deleted": deleted,
        }

    def get_reactions(self, obj):
        if obj.deleted_at or self.context.get("skip_reactions"):
            return []

        request = self.context.get("request")
        current_user_id = (
            request.user.pk
            if request and request.user.is_authenticated
            else None
        )

        counts = {}
        mine = set()
        for reaction in obj.reactions.all():
            counts[reaction.emoji] = counts.get(reaction.emoji, 0) + 1
            if current_user_id and reaction.user_id == current_user_id:
                mine.add(reaction.emoji)

        return [
            {
                "emoji": emoji,
                "count": count,
                "reacted_by_me": emoji in mine,
            }
            for emoji, count in sorted(counts.items())
        ]

    def get_is_deleted(self, obj):
        return obj.deleted_at is not None

    def get_is_mine(self, obj):
        request = self.context.get("request")
        return bool(request and request.user.is_authenticated and obj.sender_id == request.user.id)


class ConversationSerializer(serializers.ModelSerializer):
    other_user = serializers.SerializerMethodField()
    last_message = serializers.SerializerMethodField()
    unread_count = serializers.SerializerMethodField()

    class Meta:
        model = Conversation
        fields = (
            "id",
            "other_user",
            "last_message",
            "unread_count",
            "created_at",
            "updated_at",
        )
        read_only_fields = fields

    def get_other_user(self, obj):
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return None
        other = obj.participant_two if obj.participant_one_id == request.user.id else obj.participant_one
        return PostAuthorSerializer(other, context=self.context).data

    def get_last_message(self, obj):
        message = (
            obj.messages.select_related("sender", "reply_to", "reply_to__sender")
            .order_by("-id")
            .first()
        )
        if message is None:
            return None

        context = dict(self.context)
        context["skip_reactions"] = True
        data = dict(MessageSerializer(message, context=context).data)
        if data.get("is_deleted"):
            data["body"] = "Message deleted"
        elif not data.get("body") and data.get("audio_url"):
            data["body"] = "🎤 Voice message"
        elif not data.get("body") and data.get("image_url"):
            data["body"] = "📷 Photo"
        return data

    def get_unread_count(self, obj):
        annotated = getattr(obj, "unread_count_value", None)
        if annotated is not None:
            return annotated

        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return 0
        return obj.messages.filter(recipient=request.user, read_at__isnull=True).count()
