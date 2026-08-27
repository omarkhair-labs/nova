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
    thumbnail_url = serializers.SerializerMethodField()
    is_mine = serializers.SerializerMethodField()
    reply_to_id = serializers.IntegerField(read_only=True)
    chain_root_id = serializers.IntegerField(read_only=True)
    reactions_count = serializers.SerializerMethodField()
    viewers_count = serializers.SerializerMethodField()
    is_reacted = serializers.SerializerMethodField()

    class Meta:
        model = Pulse
        fields = (
            "id",
            "author",
            "media_url",
            "thumbnail_url",
            "media_type",
            "audience",
            "category",
            "note",
            "created_at",
            "expires_at",
            "is_mine",
            "reply_to_id",
            "chain_root_id",
            "reactions_count",
            "viewers_count",
            "is_reacted",
        )

    def get_media_url(self, pulse):
        if not pulse.media:
            return ""
        request = self.context.get("request")
        url = pulse.media.url
        return request.build_absolute_uri(url) if request is not None else url

    def get_thumbnail_url(self, pulse):
        field = pulse.thumbnail or (pulse.media if pulse.media_type == Pulse.MediaType.IMAGE else None)
        if not field:
            return ""
        request = self.context.get("request")
        url = field.url
        return request.build_absolute_uri(url) if request is not None else url

    def get_is_mine(self, pulse):
        request = self.context.get("request")
        return bool(request is not None and request.user.pk == pulse.author_id)

    def get_reactions_count(self, pulse):
        return pulse.reactions.count()

    def get_viewers_count(self, pulse):
        return pulse.views.count()

    def get_is_reacted(self, pulse):
        request = self.context.get("request")
        return bool(
            request
            and request.user.is_authenticated
            and pulse.reactions.filter(user=request.user).exists()
        )
