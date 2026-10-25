from django.contrib import admin, messages
from django.core.exceptions import ValidationError
from django.db import transaction
from django.utils import timezone
from .models import (InstallationBatch, InstallationManifestRow, PlantEvent,
                     PlantNickname, PlantUnit, Space, SpaceMember, PlantDiscussion)


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


class InstallationManifestRowInline(admin.TabularInline):
    model = InstallationManifestRow
    extra = 0
    autocomplete_fields = ("plant",)
    fields = ("source_key", "plant")

    def has_add_permission(self, request, obj=None):
        return bool(obj and obj.status == InstallationBatch.Status.DRAFT) and super().has_add_permission(request, obj)

    def has_change_permission(self, request, obj=None):
        return bool(obj and obj.status == InstallationBatch.Status.DRAFT) and super().has_change_permission(request, obj)

    def has_delete_permission(self, request, obj=None):
        return bool(obj and obj.status == InstallationBatch.Status.DRAFT) and super().has_delete_permission(request, obj)


@admin.register(InstallationBatch)
class InstallationBatchAdmin(admin.ModelAdmin):
    """G1 operations: staff-owned staged inventory, NOT an activated QR claim."""

    list_display = ("reference_code", "space", "status", "prepared_by", "reviewed_at")
    list_filter = ("status",)
    search_fields = ("reference_code", "space__name")
    readonly_fields = ("status", "prepared_by", "reviewed_by", "reviewed_at", "created_at")
    inlines = (InstallationManifestRowInline,)
    actions = ("mark_inventory_ready",)

    def save_model(self, request, obj, form, change):
        if not change:
            obj.prepared_by = request.user
        # Stage only; the reviewer action is the sole route to READY.
        obj.full_clean()
        super().save_model(request, obj, form, change)

    def has_change_permission(self, request, obj=None):
        if obj and obj.status != InstallationBatch.Status.DRAFT:
            return False
        return super().has_change_permission(request, obj)

    @admin.action(description="Review inventory and mark as ready (no customer access)")
    def mark_inventory_ready(self, request, queryset):
        if not request.user.is_staff:
            self.message_user(request, "A staff reviewer is required.", level=messages.ERROR)
            return
        ready_count = 0
        for pk in queryset.values_list("pk", flat=True):
            try:
                with transaction.atomic():
                    batch = InstallationBatch.objects.select_for_update().select_related(
                        "space__owner", "prepared_by",
                    ).get(pk=pk)
                    if batch.status != InstallationBatch.Status.DRAFT:
                        raise ValidationError("This installation is already reviewed.")
                    if not batch.manifest_rows.exists():
                        raise ValidationError("Add actual manifest plants before marking ready.")
                    if batch.manifest_rows.exclude(plant__space_id=batch.space_id).exists():
                        raise ValidationError("A manifest plant is from the wrong space.")
                    batch.reviewed_by = request.user
                    batch.reviewed_at = timezone.now()
                    batch.status = InstallationBatch.Status.READY
                    batch.full_clean()
                    batch.save(update_fields=("reviewed_by", "reviewed_at", "status"))
                    ready_count += 1
            except ValidationError as exc:
                self.message_user(
                    request, f"Installation {pk} could not be reviewed: {exc}",
                    level=messages.ERROR,
                )
        if ready_count:
            self.message_user(
                request,
                f"{ready_count} inventory batch(es) reviewed. No client access or QR was issued.",
                level=messages.SUCCESS,
            )
