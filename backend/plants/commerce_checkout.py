"""A disabled-by-default QA intake for B2C orders, not a real checkout or collection API."""
import hashlib
import json
import os
import uuid
from decimal import Decimal

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.exceptions import PermissionDenied, ValidationError
from django.db import transaction
from django.http import Http404
from django.shortcuts import get_object_or_404
from django.utils import timezone

from . import company_services as company, commerce_services
from .commerce_models import CommerceSKU, CommerceOrderRequest, CommerceOrderRequestLine


def enabled():
    # The owner must deliberately enable QA intake on an isolated environment.
    # Production sales/payment/dispatch are NOT enabled by this flag.
    return (os.getenv("PLANTUPIA_COMMERCE_QA_REQUESTS_ENABLED", "0") == "1"
            and settings.PLANTUPIA_CEO_DEMO_ENABLED and company.enabled())


def require_qa():
    if not enabled():
        raise Http404("Commercial order requests are unavailable.")


def _fingerprint(data):
    normalized = {
        "contact_name": data["contact_name"].strip(),
        "contact_phone": data["contact_phone"].strip(),
        "city": data["city"].strip(),
        "delivery_address": data["delivery_address"].strip(),
        "customer_note": data.get("customer_note", "").strip(),
        "payment_preference": data["payment_preference"],
        "lines": sorted(
            [{"sku_id": str(line["sku_id"]), "quantity": int(line["quantity"])}
             for line in data["lines"]], key=lambda line: line["sku_id"],
        ),
    }
    return hashlib.sha256(json.dumps(normalized, sort_keys=True, ensure_ascii=False).encode("utf-8")).hexdigest()


def customer_order_json(order):
    """Only call after scoped account permission check."""
    return {
        "id": str(order.pk), "status": order.status,
        "created_at": order.created_at.isoformat(),
        "payment_preference": order.payment_preference,
        "payment_status": "not_collected",
        "charged": False, "delivery_initiated": False,
        "customer": {
            "name": order.contact_name, "phone": order.contact_phone,
            "city": order.city, "address": order.delivery_address,
        },
        "provisional_subtotal": str(order.provisional_subtotal),
        "shipping_fee": None, "final_total": None,
        "review_note": order.review_note if order.status != order.Status.SUBMITTED else "",
        "lines": [
            {"sku": line.sku_code_snapshot, "name": line.product_name_snapshot,
             "variant": line.option_snapshot, "quantity": line.quantity,
             "unit_price": str(line.unit_price_snapshot),
             "subtotal": str(line.line_subtotal_snapshot)}
            for line in order.lines.all()
        ],
        "notice": (
            "QA request only: no merchant acceptance, confirmed shipping, "
            "payment transfer instructions, dispatch or plant handover."
        ),
    }


def submit_qa_request(user, data):
    require_qa()
    if user.is_staff or not user.is_active:
        raise PermissionDenied("Use a separate active nonstaff customer account.")
    if not data.get("confirm_qa_no_payment") is True:
        raise ValidationError("Confirm that this QA request is not a purchase or payment.")

    lines = data["lines"]
    if not 1 <= len(lines) <= 20:
        raise ValidationError("A QA request requires 1–20 SKU lines.")
    identities = [str(line["sku_id"]) for line in lines]
    if len(identities) != len(set(identities)):
        raise ValidationError("Each exact SKU may appear only once in a request.")
    if any(line["quantity"] < 1 or line["quantity"] > 10 for line in lines):
        raise ValidationError("Select 1–10 of each exact SKU.")
    fingerprint = _fingerprint(data)
    client_id = uuid.UUID(str(data["client_id"]))

    with transaction.atomic():
        # Serializes same-account retry and reads of SKU/current counted stock.
        get_user_model().objects.select_for_update().get(pk=user.pk)
        previous = CommerceOrderRequest.objects.filter(customer=user, client_id=client_id).first()
        if previous:
            if previous.fingerprint != fingerprint:
                raise ValidationError("This retry ID belongs to a different request.")
            return previous

        skus = {str(sku.pk): sku for sku in CommerceSKU.objects
                .filter(pk__in=identities).select_related("product").select_for_update().order_by("pk")}
        if len(skus) != len(identities):
            raise ValidationError("One of the selected SKU options no longer exists.")
        total = Decimal("0.00")
        snapshots = []
        for line in lines:
            sku = skus[str(line["sku_id"])]
            quantity = int(line["quantity"])
            if sku.currency != "EGP":
                raise ValidationError("Mixed currencies are not supported in QA.")
            if commerce_services.stock_balance(sku.pk) < quantity:
                raise ValidationError("A requested SKU lacks counted QA stock; ask staff to review.")
            subtotal = sku.price * quantity
            total += subtotal
            snapshots.append({
                "sku": sku, "product_name_snapshot": sku.product.name,
                "sku_code_snapshot": sku.code, "option_snapshot": sku.option_label,
                "unit_price_snapshot": sku.price, "quantity": quantity,
                "line_subtotal_snapshot": subtotal,
            })

        order = CommerceOrderRequest(
            customer=user, client_id=client_id, fingerprint=fingerprint,
            contact_name=data["contact_name"].strip(),
            contact_phone=data["contact_phone"].strip(),
            city=data["city"].strip(),
            delivery_address=data["delivery_address"].strip(),
            customer_note=data.get("customer_note", "").strip(),
            payment_preference=data["payment_preference"],
            provisional_subtotal=total,
        )
        order.full_clean()
        order.save()
        CommerceOrderRequestLine.objects.bulk_create([
            CommerceOrderRequestLine(order=order, **snapshot) for snapshot in snapshots
        ])
        company.audit(user, order, "New B2C QA request; no charge, stock reservation or delivery")
        return order


def list_customer_requests(user):
    require_qa()
    if user.is_staff or not user.is_active:
        raise PermissionDenied
    return CommerceOrderRequest.objects.filter(customer=user).prefetch_related("lines").order_by("-created_at")[:100]


def get_customer_request(user, order_id):
    require_qa()
    if user.is_staff or not user.is_active:
        raise PermissionDenied
    return get_object_or_404(
        CommerceOrderRequest.objects.prefetch_related("lines"),
        pk=order_id, customer=user,
    )


def review_state(order):
    return {
        "order_id": str(order.pk),
        "status": order.status,
        "reviewed_at": order.reviewed_at.isoformat() if order.reviewed_at else None,
        "subtotal": str(order.provisional_subtotal),
        "lines": list(order.lines.order_by("pk").values(
            "sku_code_snapshot", "quantity", "unit_price_snapshot", "line_subtotal_snapshot",
        )),
    }


@transaction.atomic
def review_qa_request(*, staff, order_id, action, review_note, review_token):
    require_qa()
    company.require(staff, "plants.change_commerceorderrequest")
    if action not in ("review", "decline"):
        raise ValidationError("Choose staff QA review or decline.")
    note = (review_note or "").strip()
    if len(note) > 500:
        raise ValidationError("Review note exceeds 500 characters.")
    order = get_object_or_404(
        CommerceOrderRequest.objects.select_for_update().prefetch_related("lines"),
        pk=order_id,
    )
    if order.status != order.Status.SUBMITTED:
        raise ValidationError("This QA request was already reviewed. Refresh the page.")
    company.verify_token(staff, "commerce-qa-review", order, review_state(order), review_token)
    order.status = (order.Status.REVIEWED if action == "review" else order.Status.DECLINED)
    order.review_note = note
    order.reviewed_by = staff
    order.reviewed_at = timezone.now()
    order.full_clean()
    order.save(update_fields=("status", "review_note", "reviewed_by", "reviewed_at"))
    company.audit(staff, order, "Reviewed noncommercial QA request; no buyer payment or dispatch")
    return order
