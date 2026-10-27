"""Same-origin staff web product, deliberately independent of Django Admin UI."""
import io
from functools import wraps
from urllib.parse import urlencode

import qrcode
import qrcode.image.svg
from django.contrib import messages
from django.contrib.auth import login, logout
from django.core.cache import cache
from django.core.exceptions import PermissionDenied, ValidationError
from django.core.paginator import Paginator
from django.db import IntegrityError, transaction
from django.db.models import Count, Q
from django.http import Http404, HttpResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.utils import timezone
from django.utils.http import url_has_allowed_host_and_scheme
from django.utils.safestring import mark_safe
from django.views.decorators.debug import sensitive_post_parameters
from django.views.decorators.http import require_http_methods

from . import company_services as service
from .company_forms import (GuideForm, ImportApprovalForm, InstallationForm, InvitationForm,
                            PlantForm, ReviewForm, SpeciesForm, StaffAuthenticationForm, UploadForm)
from .handovers import HandoverError, pilot_enabled
from .inventory_import import (ImportProblem, PERMISSIONS, apply_preview, digest,
                               make_preview, parse_upload, template_bytes)
from .models import (BotanicalSpecies, CareGuideVersion, HandoverInvitation,
                     InstallationBatch, InstallationManifestRow, PlantUnit, Space)


def page(request, template, context=None, status=200):
    from .demo_services import enabled as demo_enabled
    user = request.user
    context = {"ceo_demo_enabled": demo_enabled(), "can_import": user.has_perms(PERMISSIONS),
               "can_inventory": user.has_perm("plants.view_plantunit"),
               "can_care": user.has_perms(("plants.view_careguideversion", "plants.view_botanicalspecies")),
               "can_handovers": user.has_perm("plants.view_handoverinvitation"),
               "handover_enabled": pilot_enabled(), **(context or {})}
    return render(request, "plants/company/" + template + ".html", context, status=status)


def portal(*permissions, public=False, methods=("GET", "POST")):
    def decorate(fn):
        @sensitive_post_parameters()
        @require_http_methods(methods)
        @wraps(fn)
        def wrapped(request, *args, **kwargs):
            if not service.enabled():
                raise Http404
            try:
                if not public:
                    if not request.user.is_authenticated:
                        return redirect(reverse("company:login") + "?" + urlencode({"next": request.path}))
                    service.require(request.user, *permissions)
                response = fn(request, *args, **kwargs)
            except PermissionDenied:
                response = page(request, "error", {"title": "Access is restricted", "detail": "Your account does not have permission for this company workspace."}, 403)
            except Http404:
                response = page(request, "error", {"title": "Record unavailable", "detail": "This record is unavailable in your authorized workspace."}, 404)
            except (ImportProblem, HandoverError) as exc:
                response = page(request, "error", {"title": "Action needs review", "detail": str(exc)}, 409)
            response["Cache-Control"] = "private, no-store, max-age=0"
            response["Pragma"] = "no-cache"
            response["Referrer-Policy"] = "same-origin"
            response["X-Robots-Tag"] = "noindex, nofollow"
            response["Content-Security-Policy"] = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'"
            return response
        return wrapped
    return decorate


def get_batch(request, batch_id):
    batch = get_object_or_404(service.batches(request.user).select_related("space__owner", "prepared_by", "reviewed_by"), pk=batch_id)
    batch.inventory_visible = service.inventory_visible(request.user, batch)
    batch.site_label = batch.space.name if batch.inventory_visible else "Customer-private space after handover"
    return batch


def paginated(request, queryset):
    return Paginator(queryset, 24).get_page(request.GET.get("page"))


@portal(public=True)
def login_view(request):
    if request.user.is_authenticated:
        service.require(request.user)
        return redirect("company:overview")
    form = StaffAuthenticationForm(request, data=request.POST or None)
    if request.method == "POST":
        key = "plantupia-company-login:" + digest([request.META.get("REMOTE_ADDR", ""), request.POST.get("username", "").casefold()[:150]])
        if cache.add(key, 1, timeout=60):
            attempts = 1
        else:
            try:
                attempts = cache.incr(key)
            except ValueError:
                cache.set(key, 1, timeout=60)
                attempts = 1
        if attempts > 10:
            form = StaffAuthenticationForm(request, data={"username": request.POST.get("username", ""), "password": ""})
            form.is_valid()
            form.add_error(None, "Too many attempts. Wait a minute before trying again.")
        elif form.is_valid():
            login(request, form.get_user())
            target = request.POST.get("next", "")
            if not target.startswith("/company/") or not url_has_allowed_host_and_scheme(target, {request.get_host()}, require_https=request.is_secure()):
                target = reverse("company:overview")
            return redirect(target)
    return page(request, "login", {"form": form, "next": request.GET.get("next", ""), "title": "Company sign in"})


@portal(methods=("POST",))
def logout_view(request):
    logout(request)
    return redirect("company:login")


@portal(methods=("GET",))
def overview(request):
    qs = service.batches(request.user)
    counts = {s: qs.filter(status=s).count() for s in ("draft", "ready", "claimed")}
    drafts = qs.filter(status="draft").select_related("space__owner").annotate(plant_total=Count("manifest_rows")).order_by("-created_at")[:4]
    can_care = request.user.has_perms(("plants.view_careguideversion", "plants.view_botanicalspecies"))
    guides = CareGuideVersion.objects.filter(published=False, ever_published=False).select_related("species", "assigned_reviewer").order_by("pk")[:4] if can_care else []
    pending_guides = CareGuideVersion.objects.filter(published=False, ever_published=False).count() if can_care else None
    invitations = HandoverInvitation.objects.filter(batch__in=qs).select_related("batch", "intended_recipient").order_by("-created_at")[:4] if request.user.has_perm("plants.view_handoverinvitation") else []
    return page(request, "overview", {"title": "Overview", "section": "overview", "counts": counts, "drafts": drafts,
        "guides": guides, "pending_guides": pending_guides, "invitations": invitations})


@portal(methods=("GET",))
def installations(request):
    qs = service.batches(request.user).select_related("space__owner").annotate(plant_total=Count("manifest_rows"))
    query, state = request.GET.get("q", "").strip()[:80], request.GET.get("status", "")
    if state in InstallationBatch.Status.values:
        qs = qs.filter(status=state)
    if query:
        qs = qs.filter(Q(reference_code__icontains=query) | Q(space__name__icontains=query, status__in=("draft", "ready")))
    records = paginated(request, qs.order_by("-created_at"))
    for batch in records:
        batch.site_label = batch.space.name if service.inventory_visible(request.user, batch) else "Customer-private space after handover"
    return page(request, "installations", {"title": "Installations", "section": "installations", "records": records, "query": query, "state": state})


@portal("plants.view_plantunit", methods=("GET",))
def installation_detail(request, batch_id):
    batch = get_batch(request, batch_id)
    rows = list(batch.manifest_rows.select_related("plant__verified_species")) if batch.inventory_visible else []
    if request.user.has_perms(("plants.view_careguideversion", "plants.view_botanicalspecies")):
        species_ids = {row.plant.verified_species_id for row in rows if row.plant.verified_species_id}
        guides = {}
        for guide in CareGuideVersion.objects.filter(species_id__in=species_ids, published=True).select_related("reviewer").order_by("species_id", "-version"):
            guides.setdefault(guide.species_id, guide)
        for row in rows:
            row.published_guide = guides.get(row.plant.verified_species_id)
    return page(request, "installation", {"title": batch.reference_code, "section": "installations", "batch": batch,
        "rows": rows, "manifest_count": batch.manifest_rows.count(),
        "extra_count": batch.space.plants.count() - batch.manifest_rows.count() if batch.inventory_visible else 0,
        "receipts": batch.import_receipts.select_related("approved_by").order_by("-approved_at")[:10] if batch.inventory_visible else [],
        "invitation_count": batch.invitations.count() if request.user.has_perm("plants.view_handoverinvitation") else None})


@portal("plants.change_installationbatch", "plants.change_space")
def installation_edit(request, batch_id=None):
    batch = get_batch(request, batch_id) if batch_id else None
    if batch:
        service.require_draft(request.user, batch)
    else:
        service.require(request.user, "plants.add_installationbatch", "plants.add_space")
    initial = {"reference_code": batch.reference_code, "name": batch.space.name, "description": batch.space.description, "location_note": batch.space.location_note} if batch else {}
    form = InstallationForm(request.POST or None, initial=initial)
    if request.method == "POST" and form.is_valid():
        try:
            with transaction.atomic():
                if batch:
                    batch = InstallationBatch.objects.select_for_update().get(pk=batch.pk)
                    batch.space = Space.objects.select_for_update().select_related("owner").get(pk=batch.space_id)
                    service.require_draft(request.user, batch)
                    space = batch.space
                else:
                    space = Space(owner=request.user)
                for key in ("name", "description", "location_note"):
                    setattr(space, key, form.cleaned_data[key])
                space.full_clean()
                space.save()
                if batch is None:
                    batch = InstallationBatch(space=space, prepared_by=request.user)
                batch.reference_code = form.cleaned_data["reference_code"]
                batch.full_clean()
                batch.save()
                service.audit(request.user, batch, "Company dashboard: draft installation prepared/edited")
            messages.success(request, "Draft installation saved. No customer access was granted.")
            return redirect("company:installation", batch_id=batch.pk)
        except (ValidationError, IntegrityError):
            form.add_error(None, "The installation could not be saved. Check the fields and use a unique installation reference.")
    return page(request, "form", {"title": "Edit installation" if batch else "Prepare an installation", "section": "installations", "form": form, "eyebrow": "01 / Prepare", "submit": "Save draft installation", "description": "A private staff-owned Space and its delivery inventory. Customer access is a separate, reviewed handover."})


@portal("plants.change_installationbatch", "plants.view_plantunit")
def installation_review(request, batch_id):
    batch = get_batch(request, batch_id)
    service.require_draft(request.user, batch)
    form = ReviewForm(request.POST or None, initial={"review_token": service.action_token(request.user, "ready", batch, service.inventory_state(batch))})
    if request.method == "POST" and form.is_valid():
        try:
            service.review_installation(request.user, batch.pk, form.cleaned_data["review_token"])
            messages.success(request, "Inventory reviewed and locked as READY. No invitation has been issued.")
            return redirect("company:installation", batch_id=batch.pk)
        except (ImportProblem, ValidationError) as exc:
            form.add_error(None, str(exc))
    return page(request, "review", {"title": "Review the complete inventory", "section": "installations", "batch": batch,
        "rows": batch.manifest_rows.select_related("plant__verified_species"), "form": form})


@portal(*PERMISSIONS)
def import_workspace(request, batch_id):
    batch = get_batch(request, batch_id)
    service.require_draft(request.user, batch)
    form, approval, plan, applied = UploadForm(), None, None, None
    if request.method == "POST":
        if "preview" in request.POST:
            approval = ImportApprovalForm(request.POST)
            if approval.is_valid():
                try:
                    receipt, created = apply_preview(request.user, approval.cleaned_data["preview"],
                        confirmed=approval.cleaned_data["confirmed"], identities_confirmed=approval.cleaned_data["identities_confirmed"], expected_batch_id=batch.pk)
                    applied = {"summary": receipt.summary, "created": created}
                    service.audit(request.user, batch, "Company dashboard: inventory import approved" if created else "Company dashboard: import replay; no changes")
                    approval = None
                except ImportProblem as exc:
                    approval.add_error(None, str(exc))
        else:
            form = UploadForm(request.POST, request.FILES)
            if form.is_valid():
                try:
                    plan, token = make_preview(request.user, batch, parse_upload(form.cleaned_data["file"], batch))
                    if token:
                        approval = ImportApprovalForm(initial={"preview": token})
                except ImportProblem as exc:
                    form.add_error("file", str(exc))
    return page(request, "import", {"title": "Import plant inventory", "section": "installations", "batch": batch,
        "form": form, "approval": approval, "plan": plan, "applied": applied,
        "receipts": batch.import_receipts.select_related("approved_by").order_by("-approved_at")[:10]})


@portal(*PERMISSIONS, methods=("GET",))
def import_template(request, batch_id):
    batch = get_batch(request, batch_id)
    service.require_draft(request.user, batch)
    csv_only = request.GET.get("format") == "csv"
    response = HttpResponse(template_bytes(batch, csv_only), content_type="text/csv; charset=utf-8" if csv_only else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    response["Content-Disposition"] = 'attachment; filename="plantupia-inventory-v1.' + ("csv" if csv_only else "xlsx") + '"'
    return response


@portal("plants.view_plantunit", methods=("GET",))
def inventory(request):
    qs = PlantUnit.objects.filter(space__installation_batch__in=service.batches(request.user), space__owner__is_staff=True)
    if not request.user.is_superuser:
        qs = qs.filter(space__owner=request.user)
    query = request.GET.get("q", "").strip()[:80]
    if query:
        qs = qs.filter(Q(label__icontains=query) | Q(species_label__icontains=query))
    return page(request, "inventory", {"title": "Plant inventory", "section": "inventory", "query": query,
        "records": paginated(request, qs.select_related("space__installation_batch", "verified_species").order_by("label", "pk"))})


@portal("plants.view_plantunit")
def plant_edit(request, batch_id, plant_id=None):
    batch = get_batch(request, batch_id)
    service.require_draft(request.user, batch)
    plant = get_object_or_404(PlantUnit, pk=plant_id, space=batch.space, installation_manifest_row__batch=batch) if plant_id else None
    service.require(request.user, *( ("plants.change_plantunit", "plants.change_installationmanifestrow") if plant else ("plants.add_plantunit", "plants.add_installationmanifestrow") ))
    source_key = plant.installation_manifest_row.source_key if plant else ""
    form = PlantForm(request.POST or None, instance=plant, initial={"source_key": source_key})
    if plant:
        form.fields["source_key"].disabled = True
    if request.method == "POST" and form.is_valid():
        try:
            with transaction.atomic():
                locked = InstallationBatch.objects.select_for_update().get(pk=batch.pk)
                locked.space = Space.objects.select_for_update().select_related("owner").get(pk=locked.space_id)
                service.require_draft(request.user, locked)
                old = PlantUnit.objects.select_for_update().get(pk=plant.pk) if plant else None
                obj = form.save(commit=False)
                obj.space = locked.space
                if old and old.species_label != obj.species_label:
                    obj.verified_species, obj.identity_reference, obj.identity_verified_by, obj.identity_verified_at = None, "", None, None
                obj.full_clean()
                obj.save()
                if not plant:
                    row = InstallationManifestRow(batch=locked, plant=obj, source_key=form.cleaned_data["source_key"])
                    row.full_clean()
                    row.save()
                service.audit(request.user, obj, "Company dashboard: draft inventory unit saved")
            messages.success(request, "Plant record saved. Identity verification requires independently reviewed evidence in the import workflow.")
            return redirect("company:installation", batch_id=batch.pk)
        except (ValidationError, IntegrityError):
            form.add_error(None, "The plant could not be saved. Check the fields and use a unique source key for a new plant.")
    return page(request, "form", {"title": "Edit plant context" if plant else "Add a plant", "section": "inventory", "form": form,
        "submit": "Save plant record", "description": "Keep the biological identity and original history. Free-text species is unverified; photos use the existing separate asset upload."})


@portal("plants.view_careguideversion", "plants.view_botanicalspecies", methods=("GET",))
def care_library(request):
    qs = CareGuideVersion.objects.select_related("species", "reviewer", "assigned_reviewer")
    state = request.GET.get("status", "")
    if state == "published":
        qs = qs.filter(published=True)
    elif state == "draft":
        qs = qs.filter(published=False, ever_published=False)
    elif state == "assigned":
        qs = qs.filter(published=False, assigned_reviewer=request.user)
    query = request.GET.get("q", "").strip()[:80]
    if query:
        qs = qs.filter(Q(species__scientific_name__icontains=query) | Q(source_reference__icontains=query))
    return page(request, "care", {"title": "Botanical care library", "section": "care", "state": state, "query": query,
        "records": paginated(request, qs.order_by("species__scientific_name", "-version")),
        "species": BotanicalSpecies.objects.annotate(guide_total=Count("guides")).order_by("scientific_name")[:100]})


@portal("plants.add_botanicalspecies", "plants.view_botanicalspecies")
def species_create(request):
    form = SpeciesForm(request.POST or None)
    if request.method == "POST" and form.is_valid():
        obj = form.save()
        service.audit(request.user, obj, "Company dashboard: botanical identity curated; plant identity not implied")
        messages.success(request, "Species saved. This does not verify any individual plant.")
        return redirect("company:care")
    return page(request, "form", {"title": "Curate a species", "section": "care", "form": form, "submit": "Save species", "description": "A reusable botanical identity, distinct from a personal plant name or unverified species label."})


@portal("plants.view_careguideversion", "plants.view_botanicalspecies")
def guide_edit(request, guide_id=None):
    guide = get_object_or_404(CareGuideVersion, pk=guide_id) if guide_id else None
    service.require(request.user, "plants.change_careguideversion" if guide else "plants.add_careguideversion")
    if guide and guide.ever_published:
        raise ImportProblem("Published versions are immutable, even after withdrawal. Create a new version.")
    form = GuideForm(request.POST or None, instance=guide)
    if request.method == "POST" and form.is_valid():
        try:
            with transaction.atomic():
                obj = form.save(commit=False)
                obj.reviewer, obj.reviewed_at, obj.published = None, None, False
                obj.save()
                service.audit(request.user, obj, "Company dashboard: unreviewed care-guide draft saved")
            return redirect("company:guide", guide_id=obj.pk)
        except (ValidationError, IntegrityError):
            form.add_error(None, "This version changed or conflicts with an existing guide. Reload and review; published content cannot be overwritten.")
    return page(request, "form", {"title": "Edit care-guide draft" if guide else "Create a care-guide version", "section": "care",
        "form": form, "submit": "Save unpublished draft", "description": "Record the actual source and contextual guidance. A reviewer assignment is not botanical approval; publication is a separate confirmation."})


@portal("plants.view_careguideversion", "plants.view_botanicalspecies")
def guide_detail(request, guide_id):
    guide = get_object_or_404(CareGuideVersion.objects.select_related("species", "reviewer", "assigned_reviewer"), pk=guide_id)
    form = ReviewForm(request.POST or None, initial={"review_token": service.action_token(request.user, "guide", guide, service.guide_state(guide))})
    if request.method == "POST" and form.is_valid():
        try:
            service.publish_guide(request.user, guide.pk, form.cleaned_data["review_token"], withdraw=request.POST.get("action") == "withdraw")
            messages.success(request, "Publication withdrawn; version retained." if request.POST.get("action") == "withdraw" else "Guide published after your explicit source/content review.")
            return redirect("company:guide", guide_id=guide.pk)
        except (ImportProblem, ValidationError) as exc:
            form.add_error(None, str(exc))
    can_publish = request.user.has_perms(service.PUBLISH) and (not guide.assigned_reviewer_id or guide.assigned_reviewer_id == request.user.pk or request.user.is_superuser)
    return page(request, "guide", {"title": guide.species.scientific_name, "section": "care", "guide": guide, "form": form, "can_publish": can_publish})


@portal("plants.view_handoverinvitation", methods=("GET",))
def handovers(request):
    qs = HandoverInvitation.objects.filter(batch__in=service.batches(request.user)).select_related("batch", "intended_recipient", "issued_by").order_by("-created_at")
    records = paginated(request, qs)
    for invitation in records:
        invitation.delivery_state = "Claimed" if invitation.claimed_at else "Revoked" if invitation.revoked_at else "Expired" if invitation.expires_at <= timezone.now() else "Awaiting recipient"
    ready = service.batches(request.user).filter(status="ready", space__owner__is_staff=True).select_related("space")
    return page(request, "handovers", {"title": "Delivery handovers", "section": "handovers", "records": records, "ready": ready})


@portal("plants.add_handoverinvitation", "plants.view_handoverinvitation")
def handover_issue(request, batch_id):
    batch = get_batch(request, batch_id)
    if not batch.inventory_visible:
        raise PermissionDenied
    if batch.status != "ready" or not pilot_enabled():
        raise ImportProblem("Issuance needs a reviewed READY installation and explicitly enabled isolated-QA handovers.")
    form = InvitationForm(request.POST or None, initial={"review_token": service.action_token(request.user, "issue", batch, service.invitation_state(batch))})
    if request.method == "POST" and form.is_valid():
        try:
            code, invitation = service.issue_invitation(request.user, batch.pk, form.cleaned_data["recipient_username"], form.cleaned_data["mode"], form.cleaned_data["review_token"], replace=form.cleaned_data["replace_existing"])
            image = qrcode.make(code, image_factory=qrcode.image.svg.SvgPathImage)
            output = io.BytesIO()
            image.save(output)
            svg = output.getvalue().decode("utf-8").split("?>")[-1]
            return page(request, "secret", {"title": "Secure handover code", "section": "handovers", "batch": batch, "invitation": invitation,
                "code": code, "qr": mark_safe(svg)})  # SVG is generated solely from an ASCII random token, never submitted markup.
        except (ImportProblem, HandoverError) as exc:
            form.add_error(None, str(exc))
    return page(request, "form", {"title": "Issue a QA handover", "section": "handovers", "batch": batch, "form": form,
        "submit": "Confirm recipient and issue once", "description": "Select the exact existing recipient and explicitly approved QA ownership mode. The code and locally generated QR are shown once; a scan alone never grants access."})


@portal("plants.change_handoverinvitation", "plants.view_handoverinvitation")
def handover_revoke(request, invitation_id):
    invitation = get_object_or_404(HandoverInvitation.objects.filter(batch__in=service.batches(request.user)).select_related("batch", "intended_recipient"), pk=invitation_id)
    form = ReviewForm(request.POST or None, initial={"review_token": service.action_token(request.user, "revoke", invitation, service.invitation_state(invitation.batch))})
    if request.method == "POST" and form.is_valid():
        try:
            service.revoke_invitation(request.user, invitation.pk, form.cleaned_data["review_token"])
            messages.success(request, "Unused invitation revoked. The previous code no longer grants access.")
            return redirect("company:handovers")
        except (ImportProblem, HandoverError) as exc:
            form.add_error(None, str(exc))
    return page(request, "form", {"title": "Revoke this invitation", "section": "handovers", "form": form, "submit": "Confirm revocation",
        "description": f"Installation {invitation.batch.reference_code}. Intended account: {invitation.intended_recipient.username}. No plant or history will be deleted."})
