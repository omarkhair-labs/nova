from rest_framework import serializers
from .models import PlantEvent, PlantUnit, Space


class SpaceSerializer(serializers.ModelSerializer):
    class Meta:
        model = Space
        fields = ("id", "name", "description", "location_note")


class PlantSerializer(serializers.ModelSerializer):
    my_name = serializers.SerializerMethodField()

    class Meta:
        model = PlantUnit
        fields = ("id", "space", "unit_type", "label", "my_name", "species_label", "placement_note", "is_active")

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
