"""Token-authenticated website ingress and permission-scoped company inbox."""
import hashlib
import hmac
import json
import os

from django import forms
from django.core.cache import cache
from django.core.exceptions import RequestDataTooBig
from django.db import transaction
from django.http import JsonResponse
from django.shortcuts import get_object_or_404, redirect
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.debug import sensitive_post_parameters
from django.views.decorators.http import require_POST

from .company_views import page, paginated, portal
from .company_services import audit
from .website_inbox_models import WebsiteEnquiry


class EnquiryForm(forms.Form):
    submission_id = forms.UUIDField()
    topic = forms.ChoiceField(choices=[(x, x) for x in ("project", "care", "general")])
    name = forms.CharField(max_length=100)
    email = forms.EmailField(max_length=160)
    organization = forms.CharField(max_length=120, required=False)
    environment = forms.ChoiceField(required=False, choices=[(x, x) for x in ("", "home", "work", "gather", "industrial", "retail", "public", "other")])
    location = forms.CharField(max_length=100, required=False)
    message = forms.CharField(max_length=2000)

    def clean(self):
        data = super().clean()
        if data.get("topic") != "project":
            data["environment"] = ""
        if data.get("topic") == "general":
            data["location"] = ""
        return data


def reply(data, status):
    response = JsonResponse(data, status=status)
    response["Cache-Control"] = "no-store"
    return response


def allowed(key, limit, window):
    if cache.add(key, 1, timeout=window):
        return True
    try:
        return cache.incr(key) <= limit
    except ValueError:
        return cache.add(key, 1, timeout=window)


@csrf_exempt
@sensitive_post_parameters()
@require_POST
def receive_website_enquiry(request):
    # No session-cookie authentication, no public GET, and no anonymous direct DB writes.
    secret = os.getenv("PLANTUPIA_WEBSITE_INBOX_TOKEN", "")
    if os.getenv("PLANTUPIA_WEBSITE_INBOX_ENABLED") != "1" or len(secret) < 32:
        return reply({"error": "unavailable"}, 503)
    auth = request.headers.get("Authorization", "")
    if not hmac.compare_digest(auth.encode("utf-8"), ("Bearer " + secret).encode("utf-8")):
        return reply({"error": "unauthorized"}, 401)
    try:
        body = request.body
        if len(body) > 16384 or request.content_type != "application/json":
            return reply({"error": "invalid_request"}, 400)
        data = json.loads(body)
    except (ValueError, UnicodeError, RequestDataTooBig):
        return reply({"error": "invalid_request"}, 400)
    if not isinstance(data, dict):
        return reply({"error": "invalid_request"}, 400)
    form = EnquiryForm(data)
    if not form.is_valid():
        return reply({"error": "invalid_fields"}, 400)
    fields = dict(form.cleaned_data)
    submission_id = fields.pop("submission_id")
    fingerprint = hashlib.sha256(json.dumps(fields, sort_keys=True).encode()).hexdigest()
    existing = WebsiteEnquiry.objects.filter(submission_id=submission_id).first()
    if existing:
        if existing.fingerprint != fingerprint:
            return reply({"error": "conflicting_submission"}, 409)
        return reply({"reference": str(existing.id)}, 200)
    email_key = hmac.new(secret.encode(), fields["email"].casefold().encode(), hashlib.sha256).hexdigest()
    if not allowed("website-inbox:global", 120, 60) or not allowed("website-inbox:email:" + email_key, 5, 900):
        return reply({"error": "rate_limited"}, 429)
    with transaction.atomic():
        record, created = WebsiteEnquiry.objects.get_or_create(
            submission_id=submission_id, defaults={**fields, "fingerprint": fingerprint}
        )
    if record.fingerprint != fingerprint:
        return reply({"error": "conflicting_submission"}, 409)
    return reply({"reference": str(record.id)}, 201 if created else 200)


@portal("plants.view_websiteenquiry", methods=("GET",))
def website_inbox(request):
    records = WebsiteEnquiry.objects.all()
    topic, status = request.GET.get("topic", ""), request.GET.get("status", "")
    if topic in ("project", "care", "general"):
        records = records.filter(topic=topic)
    if status in ("new", "reviewed", "closed"):
        records = records.filter(status=status)
    return page(request, "website_inbox", {"title": "Website enquiries", "section": "website_inbox", "records": paginated(request, records), "topic_filter": topic, "status_filter": status})


@portal("plants.view_websiteenquiry", methods=("GET",))
def website_enquiry(request, enquiry_id):
    record = get_object_or_404(WebsiteEnquiry, pk=enquiry_id)
    return page(request, "website_enquiry", {"title": "Website enquiry", "section": "website_inbox", "record": record})


@portal("plants.view_websiteenquiry", "plants.change_websiteenquiry", methods=("POST",))
@transaction.atomic
def website_enquiry_status(request, enquiry_id):
    record = get_object_or_404(WebsiteEnquiry.objects.select_for_update(), pk=enquiry_id)
    status = request.POST.get("status", "")
    if status in ("new", "reviewed", "closed") and status != record.status:
        record.status = status
        record.reviewed_by = request.user
        record.reviewed_at = timezone.now()
        record.save(update_fields=("status", "reviewed_by", "reviewed_at"))
        audit(request.user, record, "Website enquiry status changed to " + status)
    return redirect("company:website-enquiry", enquiry_id=record.id)
