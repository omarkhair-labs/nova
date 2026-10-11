from rest_framework import serializers
from django.utils import timezone
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
    next_follow_up_on = serializers.DateField(read_only=True, allow_null=True)

    class Meta:
        model = PlantUnit
        fields = ("id", "space", "unit_type", "label", "cover_photo", "my_name", "species_label", "placement_note", "is_active", "can_record", "last_event_kind", "last_event_at", "next_follow_up_on")

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
        if not photo:
            return photo
        # Normalize uploaded cover images: strip GPS/EXIF metadata and
        # reject oversized/unsafe formats before putting bytes on storage.
        from .media import sanitize_public_plant_photo
        try:
            clean = sanitize_public_plant_photo(photo)
        except ValueError as error:
            raise serializers.ValidationError(str(error)) from error
        clean.name = "plant-cover.jpg"
        return clean


class PlantEventSerializer(serializers.ModelSerializer):
    class Meta:
        model = PlantEvent
        fields = (
            "id", "kind", "evidence_type", "note", "photo", "observed_at", "created_at",
            "follow_up_on", "follow_up_completed_at", "follow_up_for",
        )
        read_only_fields = ("id", "created_at", "follow_up_completed_at")

    def validate(self, attrs):
        follow_up_on = attrs.get("follow_up_on")
        if follow_up_on and follow_up_on < timezone.localdate():
            raise serializers.ValidationError({"follow_up_on": "Choose today or a future check-in date."})
        source = attrs.get("follow_up_for")
        if source is not None:
            plant = self.context.get("plant")
            if (
                plant is None or source.plant_id != plant.pk
                or source.follow_up_on is None or source.follow_up_completed_at is not None
                or attrs.get("kind") != PlantEvent.Kind.OUTCOME
            ):
                raise serializers.ValidationError({"follow_up_for": "Not an open follow-up for this plant."})
            if not attrs.get("note", "").strip():
                raise serializers.ValidationError({"note": "Describe what actually changed before closing a check-in."})
        if follow_up_on and attrs.get("kind") == PlantEvent.Kind.OUTCOME:
            raise serializers.ValidationError({"follow_up_on": "Create a new care/observation check-in instead."})
        return attrs
