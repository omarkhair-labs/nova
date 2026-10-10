from django.contrib import admin
from .models import PlantEvent, PlantNickname, PlantUnit, Space, SpaceMember, PlantDiscussion


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

@admin.register(PlantDiscussion)
class PlantDiscussionAdmin(admin.ModelAdmin):
    list_display = ("public_topic", "plant", "post", "created_at", "resolved_at")
    search_fields = ("public_topic", "plant__label", "post__caption")
    list_filter = ("resolved_at",)
    readonly_fields = ("post", "plant", "resolution_event", "created_at", "resolved_at")
