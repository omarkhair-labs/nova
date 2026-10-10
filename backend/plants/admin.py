from django.contrib import admin
from .models import PlantEvent, PlantNickname, PlantUnit, Space, SpaceMember


class SpaceMemberInline(admin.TabularInline):
    model = SpaceMember
    extra = 0


@admin.register(Space)
class SpaceAdmin(admin.ModelAdmin):
    list_display = ("name", "owner", "created_at")
    search_fields = ("name", "owner__email")
    inlines = (SpaceMemberInline,)


@admin.register(PlantUnit)
class PlantUnitAdmin(admin.ModelAdmin):
    list_display = ("label", "space", "unit_type", "species_label", "is_active")
    list_filter = ("unit_type", "is_active")
    search_fields = ("label", "species_label", "space__name")


@admin.register(PlantEvent)
class PlantEventAdmin(admin.ModelAdmin):
    list_display = ("plant", "kind", "actor", "evidence_type", "observed_at")
    list_filter = ("kind", "evidence_type")
    search_fields = ("plant__label", "note")


@admin.register(PlantNickname)
class PlantNicknameAdmin(admin.ModelAdmin):
    list_display = ("plant", "user", "nickname")
