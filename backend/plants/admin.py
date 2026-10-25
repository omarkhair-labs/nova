from django.contrib import admin, messages
from django.core.exceptions import ValidationError
from django.db import transaction
from django.utils import timezone
from django import forms
from django.contrib.auth import get_user_model
from django.core.exceptions import PermissionDenied
from django.template.response import TemplateResponse
from django.urls import path
from django.views.decorators.debug import sensitive_post_parameters
from django.utils.decorators import method_decorator
from .handovers import HandoverError, issue_pilot_invitation, pilot_enabled
from .models import BotanicalSpecies, CareGuideVersion, HandoverInvitation
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


@admin.register(BotanicalSpecies)
class BotanicalSpeciesAdmin(admin.ModelAdmin):
    search_fields = ("key", "scientific_name")


@admin.register(CareGuideVersion)
class CareGuideVersionAdmin(admin.ModelAdmin):
    list_display = ("species", "version", "reviewer", "reviewed_at", "published")
    list_filter = ("published",)

    def get_readonly_fields(self, request, obj=None):
        if obj and obj.published:
            return tuple(field.name for field in obj._meta.fields if field.name != "published")
        return ()

    def has_delete_permission(self, request, obj=None):
        return bool(not obj or not obj.published) and super().has_delete_permission(request, obj)


class PilotInvitationForm(forms.Form):
    batch = forms.ModelChoiceField(queryset=InstallationBatch.objects.filter(status="ready"))
    recipient = forms.ModelChoiceField(queryset=get_user_model().objects.filter(is_active=True, is_staff=False))
    mode = forms.ChoiceField(choices=[("", "Select explicitly approved QA ownership mode")] + list(HandoverInvitation.Mode.choices))
    approved_qa_policy = forms.BooleanField(label="This is isolated QA and the selected simulated ownership mode is approved for this test")


@admin.register(HandoverInvitation)
class HandoverInvitationAdmin(admin.ModelAdmin):
    list_display = ("batch", "intended_recipient", "mode", "expires_at", "revoked_at", "claimed_at")
    readonly_fields = tuple(f.name for f in HandoverInvitation._meta.fields if f.name != "token_digest")
    exclude = ("token_digest",)
    actions = ("revoke_invitations",)
    change_list_template = "admin/plants/handover_list.html"

    def get_urls(self):
        return [path("issue/", self.admin_site.admin_view(self.issue_view), name="plants_handoverinvitation_issue")] + super().get_urls()

    def has_add_permission(self, request):
        return False  # Issuance service is the only creation path.

    def has_delete_permission(self, request, obj=None):
        return False

    @method_decorator(sensitive_post_parameters())
    def issue_view(self, request):
        if not request.user.has_perm("plants.add_handoverinvitation") or not pilot_enabled():
            raise PermissionDenied
        form = PilotInvitationForm(request.POST or None)
        raw_code = None
        if request.method == "POST" and form.is_valid():
            try:
                raw_code, invitation = issue_pilot_invitation(
                    batch_id=form.cleaned_data["batch"].pk, recipient=form.cleaned_data["recipient"],
                    staff=request.user, mode=form.cleaned_data["mode"],
                )
                self.log_addition(request, invitation, "QA invitation issued; secret withheld")
            except HandoverError as exc:
                form.add_error(None, exc.code)
        response = TemplateResponse(request, "admin/plants/handover_issue.html", {
            **self.admin_site.each_context(request), "title": "Isolated QA handover", "form": form,
            "raw_code": raw_code, "opts": self.model._meta,
        })
        response["Cache-Control"] = "no-store, private"
        response["Referrer-Policy"] = "no-referrer"
        return response

    @admin.action(description="Revoke selected unused QA invitations")
    def revoke_invitations(self, request, queryset):
        if not pilot_enabled() or not self.has_change_permission(request):
            raise PermissionDenied
        for invitation_id, batch_id in queryset.values_list("pk", "batch_id"):
            with transaction.atomic():
                InstallationBatch.objects.select_for_update().get(pk=batch_id)
                invitation = HandoverInvitation.objects.select_for_update().get(pk=invitation_id)
                if invitation.claimed_at is None and invitation.revoked_at is None:
                    invitation.revoked_at = timezone.now()
                    invitation.save(update_fields=("revoked_at",))
                    self.log_change(request, invitation, "Unused QA invitation revoked")


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
