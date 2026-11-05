"""Transactional submission receipts. Blob names are stable even after DB rollback/crash."""
from datetime import timezone as datetime_timezone
import hashlib
import json
import uuid

from django.db import transaction
from django.core.serializers.json import DjangoJSONEncoder
from rest_framework import serializers
from rest_framework.response import Response

from .models import PlantSubmission


class RejectedSubmission(Exception):
    def __init__(self, response):
        self.response = response


def photo_digest(upload):
    if upload is None:
        return None
    if upload.size > 10 * 1024 * 1024:
        raise serializers.ValidationError({"photo": "Choose a photo smaller than 10 MB."})
    digest = hashlib.sha256()
    upload.seek(0)
    for chunk in upload.chunks():
        digest.update(chunk)
    upload.seek(0)
    return digest.hexdigest()


def fingerprint(request, operation, plant):
    data = request.data
    if operation == "event":
        when = serializers.DateTimeField().run_validation(data.get("observed_at"))
        fields = {
            "kind": str(data.get("kind", "")),
            "evidence_type": str(data.get("evidence_type", "observed")),
            "note": str(data.get("note", "")).strip(),
            "observed_at": when.astimezone(datetime_timezone.utc).isoformat(),
            "follow_up_on": str(data.get("follow_up_on") or ""),
            "follow_up_for": str(data.get("follow_up_for") or ""),
        }
        photo = request.FILES.get("photo")
        if data.get("photo") and photo is None:
            raise serializers.ValidationError({"photo": "Upload the exact selected photo."})
    else:
        fields = {"public_topic": str(data.get("public_topic", "")).strip(),
                  "caption": str(data.get("caption", "")).strip(), "public_share_consent": True,
                  "media_type": str(data.get("media_type", "image"))}
        photo = request.FILES.get("image")
    body = dict(operation=operation, plant=str(plant.pk), fields=fields, photo_sha256=photo_digest(photo))
    return hashlib.sha256(json.dumps(body, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def save_submission_photo(receipt, storage, upload, prefix):
    """Winner alone writes; same bytes after a rolled-back transaction reuse the same blob.

    Never trust filenames. Public callers must pass sanitized bytes. Content and actor
    are part of the path, so a conflicting/unrelated submission cannot overwrite a photo.
    """
    digest = photo_digest(upload)
    suffix = {"image/png": "png", "image/webp": "webp"}.get(upload.content_type, "jpg")
    # Both private events and Nova Post.image retain their existing 100-character
    # columns. Hash the complete ownership tuple rather than embedding long UUIDs.
    owned_digest = hashlib.sha256(f"{receipt.actor_id}:{receipt.client_submission_id}:{digest}".encode()).hexdigest()
    name = f"{prefix}/s/{owned_digest}.{suffix}"
    if not storage.exists(name):
        saved = storage.save(name, upload)
        if saved != name:
            # Defensive handling for storage backends returning an alternate collision name.
            if not storage.exists(name):
                storage.delete(saved)
                raise RuntimeError("Storage did not preserve the immutable submission name.")
            storage.delete(saved)
    return name


def submit(request, plant, operation, create, required=False):
    raw = str(request.data.get("client_submission_id") or "").strip()
    if not raw:
        if required:
            return Response({"detail": "A submission identity is required. Update the app and retry."}, status=400)
        # Keep legacy Post + private discussion linkage atomic as before.
        with transaction.atomic():
            return create(None)  # Compatibility for earlier Plantupia clients only.
    try:
        identity = uuid.UUID(raw)
    except ValueError:
        return Response({"detail": "Invalid submission identity."}, status=400)
    signature = fingerprint(request, operation, plant)
    try:
        with transaction.atomic():
            # get_or_create waits on the unique key: concurrent losers see the committed
            # original receipt. Resource + receipt commit/rollback together.
            receipt, created = PlantSubmission.objects.get_or_create(
                actor=request.user, client_submission_id=identity,
                defaults={"operation": operation, "plant_id": plant.pk, "fingerprint": signature},
            )
            if not created:
                if receipt.operation != operation or receipt.plant_id != plant.pk or receipt.fingerprint != signature:
                    return Response({"detail": "This submission identity belongs to different content or a different plant. Retry the original submission."}, status=409)
                return Response(receipt.response, status=200 if receipt.status_code == 201 else receipt.status_code)
            try:
                with transaction.atomic():
                    response = create(receipt)
                    if response.status_code >= 400:
                        raise RejectedSubmission(response)
            except serializers.ValidationError as error:
                response = Response(error.detail, status=400)
            except RejectedSubmission as error:
                if error.response.status_code != 400:
                    raise
                response = error.response
            body = json.loads(json.dumps(dict(response.data, client_submission_id=str(identity)), cls=DjangoJSONEncoder))
            if response.status_code == 400:
                # A durable rejection binds this identity too. Another request cannot
                # race the rejection with changed content and commit behind the client.
                body.update(code="submission_rejected", request_id=str(identity))
            receipt.response = body
            receipt.status_code = response.status_code
            receipt.save(update_fields=("response", "status_code"))
            return Response(body, status=response.status_code)
    except RejectedSubmission as rejected:
        return rejected.response
