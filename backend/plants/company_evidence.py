"""Private Living Systems review workspace; no new role, record or diagnosis engine."""
import uuid
from io import BytesIO

from django.conf import settings
from django.contrib import messages
from django.core.exceptions import PermissionDenied as DjangoPermissionDenied
from django.db.models import Exists, OuterRef, Q
from django.http import Http404, HttpResponse
from django.shortcuts import get_object_or_404, redirect
from PIL import Image, ImageOps
from rest_framework.exceptions import PermissionDenied

from .company_views import page, paginated, portal
from .living_api import EvidenceReviewConflict, ReviewInput, save_evidence_review
from .models import EvidenceReview, PlantEvent, Space, SpaceMember, TracedRegion
from .vision_evidence import MAX_BYTES, MAX_PIXELS

PERMISSION = "plants.review_visual_evidence"


def eligible_evidence(user):
    # Identical underlying owner/carer/manager rule to the native review API.
    # Installation staff membership and even superuser status never imply access.
    spaces = Space.objects.filter(Q(owner=user) | Q(memberships__user=user,
        memberships__role__in=(SpaceMember.Role.CARER, SpaceMember.Role.MANAGER))).distinct()
    return (PlantEvent.objects.filter(plant__space__in=spaces, plant__is_active=True)
        .annotate(has_trace=Exists(TracedRegion.objects.filter(event_id=OuterRef("pk"))),
                  has_review=Exists(EvidenceReview.objects.filter(event_id=OuterRef("pk"))))
        .filter(Q(photo__gt="") | Q(kind=PlantEvent.Kind.ISSUE) | Q(has_trace=True))
        .select_related("plant__space"))


def require_enabled():
    if not settings.PLANTUPIA_LIVING_SYSTEMS_ENABLED:
        raise Http404


@portal(PERMISSION, methods=("GET",))
def evidence_queue(request):
    require_enabled()
    query = request.GET.get("q", "").strip()[:100]
    state = request.GET.get("status", "unreviewed")
    if state not in ("unreviewed", "reviewed", "all"):
        state = "unreviewed"
    qs = eligible_evidence(request.user)
    if query:
        qs = qs.filter(Q(plant__label__icontains=query) | Q(plant__space__name__icontains=query))
    if state != "all":
        qs = qs.filter(has_review=state == "reviewed")
    records = paginated(request, qs.order_by("-observed_at", "-created_at", "-pk"))
    return page(request, "field_evidence_queue", {"title": "Field evidence", "section": "field_evidence",
        "records": records, "query": query, "state": state})


@portal(PERMISSION, methods=("GET", "POST"))
def evidence_detail(request, event_id):
    require_enabled()
    event = get_object_or_404(eligible_evidence(request.user), pk=event_id)
    submission_id = str(uuid.uuid4())
    finding, rationale, errors, status = "", "", [], 200
    if request.method == "POST":
        submission_id = request.POST.get("submission_id", "")[:36]
        finding = request.POST.get("finding", "")[:40]
        rationale = request.POST.get("rationale", "")[:1001]
        data = ReviewInput(data=request.POST)
        if data.is_valid():
            try:
                review, created = save_evidence_review(request.user, event.plant_id, event.pk, data.validated_data)
            except PermissionDenied as exc:
                raise DjangoPermissionDenied from exc
            except EvidenceReviewConflict as exc:
                errors, status = [str(exc) + " Reload before submitting again."], 409
            else:
                messages.success(request, "Review recorded in the plant's private history." if created
                    else "This review was already recorded.")
                return redirect("company:field-evidence-detail", event_id=event.pk)
        else:
            errors, status = [f"{key}: {detail}" for key, details in data.errors.items()
                for detail in details], 400
    regions = event.traced_regions.order_by("-created_at", "-pk")
    reviews = event.evidence_reviews.order_by("-created_at", "-pk")
    return page(request, "field_evidence_detail", {"title": "Inspect a record", "section": "field_evidence",
        "event": event, "regions": list(regions[:20]), "region_count": regions.count(),
        "reviews": list(reviews[:20]), "review_count": reviews.count(),
        "submission_id": submission_id, "finding": finding, "rationale": rationale,
        "errors": errors}, status=status)


@portal(PERMISSION, methods=("GET",))
def evidence_photo(request, event_id):
    require_enabled()
    event = get_object_or_404(eligible_evidence(request.user), pk=event_id)
    if not event.photo:
        raise Http404
    # Only a bounded private preview; never expose storage paths, EXIF or direct
    # customer media URLs. Measurements remain in original recorded image units.
    try:
        with event.photo.open("rb") as source:
            data = source.read(MAX_BYTES + 1)
        if not data or len(data) > MAX_BYTES:
            raise Http404
        with Image.open(BytesIO(data)) as source:
            if (source.format not in ("JPEG", "PNG", "WEBP") or
                    source.width * source.height > MAX_PIXELS or getattr(source, "n_frames", 1) != 1):
                raise Http404
            image = ImageOps.exif_transpose(source).convert("RGB")
            image.thumbnail((1600, 1600))
            output = BytesIO()
            image.save(output, format="JPEG", quality=88)
    except (OSError, ValueError, Image.DecompressionBombError) as exc:
        raise Http404 from exc
    response = HttpResponse(output.getvalue(), content_type="image/jpeg")
    response["X-Content-Type-Options"] = "nosniff"
    return response
