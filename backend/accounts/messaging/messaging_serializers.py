from rest_framework import serializers

from .messaging_models import GroupMembership, GroupReadState
from .models import Conversation, Message
from .privacy import can_view_user_content
from .serializers import PostAuthorSerializer
from .trust_safety import blocked_user_ids, users_blocked


def _absolute_file_url(request, field):
    if not field:
        return ""
    try:
        url = field.url
    except Exception:
        return ""
    return request.build_absolute_uri(url) if request else url


def _visible_to_context_user(context, target_user):
    if target_user is None or not target_user.is_active:
        return False
    request = context.get("request")
    current_user = getattr(request, "user", None)
    if current_user is None or not getattr(current_user, "is_authenticated", False):
        return True
    return not users_blocked(current_user, target_user)


def _post_visible_to_context_user(context, post):
    if post is None or not post.author.is_active:
        return False
    request = context.get("request")
    current_user = getattr(request, "user", None)
    if current_user is None or not getattr(current_user, "is_authenticated", False):
        return True
    return can_view_user_content(current_user, post.author)


def _reel_visible_to_context_user(context, reel):
    if reel is None or not reel.author.is_active:
        return False
    request = context.get("request")
    current_user = getattr(request, "user", None)
    if current_user is None or not getattr(current_user, "is_authenticated", False):
        return True
    return can_view_user_content(current_user, reel.author)


def _blocked_ids_for_context(context):
    request = context.get("request")
    current_user = getattr(request, "user", None)
    if current_user is None or not getattr(current_user, "is_authenticated", False):
        return set()
    return blocked_user_ids(current_user)


class MessageSerializer(serializers.ModelSerializer):
    sender = PostAuthorSerializer(read_only=True)
    body = serializers.SerializerMethodField()
    image_url = serializers.SerializerMethodField()
    audio_url = serializers.SerializerMethodField()
    audio_duration_ms = serializers.SerializerMethodField()
    reply_to = serializers.SerializerMethodField()
    reactions = serializers.SerializerMethodField()
    share = serializers.SerializerMethodField()
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
            "share",
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
        if reply is None or reply.sender_id in _blocked_ids_for_context(self.context):
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
        hidden_ids = _blocked_ids_for_context(self.context)

        counts = {}
        mine = set()
        for reaction in obj.reactions.all():
            if reaction.user_id in hidden_ids:
                continue
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

    def get_share(self, obj):
        if obj.deleted_at:
            return None
        try:
            shared = obj.shared_content
        except Exception:
            return None

        request = self.context.get("request")
        if shared.kind == "post":
            post = shared.post
            available = _post_visible_to_context_user(self.context, post)
            if not available:
                return {
                    "kind": "post",
                    "available": False,
                    "post": None,
                    "profile": None,
                    "reel": None,
                }
            return {
                "kind": "post",
                "available": True,
                "post": {
                    "id": post.pk,
                    "author": PostAuthorSerializer(post.author, context=self.context).data,
                    "image_url": _absolute_file_url(request, post.image),
                    "media_url": _absolute_file_url(
                        request,
                        post.video if post.media_type == post.MediaType.VIDEO else post.image,
                    ),
                    "media_type": post.media_type,
                    "thumbnail_url": _absolute_file_url(
                        request,
                        post.thumbnail or post.image,
                    ),
                    "caption": post.caption,
                },
                "profile": None,
                "reel": None,
            }

        if shared.kind == "reel":
            reel = shared.reel
            available = _reel_visible_to_context_user(self.context, reel)
            if not available:
                return {
                    "kind": "reel",
                    "available": False,
                    "post": None,
                    "profile": None,
                    "reel": None,
                }
            return {
                "kind": "reel",
                "available": True,
                "post": None,
                "profile": None,
                "reel": {
                    "id": reel.pk,
                    "author": PostAuthorSerializer(reel.author, context=self.context).data,
                    "video_url": _absolute_file_url(request, reel.video),
                    "caption": reel.caption,
                },
            }

        profile = shared.profile
        available = _visible_to_context_user(self.context, profile)
        if not available:
            return {
                "kind": "profile",
                "available": False,
                "post": None,
                "profile": None,
                "reel": None,
            }
        return {
            "kind": "profile",
            "available": True,
            "post": None,
            "profile": PostAuthorSerializer(profile, context=self.context).data,
            "reel": None,
        }

    def get_is_deleted(self, obj):
        return obj.deleted_at is not None

    def get_is_mine(self, obj):
        request = self.context.get("request")
        return bool(request and request.user.is_authenticated and obj.sender_id == request.user.id)


class ConversationSerializer(serializers.ModelSerializer):
    other_user = serializers.SerializerMethodField()
    members_preview = serializers.SerializerMethodField()
    members_count = serializers.SerializerMethodField()
    current_user_role = serializers.SerializerMethodField()
    last_message = serializers.SerializerMethodField()
    unread_count = serializers.SerializerMethodField()

    class Meta:
        model = Conversation
        fields = (
            "id",
            "kind",
            "title",
            "other_user",
            "members_preview",
            "members_count",
            "current_user_role",
            "last_message",
            "unread_count",
            "created_at",
            "updated_at",
        )
        read_only_fields = fields

    def get_other_user(self, obj):
        if obj.kind != Conversation.Kind.DIRECT:
            return None
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return None
        other = obj.participant_two if obj.participant_one_id == request.user.id else obj.participant_one
        if other is None:
            return None
        return PostAuthorSerializer(other, context=self.context).data

    def get_members_preview(self, obj):
        if obj.kind != Conversation.Kind.GROUP:
            return []
        request = self.context.get("request")
        current_user_id = (
            request.user.pk
            if request and request.user.is_authenticated
            else None
        )
        hidden_ids = _blocked_ids_for_context(self.context)
        memberships = (
            obj.group_memberships.select_related("user")
            .filter(user__is_active=True)
            .exclude(user_id=current_user_id)
            .exclude(user_id__in=hidden_ids)
            .order_by("joined_at", "id")[:4]
        )
        return [
            PostAuthorSerializer(item.user, context=self.context).data
            for item in memberships
        ]

    def get_members_count(self, obj):
        if obj.kind != Conversation.Kind.GROUP:
            return 2
        return obj.group_memberships.filter(user__is_active=True).count()

    def get_current_user_role(self, obj):
        if obj.kind != Conversation.Kind.GROUP:
            return ""
        request = self.context.get("request")
        if not request or not request.user.is_authenticated:
            return ""
        membership = GroupMembership.objects.filter(
            conversation=obj,
            user=request.user,
        ).only("role").first()
        return membership.role if membership else ""

    def get_last_message(self, obj):
        messages = obj.messages.select_related(
            "sender",
            "reply_to",
            "reply_to__sender",
            "shared_content",
            "shared_content__post__author",
            "shared_content__profile",
            "shared_content__reel__author",
        )
        if obj.kind == Conversation.Kind.GROUP:
            messages = messages.exclude(sender_id__in=_blocked_ids_for_context(self.context))
        message = messages.order_by("-id").first()
        if message is None:
            return None

        context = dict(self.context)
        context["skip_reactions"] = True
        data = dict(MessageSerializer(message, context=context).data)
        if data.get("is_deleted"):
            data["body"] = "Message deleted"
        elif data.get("share"):
            share = data["share"]
            if share.get("kind") == "post":
                data["body"] = "Shared a post"
            elif share.get("kind") == "reel":
                data["body"] = "Shared a Reel"
            else:
                data["body"] = "Shared a profile"
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
        if obj.kind == Conversation.Kind.GROUP:
            state = GroupReadState.objects.filter(
                conversation=obj,
                user=request.user,
            ).only("last_read_message_id").first()
            last_read_id = state.last_read_message_id if state else None
            unread = obj.messages.exclude(sender=request.user).exclude(
                sender_id__in=blocked_user_ids(request.user)
            )
            if last_read_id:
                unread = unread.filter(id__gt=last_read_id)
            return unread.count()
        return obj.messages.filter(recipient=request.user, read_at__isnull=True).count()
