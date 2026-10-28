"""Transactional CEO QA operations; no payment gateway or simulated provider."""
import hashlib
import json
import os
from datetime import timedelta
from decimal import Decimal
from uuid import UUID
from zoneinfo import ZoneInfo

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.exceptions import PermissionDenied
from django.db import transaction, IntegrityError
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.exceptions import APIException, ValidationError

from . import company_services as company
from .models import (CareInspectionPlan, CarePlanEnrollment, CarePlanDecision, DemoOffer,
                     DemoEntitlement, DemoMutation, ServiceRequest, CatalogProduct,
                     DemoCartLine, DemoOrder, DemoOrderLine, PlantEvent, PlantUnit,
                     InstallationBatch, InstallationManifestRow, Space)


class DemoProblem(APIException):
    def __init__(self, message, status=409, code="needs_review"):
        self.status_code = status
        super().__init__({"detail": message, "code": code})


def enabled():
    return settings.PLANTUPIA_CEO_DEMO_ENABLED and company.enabled()


def require_demo():
    if not enabled():
        raise DemoProblem("This isolated QA feature is not enabled.", 404, "demo_disabled")


def customer(user):
    if user.is_staff:
        raise PermissionDenied("Use a separate nonstaff customer account for the QA journey.")


def signature(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(",", ":"), default=str).encode()).hexdigest()


def _mutate(user, client_id, operation, payload, create):
    """Serialize all demo mutations per actor; receipt and resource commit together."""
    try:
        identity = UUID(str(client_id))
    except (ValueError, TypeError, AttributeError):
        raise ValidationError("A stable client_id UUID is required for this action.")
    fingerprint = signature(payload)
    with transaction.atomic():
        get_user_model().objects.select_for_update().get(pk=user.pk)
        receipt = DemoMutation.objects.filter(actor=user, client_id=identity).first()
        if receipt:
            if receipt.operation != operation or receipt.fingerprint != fingerprint:
                raise DemoProblem("This retry identity belongs to different content. Retry the original action.")
            return receipt.response
        response = create()
        DemoMutation.objects.create(actor=user, client_id=identity, operation=operation, fingerprint=fingerprint, response=response)
        return response


def mutate(user, client_id, operation, payload, create):
    try:
        return _mutate(user, client_id, operation, payload, create)
    except IntegrityError as exc:
        raise DemoProblem("This action conflicts with an existing record. No partial change was committed.") from exc


def entitlement(user):
    return (DemoEntitlement.objects.filter(user=user, revoked_at__isnull=True,
            expires_at__gt=timezone.now(), offer__active=True).select_related("offer")
            .order_by("-granted_at").first()) if enabled() else None


def require_access(user, human=False):
    access = entitlement(user)
    if not access or (human and access.offer.human_requests_per_30_days < 1):
        raise DemoProblem("Choose QA Expert Care access for team support." if human else "Choose a QA care plan to use AI.", 402, "entitlement_required")
    return access


def offer_json(offer):
    return {"key": offer.key, "name": offer.name, "price": str(offer.proposed_monthly_price),
            "currency": offer.currency, "ai_daily_limit": offer.ai_daily_limit,
            "human_limit": offer.human_requests_per_30_days, "qa_hours": offer.qa_hours}


def access_json(user):
    access = entitlement(user)
    return {"qa_only": True, "notice": "QA demonstration — no payment / not a commercial offer",
            "ai_configured": bool(os.getenv("PLANTUPIA_AI_ENABLED") == "1" and os.getenv("PLANTUPIA_AI_API_KEY")
                                  and settings.REDIS_URL),
            "entitled": bool(access), "human_access": bool(access and access.offer.human_requests_per_30_days),
            "expires_at": access.expires_at.isoformat() if access else None,
            "offer": offer_json(access.offer) if access else None,
            "offers": [offer_json(o) for o in DemoOffer.objects.filter(active=True).order_by("proposed_monthly_price")]}


def activate(user, offer_key, client_id):
    require_demo()
    customer(user)
    def create():
        offer = get_object_or_404(DemoOffer.objects.select_for_update(), pk=offer_key, active=True)
        offer.full_clean()
        DemoEntitlement.objects.filter(user=user, revoked_at__isnull=True).update(revoked_at=timezone.now())
        grant = DemoEntitlement.objects.create(user=user, offer=offer, expires_at=timezone.now() + timedelta(hours=offer.qa_hours))
        company.audit(user, grant, "CEO QA: short-lived no-charge demo access explicitly activated")
        return {"id": str(grant.pk), "qa_only": True, "charged": False}
    return mutate(user, client_id, "activate", {"offer": offer_key}, create)


def plan_valid(plan):
    return bool(plan.active and plan.approved_at and plan.guide.published and plan.plant.is_active
                and plan.plant.verified_species_id == plan.guide.species_id)


def plan_json(plan, user):
    enrollment = CarePlanEnrollment.objects.filter(plan=plan, responsible=user).first()
    today = timezone.now().astimezone(ZoneInfo(plan.timezone)).date()
    return {"id": str(plan.pk), "plant_id": str(plan.plant_id), "plant_name": plan.plant.label,
            "action": plan.get_action_display(), "instructions": plan.instructions,
            "every_days": plan.every_days, "timezone": plan.timezone,
            "placement_context": plan.placement_context, "guide_version": plan.guide.version,
            "reviewer": plan.approved_by.username, "source_url": plan.guide.source_url,
            "enabled": bool(enrollment and enrollment.enabled),
            "revision": enrollment.revision if enrollment else 0,
            "next_due_on": str(enrollment.next_due_on) if enrollment else str(max(plan.start_on, today)),
            "due": bool(enrollment and enrollment.enabled and enrollment.next_due_on <= today),
            "valid": plan_valid(plan)}


def plan_action(user, plan_id, data):
    from .views import accessible_plants, can_record
    plan = get_object_or_404(CareInspectionPlan, pk=plan_id, approved_at__isnull=False,
                            plant__in=accessible_plants(user))
    if not can_record(user, plan.plant):
        raise PermissionDenied
    payload = {k: v for k, v in data.items() if k != "client_id"}
    def create():
        locked = CareInspectionPlan.objects.select_for_update(of=("self",)).select_related("plant__space", "guide", "approved_by").get(pk=plan.pk)
        Space.objects.select_for_update().get(pk=locked.plant.space_id)
        if not accessible_plants(user).filter(pk=locked.plant_id).exists() or not can_record(user, locked.plant):
            raise PermissionDenied
        if not plan_valid(locked):
            raise DemoProblem("This plan is retired or its source/identity changed. It cannot schedule care.")
        today = timezone.now().astimezone(ZoneInfo(locked.timezone)).date()
        enrollment, _ = CarePlanEnrollment.objects.select_for_update().get_or_create(plan=locked, responsible=user,
            defaults={"next_due_on": max(locked.start_on, today)})
        if data["revision"] != enrollment.revision:
            raise DemoProblem("The inspection changed. Reload its current occurrence before confirming.")
        action = data["action"]
        old_due = enrollment.next_due_on
        event = None
        if action == "opt_in":
            enrollment.enabled = True
            enrollment.next_due_on = max(locked.start_on, today)
        elif action == "decline":
            enrollment.enabled = False
        else:
            if not enrollment.enabled:
                raise DemoProblem("Opt into this inspection plan first.")
            if action == "complete":
                if not data.get("note", "").strip():
                    raise ValidationError("Record what you actually observed; no watering is inferred.")
                event = PlantEvent.objects.create(plant=locked.plant, actor=user, kind="observation", evidence_type="observed",
                    note=data["note"].strip(), observed_at=timezone.now())
            if action == "reschedule":
                enrollment.next_due_on = data["next_due_on"]
                if enrollment.next_due_on < today or enrollment.next_due_on > today + timedelta(days=365):
                    raise ValidationError("Choose today through one year ahead.")
            else:
                # Skip/completion advances from today/previous due without generating missed tasks.
                enrollment.next_due_on = max(today, old_due) + timedelta(days=locked.every_days)
        enrollment.revision += 1
        enrollment.save()
        CarePlanDecision.objects.create(enrollment=enrollment, action=action, due_on=old_due,
            next_due_on=enrollment.next_due_on, event=event)
        return {"plan_id": str(locked.pk), "event_id": str(event.pk) if event else None,
                "revision": enrollment.revision, "next_due_on": str(enrollment.next_due_on)}
    return mutate(user, data["client_id"], "inspection:" + str(plan_id), payload, create)


def create_request(user, data):
    from accounts.models import Conversation, Message
    from .views import accessible_plants
    customer(user)
    plant = get_object_or_404(accessible_plants(user), pk=data["plant_id"])
    if data.get("share_context") is not True:
        raise ValidationError("Explicitly agree to share this request and the selected plant label with support staff.")
    payload = {k: v for k, v in data.items() if k != "client_id"}
    def create():
        access = require_access(user, human=True)
        used = ServiceRequest.objects.filter(customer=user, created_at__gte=timezone.now() - timedelta(days=30)).count()
        if used >= access.offer.human_requests_per_30_days:
            raise DemoProblem("The provisional QA support request limit has been reached.", 429, "support_limit")
        conversation = Conversation.objects.create(kind="group", title="Private Plantupia service request")
        ticket = ServiceRequest.objects.create(customer=user, plant=plant, conversation=conversation,
            subject=data["subject"], shared_plant_label=plant.label,
            shared_context=plant.placement_note if data.get("share_placement") else "")
        Message.objects.create(conversation=conversation, sender=user, body=data["body"], client_id="support:" + str(ticket.pk))
        company.audit(user, ticket, "CEO QA: customer explicitly shared a scoped support request")
        return {"id": str(ticket.pk)}
    return mutate(user, data["client_id"], "support:create", payload, create)


def support_reply(user, ticket_id, body, client_id, staff=False):
    from accounts.models import Message
    if staff:
        company.require(user, "plants.manage_service_requests")
    ticket = get_object_or_404(ServiceRequest, pk=ticket_id, **({} if staff else {"customer": user}))
    def create():
        locked = ServiceRequest.objects.select_for_update().get(pk=ticket.pk)
        if staff:
            if locked.assigned_to_id != user.pk:
                raise PermissionDenied("Claim this request before replying.")
        else:
            require_access(user, human=True)
        if locked.status == "closed":
            raise DemoProblem("This request is closed. No reply was sent.")
        message = Message.objects.create(conversation=locked.conversation, sender=user,
            body=body, client_id="service:" + str(client_id))
        locked.status = "replied" if staff else "open"
        locked.save(update_fields=("status", "updated_at"))
        company.audit(user, locked, "CEO QA: actual staff reply persisted" if staff else "CEO QA: customer follow-up persisted")
        if staff:
            from accounts.models import Notification
            Notification.objects.get_or_create(dedupe_key=f"plant-team:{message.pk}", defaults={
                "recipient_id": locked.customer_id, "actor": user, "kind": "plant_team",
                "plantupia_ticket_id": locked.pk, "plantupia_plant_id": locked.plant_id,
            })
            # Only a real employee response triggers a customer notification.
            # Idempotent mutation receipts prevent duplicate push on retries.
            from accounts.push import send_plantupia_support_reply_push
            transaction.on_commit(lambda: send_plantupia_support_reply_push(
                recipient_id=locked.customer_id,
                ticket_id=locked.pk,
                plant_id=locked.plant_id,
                message_id=message.pk,
            ))
        return {"id": str(locked.pk), "message_id": message.pk}
    return mutate(user, client_id, "support:reply:" + str(ticket.pk), {"body": body, "staff": staff}, create)


def product_json(product):
    return {"id": str(product.pk), "name": product.name, "description": product.description,
            "kind": product.kind, "price": str(product.proposed_price), "currency": product.currency,
            "qa_stock": product.qa_stock, "has_image": bool(product.image), "qa_only": True}


def cart_json(user):
    lines = list(DemoCartLine.objects.filter(customer=user).select_related("product").order_by("product_id"))
    state = [{"product_id": str(l.product_id), "quantity": l.quantity,
              "price": str(l.product.proposed_price), "active": l.product.active} for l in lines]
    return {"results": [{"product": product_json(l.product), "quantity": l.quantity} for l in lines],
            "fingerprint": signature(state), "total_provisional": str(sum((l.quantity*l.product.proposed_price for l in lines), Decimal("0.00"))),
            "notice": "Demo order — no charge or delivery initiated"}


def checkout(user, data):
    customer(user)
    def create():
        cart = cart_json(user)
        if cart["fingerprint"] != data["cart_fingerprint"]:
            raise DemoProblem("Cart or provisional prices changed. Review the current total first.")
        lines = list(DemoCartLine.objects.filter(customer=user).order_by("product_id"))
        if not lines or len(lines) > 20:
            raise ValidationError("Choose between one and twenty different QA products.")
        products = {p.pk: p for p in CatalogProduct.objects.select_for_update().filter(pk__in=[l.product_id for l in lines]).order_by("pk")}
        # Recheck prices after the product locks: a stale review cannot accept new prices.
        if cart_json(user)["fingerprint"] != data["cart_fingerprint"]:
            raise DemoProblem("Provisional product prices changed. Review the cart again.")
        total = Decimal("0.00")
        for line in lines:
            product = products[line.product_id]
            if not product.active or product.qa_stock < line.quantity:
                raise DemoProblem("A QA item is unavailable or its test stock changed. No order was created.")
            total += product.proposed_price * line.quantity
        order = DemoOrder.objects.create(customer=user, total_provisional=total)
        for line in lines:
            product = products[line.product_id]
            product.qa_stock -= line.quantity
            product.save(update_fields=("qa_stock",))
            DemoOrderLine.objects.create(order=order, product=product, name_snapshot=product.name,
                kind_snapshot=product.kind, unit_price_provisional=product.proposed_price, quantity=line.quantity)
        DemoCartLine.objects.filter(customer=user).delete()
        company.audit(user, order, "CEO QA: noncommercial order persisted; no payment or delivery")
        return {"id": str(order.pk), "charged": False, "delivery_initiated": False}
    return mutate(user, data["client_id"], "checkout", {"cart_fingerprint": data["cart_fingerprint"]}, create)


@transaction.atomic
def convert_order(user, order_id):
    company.require(user, "plants.change_demoorder", "plants.add_installationbatch", "plants.add_space", "plants.add_plantunit", "plants.add_installationmanifestrow")
    order = DemoOrder.objects.select_for_update().get(pk=order_id)
    if order.installation_id:
        return order.installation
    plant_lines = list(order.lines.filter(kind_snapshot="plant"))
    if not plant_lines:
        raise DemoProblem("Only an order containing plant units can prepare an installation.")
    space = Space.objects.create(owner=user, name="FICTIONAL QA order " + str(order.pk)[:8])
    batch = InstallationBatch.objects.create(space=space, prepared_by=user, reference_code="QA-ORDER-" + str(order.pk))
    for line in plant_lines:
        for number in range(line.quantity):
            plant = PlantUnit.objects.create(space=space, label=line.name_snapshot, species_label="Unverified QA catalog item")
            InstallationManifestRow.objects.create(batch=batch, plant=plant, source_key=f"order-{line.pk}-{number + 1}")
    order.installation, order.status = batch, "draft_prepared"
    order.save(update_fields=("installation", "status"))
    company.audit(user, order, "CEO QA: explicit conversion to fresh DRAFT; no handover or delivery")
    from accounts.models import Notification
    notice, created = Notification.objects.get_or_create(dedupe_key=f"plant-order:{order.pk}:draft", defaults={
        "recipient_id": order.customer_id, "actor": user, "kind": "plant_order", "plantupia_order_id": order.pk,
    })
    if created:
        from accounts.push import send_plantupia_order_update_push
        transaction.on_commit(lambda: send_plantupia_order_update_push(recipient_id=order.customer_id, order_id=order.pk, notification_id=notice.pk))
    return batch
