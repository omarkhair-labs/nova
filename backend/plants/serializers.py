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


class PlantEventSerializer(serializers.ModelSerializer):
    class Meta:
        model = PlantEvent
        fields = ("id", "kind", "evidence_type", "note", "photo", "observed_at", "created_at")
        read_only_fields = ("id", "created_at")
