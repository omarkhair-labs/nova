from django.db.models import Count
from rest_framework import serializers

from .models import Conversation, Message
from .serializers import PostAuthorSerializer


class MessageSerializer(serializers.ModelSerializer):
    sender = PostAuthorSerializer(read_only=True)
    image_url = serializers.SerializerMethodField()
    reply_to = serializers.SerializerMethodField()
    reactions = serializers.SerializerMethodField()
    is_mine = serializers.SerializerMethodField()

    class Meta:
        model = Message
        fields = (
            "id",
            "client_id",
            "sender",
            "body",
            "image_url",
            "reply_to",
            "reactions",
            "created_at",
            "delivered_at",
            "read_at",
            "is_mine",
        )
        read_only_fields = fields

    def get_image_url(self, obj):
        if not obj.image:
            return ""
        try:
            return obj.image.url
        except Exception:
            return ""

    def get_reply_to(self, obj):
        reply = obj.reply_to
        if reply is None:
            return None
        return {
            "id": reply.pk,
            "sender": PostAuthorSerializer(reply.sender, context=self.context).data,
            "body": reply.body,
            "image_url": self.get_image_url(reply),
        }

    def get_reactions(self, obj):
        request = self.context.get("request")
        current_user_id = (
            request.user.pk
            if request and request.user.is_authenticated
            else None
        )
        rows = list(
            obj.reactions.values("emoji")
            .annotate(count=Count("id"))
            .order_by("created_at", "emoji")
        )
        mine = set()
        if current_user_id:
            mine = set(
                obj.reactions.filter(user_id=current_user_id)
                .values_list("emoji", flat=True)
            )
        return [
            {
                "emoji": row["emoji"],
                "count": row["count"],
                "reacted_by_me": row["emoji"] in mine,
            }
            for row in rows
        ]

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
        return MessageSerializer(message, context=self.context).data

    def get_unread_count(self, obj):
        annotated = getattr(obj, "unread_count_value", None)
        if annotated is not None:
            return annotated

        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return 0
        return obj.messages.filter(recipient=request.user, read_at__isnull=True).count()
