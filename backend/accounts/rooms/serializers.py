from rest_framework import serializers

from ..room_models import RoomItem
from ..serializers import PostAuthorSerializer


MAX_ROOM_PHOTO_BYTES = 12 * 1024 * 1024
MAX_ROOM_VIDEO_BYTES = 60 * 1024 * 1024


class RoomItemSerializer(serializers.ModelSerializer):
    created_by = PostAuthorSerializer(read_only=True)
    media_url = serializers.SerializerMethodField()
    reminder_set = serializers.SerializerMethodField()

    class Meta:
        model = RoomItem
        fields = (
            "id",
            "kind",
            "created_by",
            "title",
            "body",
            "url",
            "media_url",
            "scheduled_for",
            "pinned",
            "reminder_set",
            "created_at",
            "updated_at",
        )
        read_only_fields = fields

    def get_media_url(self, obj):
        if not obj.media:
            return ""
        try:
            url = obj.media.url
        except Exception:
            return ""
        request = self.context.get("request")
        return request.build_absolute_uri(url) if request else url

    def get_reminder_set(self, obj):
        request = self.context.get("request")
        return bool(
            request
            and request.user.is_authenticated
            and obj.reminders.filter(user=request.user).exists()
        )


class RoomItemCreateSerializer(serializers.Serializer):
    kind = serializers.ChoiceField(choices=RoomItem.Kind.choices)
    title = serializers.CharField(required=False, allow_blank=True, max_length=120)
    body = serializers.CharField(required=False, allow_blank=True, max_length=500)
    url = serializers.URLField(required=False, allow_blank=True, max_length=700)
    media = serializers.FileField(required=False, allow_null=True)
    scheduled_for = serializers.DateTimeField(required=False, allow_null=True)

    def validate(self, attrs):
        kind = attrs["kind"]
        title = attrs.get("title", "").strip()
        body = attrs.get("body", "").strip()
        url = attrs.get("url", "").strip()
        media = attrs.get("media")

        if kind == RoomItem.Kind.NOTE and not body:
            raise serializers.ValidationError({"body": "A room note needs text."})

        if kind == RoomItem.Kind.PLAN and not title:
            raise serializers.ValidationError({"title": "A plan needs a title."})

        if kind in {RoomItem.Kind.MUSIC, RoomItem.Kind.SAVED} and not url:
            raise serializers.ValidationError({"url": "This room item needs a link."})

        if kind == RoomItem.Kind.PHOTO:
            if media is None:
                raise serializers.ValidationError({"media": "Choose a photo."})
            content_type = str(getattr(media, "content_type", "") or "").lower()
            if content_type and not content_type.startswith("image/"):
                raise serializers.ValidationError({"media": "Room photos must be image files."})
            if media.size > MAX_ROOM_PHOTO_BYTES:
                raise serializers.ValidationError({"media": "Room photos must be 12 MB or smaller."})

        if kind == RoomItem.Kind.VIDEO:
            if media is None:
                raise serializers.ValidationError({"media": "Choose a video."})
            content_type = str(getattr(media, "content_type", "") or "").lower()
            if content_type and not content_type.startswith("video/"):
                raise serializers.ValidationError({"media": "Room videos must be video files."})
            if media.size > MAX_ROOM_VIDEO_BYTES:
                raise serializers.ValidationError({"media": "Room videos must be 60 MB or smaller."})

        if kind not in {RoomItem.Kind.PHOTO, RoomItem.Kind.VIDEO} and media is not None:
            raise serializers.ValidationError({"media": "This room item type doesn't accept media."})

        attrs["title"] = title
        attrs["body"] = body
        attrs["url"] = url
        return attrs
