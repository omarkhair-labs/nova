"""Connected company workspaces for isolated CEO QA. No Admin dependency."""
import uuid
import os
from functools import wraps

from django import forms
from django.contrib import messages
from django.core.exceptions import ValidationError
from django.db import transaction
from django.db.models import Q
from django.http import Http404
from django.shortcuts import get_object_or_404, redirect
from django.utils import timezone

from . import company_services as company, demo_services as demo
from .company_forms import ReviewForm
from .company_views import portal, page, paginated
from .media import sanitize_public_plant_photo
from .models import (CareInspectionPlan, CareGuideVersion, BotanicalSpecies, PlantUnit,
                     Space, InstallationBatch, ServiceRequest, CatalogProduct, DemoOrder,
                     DemoOffer, DemoEntitlement)


def demo_portal(*permissions, methods=("GET", "POST")):
    def decorate(fn):
        @portal(*permissions, methods=methods)
        @wraps(fn)
        def wrapped(request, *args, **kwargs):
            if not demo.enabled():
                raise Http404
            try:
                return fn(request, *args, **kwargs)
            except demo.DemoProblem as exc:
                return page(request, "error", {"title": "Action needs review", "detail": str(exc.detail.get("detail", "Reload this record before trying again."))}, exc.status_code)
            except ValidationError as exc:
                return page(request, "error", {"title": "Review the input", "detail": " ".join(exc.messages)}, 409)
        return wrapped
    return decorate


def prepared_plants(user):
    plants = PlantUnit.objects.filter(space__installation_batch__in=company.batches(user),
        space__owner__is_staff=True, is_active=True)
    return plants if user.is_superuser else plants.filter(space__owner=user)


def plan_scope(user):
    plans = CareInspectionPlan.objects.filter(plant__space__owner__is_staff=True)
    if user.is_superuser:
        return plans
    own = Q(prepared_by=user, plant__space__owner=user)
    # The actual published-source reviewer can review proposals using that source,
    # while the physical inventory is still staff-owned. Never customer history.
    return plans.filter(own | Q(guide__reviewer=user)) if user.has_perm("plants.approve_careinspectionplan") else plans.filter(own)


class InspectionForm(forms.ModelForm):
    class Meta:
        model = CareInspectionPlan
        fields = ("plant", "guide", "action", "every_days", "start_on", "timezone", "placement_context", "instructions")
        widgets = {"start_on": forms.DateInput(attrs={"type": "date"}), "instructions": forms.Textarea(attrs={"rows": 4})}
        help_texts = {"every_days": "Every N days; 7 means weekly from the start date. An inspection is never a watering command.",
                      "placement_context": "Staff-recorded context. No light, moisture or soil condition is inferred."}

    def __init__(self, *args, user, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["plant"].queryset = prepared_plants(user).filter(verified_species__isnull=False)
        self.fields["guide"].queryset = CareGuideVersion.objects.filter(published=True)
        self.fields["plant"].label_from_instance = lambda p: p.label


class CatalogForm(forms.ModelForm):
    class Meta:
        model = CatalogProduct
        fields = ("name", "description", "kind", "image", "proposed_price", "currency", "qa_stock", "active")
        widgets = {"description": forms.Textarea(attrs={"rows": 3})}
        help_texts = {"proposed_price": "Concept QA price only. No charge or commercial offer.", "qa_stock": "Fictional test stock; no real inventory reservation."}

    def clean_name(self):
        value = self.cleaned_data["name"]
        if not value.startswith("FICTIONAL QA"):
            raise forms.ValidationError("Start the name with FICTIONAL QA; no official merchandise is approved.")
        return value

    def clean_image(self):
        image = self.cleaned_data.get("image")
        if image and hasattr(image, "content_type"):
            try:
                cleaned = sanitize_public_plant_photo(image)
                if cleaned.size > 2 * 1024 * 1024:
                    raise ValueError("Use an image smaller than 2 MiB after normalization.")
                return cleaned
            except ValueError as exc:
                raise forms.ValidationError(str(exc)) from exc
        return image


class OfferForm(forms.ModelForm):
    class Meta:
        model = DemoOffer
        fields = ("key", "name", "proposed_monthly_price", "currency", "ai_daily_limit", "human_requests_per_30_days", "qa_hours", "active")


class StaffReplyForm(forms.Form):
    body = forms.CharField(max_length=2000, widget=forms.Textarea(attrs={"rows": 4}), label="Your actual reply")
    client_id = forms.UUIDField(widget=forms.HiddenInput)


@demo_portal("plants.view_careinspectionplan", methods=("GET",))
def plans(request):
    records = plan_scope(request.user).select_related("plant", "guide", "approved_by")
    return page(request, "plans", {"title": "Inspection care plans", "section": "plans", "records": paginated(request, records.order_by("start_on"))})


@demo_portal("plants.add_careinspectionplan")
def plan_create(request):
    form = InspectionForm(request.POST or None, user=request.user)
    form.instance.prepared_by = request.user
    if request.method == "POST" and form.is_valid():
        with transaction.atomic():
            plant = form.cleaned_data["plant"]
            InstallationBatch.objects.select_for_update().get(space=plant.space)
            if not prepared_plants(request.user).filter(pk=plant.pk).exists():
                raise Http404
            obj = form.save()
            company.audit(request.user, obj, "CEO QA: inspection proposal created; customer opt-in still required")
        return redirect("company:plan-review", plan_id=obj.pk)
    return page(request, "form", {"title": "Prepare an inspection plan", "section": "plans", "form": form,
        "submit": "Save unapproved proposal", "description": "Choose a verified supplied plant and its published source. Unknown identity has no invented cadence. Approval and customer opt-in are separate."})


@demo_portal("plants.view_careinspectionplan")
def plan_review(request, plan_id):
    plan = get_object_or_404(plan_scope(request.user).select_related("plant", "guide", "approved_by"), pk=plan_id)
    state = lambda: list(CareInspectionPlan.objects.filter(pk=plan.pk).values())
    form = ReviewForm(request.POST or None, initial={"review_token": company.action_token(request.user, "inspection-approve", plan, state())})
    if request.method == "POST" and form.is_valid():
        retire = request.POST.get("action") == "retire"
        company.require(request.user, "plants.change_careinspectionplan" if retire else "plants.approve_careinspectionplan")
        with transaction.atomic():
            InstallationBatch.objects.select_for_update().get(space=plan.plant.space)
            plan = CareInspectionPlan.objects.select_for_update().get(pk=plan.pk)
            if not plan_scope(request.user).filter(pk=plan.pk).exists():
                raise Http404
            company.verify_token(request.user, "inspection-approve", plan, state(), form.cleaned_data["review_token"])
            BotanicalSpecies.objects.select_for_update().get(pk=plan.guide.species_id)
            plan.guide = CareGuideVersion.objects.select_for_update().get(pk=plan.guide_id)
            if retire:
                plan.active = False
                plan.save(update_fields=("active",))
                company.audit(request.user, plan, "CEO QA: inspection proposal retired; no customer history changed")
                messages.success(request, "Inspection proposal retired. Existing observations remain; no further task can be completed from it.")
            elif not plan.approved_at:
                plan.approved_by, plan.approved_at = request.user, timezone.now()
                plan.full_clean()
                plan.save()
                company.audit(request.user, plan, "CEO QA: source/context inspection cadence explicitly approved")
            if not retire:
                messages.success(request, "Inspection plan approved. The recipient must still opt in; no watering or notification was scheduled.")
        return redirect("company:plans")
    return page(request, "plan_review", {"title": "Review inspection proposal", "section": "plans", "plan": plan, "form": form})


@demo_portal("plants.manage_service_requests", methods=("GET",))
def support(request):
    records = ServiceRequest.objects.select_related("assigned_to", "customer").order_by("-updated_at")
    return page(request, "support", {"title": "Shared service requests", "section": "support", "records": paginated(request, records)})


@demo_portal("plants.manage_service_requests")
def support_detail(request, ticket_id):
    ticket = get_object_or_404(ServiceRequest.objects.select_related("assigned_to", "customer", "conversation"), pk=ticket_id)
    form = StaffReplyForm(request.POST or None, initial={"client_id": uuid.uuid4()})
    if request.method == "POST":
        action = request.POST.get("action")
        if action in ("claim", "close"):
            with transaction.atomic():
                ticket = ServiceRequest.objects.select_for_update().get(pk=ticket.pk)
                if ticket.assigned_to_id not in (None, request.user.pk):
                    raise demo.DemoProblem("Another authorized employee has this request.")
                ticket.assigned_to = request.user
                if action == "close":
                    ticket.status = "closed"
                ticket.save(update_fields=("assigned_to", "status", "updated_at"))
                company.audit(request.user, ticket, "CEO QA: support employee claimed/closed shared request")
            return redirect("company:support-detail", ticket_id=ticket.pk)
        if form.is_valid():
            demo.support_reply(request.user, ticket.pk, form.cleaned_data["body"], form.cleaned_data["client_id"], staff=True)
            messages.success(request, "Your actual reply is stored in the existing Nova message engine and visible to this customer.")
            return redirect("company:support-detail", ticket_id=ticket.pk)
    return page(request, "support_detail", {"title": ticket.subject, "section": "support", "ticket": ticket,
        "thread": ticket.conversation.messages.select_related("sender").order_by("id")[:200], "form": form})


@demo_portal("plants.view_catalogproduct", methods=("GET",))
def catalog(request):
    return page(request, "catalog", {"title": "Fictional QA catalog", "section": "catalog",
        "records": CatalogProduct.objects.order_by("name")[:100],
        "can_bulk_import": (request.user.has_perm("plants.add_catalogproduct") and
                            os.getenv("PLANTUPIA_QA_CATALOG_ZIP_IMPORT_ENABLED", "0") == "1")})


@demo_portal("plants.change_catalogproduct")
def product_edit(request, product_id=None):
    product = get_object_or_404(CatalogProduct, pk=product_id) if product_id else None
    if not product:
        company.require(request.user, "plants.add_catalogproduct")
    form = CatalogForm(request.POST or None, request.FILES or None, instance=product)
    if request.method == "POST" and form.is_valid():
        with transaction.atomic():
            if product:
                CatalogProduct.objects.select_for_update().get(pk=product.pk)
            obj = form.save()
            company.audit(request.user, obj, "CEO QA: fictional catalog item updated; price remains provisional")
        return redirect("company:catalog")
    return page(request, "form", {"title": "Edit QA product" if product else "Add fictional QA product", "section": "catalog",
        "form": form, "multipart": True, "submit": "Save QA catalog item", "description": "Provisional test price and stock only. Upload only an actual authorized photograph; leave empty instead of inventing imagery."})


@demo_portal("plants.view_demoorder", methods=("GET",))
def orders(request):
    records = DemoOrder.objects.select_related("customer", "installation").prefetch_related("lines").order_by("-created_at")
    return page(request, "orders", {"title": "No-charge QA orders", "section": "orders", "records": paginated(request, records)})


@demo_portal("plants.view_demoorder")
def order_detail(request, order_id):
    order = get_object_or_404(DemoOrder.objects.select_related("customer", "installation").prefetch_related("lines"), pk=order_id)
    state = lambda: list(DemoOrder.objects.filter(pk=order.pk).values())
    form = ReviewForm(request.POST or None, initial={"review_token": company.action_token(request.user, "order-draft", order, state())})
    if request.method == "POST" and form.is_valid():
        with transaction.atomic():
            DemoOrder.objects.select_for_update().get(pk=order.pk)
            company.verify_token(request.user, "order-draft", order, state(), form.cleaned_data["review_token"])
            batch = demo.convert_order(request.user, order.pk)
        messages.success(request, "A fresh DRAFT installation was prepared. Identity verification, review and handover are still separate.")
        return redirect("company:installation", batch_id=batch.pk)
    return page(request, "order", {"title": "QA order " + str(order.pk)[:8], "section": "orders", "order": order, "form": form})


@demo_portal("plants.view_demoentitlement")
def access(request):
    if request.method == "POST":
        company.require(request.user, "plants.change_demoentitlement")
        with transaction.atomic():
            grant = get_object_or_404(DemoEntitlement.objects.select_for_update(), pk=request.POST.get("grant_id"))
            grant.revoked_at = timezone.now()
            grant.save(update_fields=("revoked_at",))
            company.audit(request.user, grant, "CEO QA: demo entitlement explicitly revoked")
        messages.success(request, "QA access revoked. Subsequent AI/team writes require a fresh authorized entitlement.")
        return redirect("company:demo-access")
    return page(request, "demo_access", {"title": "QA access and concept plans", "section": "access", "offers": DemoOffer.objects.all(),
        "records": paginated(request, DemoEntitlement.objects.select_related("user", "offer").order_by("-granted_at")), "now": timezone.now()})


@demo_portal("plants.change_demooffer")
def offer_edit(request, offer_key):
    offer = get_object_or_404(DemoOffer, pk=offer_key)
    form = OfferForm(request.POST or None, instance=offer)
    form.fields["key"].disabled = True
    if request.method == "POST" and form.is_valid():
        form.save()
        company.audit(request.user, offer, "CEO QA: concept plan price/quota configured; no commercial billing")
        return redirect("company:demo-access")
    return page(request, "form", {"title": "Configure concept QA plan", "section": "access", "form": form,
        "submit": "Save provisional QA plan", "description": "No payment or commercial offer. Core care and Circle remain free. QA access lasts at most 72 hours."})
