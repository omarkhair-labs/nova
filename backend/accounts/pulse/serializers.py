from rest_framework import serializers

from .models import Pulse, User


class PulseAuthorSerializer(serializers.ModelSerializer):
    avatar_url = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ("id", "username", "name", "avatar_url")

    def get_avatar_url(self, user):
        request = self.context.get("request")
        if not user.avatar:
            return ""
        url = user.avatar.url
        return request.build_absolute_uri(url) if request is not None else url


class PulseSerializer(serializers.ModelSerializer):
    author = PulseAuthorSerializer(read_only=True)
    media_url = serializers.SerializerMethodField()
    is_mine = serializers.SerializerMethodField()

    class Meta:
        model = Pulse
        fields = (
            "id",
            "author",
            "media_url",
            "media_type",
            "audience",
            "note",
            "created_at",
            "expires_at",
            "is_mine",
        )

    def get_media_url(self, pulse):
        if not pulse.media:
            return ""
        request = self.context.get("request")
        url = pulse.media.url
        return request.build_absolute_uri(url) if request is not None else url

    def get_is_mine(self, pulse):
        request = self.context.get("request")
        return bool(request is not None and request.user.pk == pulse.author_id)
