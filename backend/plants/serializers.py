from rest_framework import serializers
from .models import PlantEvent, PlantUnit, Space


class SpaceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Space
        fields = ("id", "name", "description", "location_note")


class PlantSerializer(serializers.ModelSerializer):
    my_name = serializers.SerializerMethodField()
    can_record = serializers.SerializerMethodField()
    last_event_kind = serializers.CharField(read_only=True, allow_null=True)
    last_event_at = serializers.DateTimeField(read_only=True, allow_null=True)

    class Meta:
        model = PlantUnit
        fields = ("id", "space", "unit_type", "label", "cover_photo", "my_name", "species_label", "placement_note", "is_active", "can_record", "last_event_kind", "last_event_at")

    def get_can_record(self, obj):
        return obj.space_id in self.context.get("editable_space_ids", set())

    def get_my_name(self, obj):
        user = self.context["request"].user
        for item in obj.nicknames.all():
            if item.user_id == user.pk:
                return item.nickname
        return obj.label


class SpaceCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Space
        fields = ("id", "name", "description", "location_note")
        read_only_fields = ("id",)

    def validate_name(self, value):
        value = value.strip()
        if not value:
            raise serializers.ValidationError("Give your space a name.")
        return value


class PlantCreateSerializer(serializers.ModelSerializer):
    # Take an ID rather than a Space object. The view strictly resolves it
    # against spaces the requester owns or manages.
    space = serializers.UUIDField()

    class Meta:
        model = PlantUnit
        fields = ("id", "space", "unit_type", "label", "species_label", "placement_note", "cover_photo")
        read_only_fields = ("id",)

    def validate_label(self, value):
        value = value.strip()
        if not value:
            raise serializers.ValidationError("Give your plant a name.")
        return value

    def validate_cover_photo(self, photo):
        if photo and photo.size > 10 * 1024 * 1024:
            raise serializers.ValidationError("Choose a photo smaller than 10 MB.")
        return photo


class PlantEventSerializer(serializers.ModelSerializer):
    class Meta:
        model = PlantEvent
        fields = ("id", "kind", "evidence_type", "note", "photo", "observed_at", "created_at")
        read_only_fields = ("id", "created_at")
