"""Two truthful entry points over existing Company permissions; no placeholder business data."""
from django.db.models import Count

from . import company_services as company
from .company_views import page, portal
from .commerce_checkout import enabled as qa_orders_enabled
from .commerce_models import CommerceBusinessRFQ
from .commerce_models import CommerceProduct, CommerceSKU, CommerceStockMovement, CommerceOrderRequest, CommerceOrder


@portal(methods=("GET",))
def projects_home(request):
    batches = company.batches(request.user)
    counts = {
        "draft": batches.filter(status="draft").count(),
        "ready": batches.filter(status="ready").count(),
        "claimed": batches.filter(status="claimed").count(),
    }
    return page(request, "operations_projects", {
        "title": "B2B / Projects", "section": "projects_home",
        "counts": counts,
        "recent": batches.select_related("space").order_by("-created_at")[:5],
        "rfq_new_count": CommerceBusinessRFQ.objects.filter(
            staff_first_seen_at__isnull=True).count()
            if qa_orders_enabled() and request.user.has_perm("plants.view_commercebusinessrfq") else None,
    })


@portal(methods=("GET",))
def commerce_home(request):
    can_products = request.user.has_perm("plants.view_commerceproduct")
    can_stock = request.user.has_perm("plants.view_commercestockmovement")
    can_requests = request.user.has_perm("plants.view_commerceorder")
    can_old_qa_requests = request.user.has_perm("plants.view_commerceorderrequest") and qa_orders_enabled()
    counts = {
        "product_drafts": CommerceProduct.objects.count() if can_products else None,
        "sku_options": CommerceSKU.objects.count() if can_products else None,
        "stock_movements": CommerceStockMovement.objects.count() if can_stock else None,
        "real_orders": CommerceOrder.objects.count() if can_requests else None,
        "quote_pending_orders": CommerceOrder.objects.filter(
            status=CommerceOrder.Status.QUOTE_PENDING).count() if can_requests else None,
        "quoted_orders": CommerceOrder.objects.filter(
            status=CommerceOrder.Status.QUOTED).count() if can_requests else None,
        "confirmed_orders": CommerceOrder.objects.filter(
            status=CommerceOrder.Status.CONFIRMED).count() if can_requests else None,
        "qa_requests": CommerceOrderRequest.objects.count() if can_old_qa_requests else None,
        "unreviewed_requests": CommerceOrderRequest.objects.filter(status="qa_submitted").count()
            if can_old_qa_requests else None,
    }
    return page(request, "operations_commerce", {
        "title": "B2C / Shop & Orders", "section": "commerce_home",
        "counts": counts, "can_products": can_products, "can_stock": can_stock,
        "can_requests": can_requests, "commerce_qa_requests_enabled": qa_orders_enabled(),
    })


@portal(methods=("GET",))
def care_home(request):
    """Single entry into real care/content/support operations.

    Do not imply there is an AI diagnostic, paid membership or an approved
    field service schedule where none exists.
    """
    return page(request, "operations_care", {
        "title": "Care & Support", "section": "care_home",
    })
