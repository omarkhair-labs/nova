"""Additional private views of one recorded visit, served only after checking membership."""
from base64 import b64encode
from hashlib import sha256
from io import BytesIO

from django.db import transaction
from django.shortcuts import get_object_or_404
from PIL import Image
from rest_framework import serializers
from rest_framework.exceptions import PermissionDenied
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response

from .living_api import ResearchView
from .living_vision import _open
from .models import PlantEvent, VisitPhotograph
from .views import accessible_plants, can_record
from .vision_evidence import MAX_BYTES


class VisitPhotoInput(serializers.Serializer):
    submission_id = serializers.UUIDField()
    part = serializers.ChoiceField(choices=["whole", "leaf", "stem", "flower", "root", "soil"])
    photo = serializers.FileField()


class VisitPhotoView(ResearchView):
    parser_classes = [MultiPartParser, FormParser]

    def get(self, request, plant_id, event_id, photo_id=None):
        if photo_id is None:
            return Response(status=405)
        photo = get_object_or_404(VisitPhotograph, pk=photo_id, event_id=event_id, event__plant_id=plant_id,
                                  event__plant__in=accessible_plants(request.user))
        return Response({"id": photo.pk, "part": photo.part, "jpeg": b64encode(photo.jpeg).decode("ascii")})

    def post(self, request, plant_id, event_id, photo_id=None):
        if photo_id is not None:
            return Response(status=405)
        event = get_object_or_404(PlantEvent, pk=event_id, plant_id=plant_id, plant__in=accessible_plants(request.user))
        if not can_record(request.user, event.plant):
            raise PermissionDenied
        data = VisitPhotoInput(data=request.data)
        data.is_valid(raise_exception=True)
        upload = data.validated_data["photo"]
        if upload.size > MAX_BYTES:
            raise serializers.ValidationError("Choose a smaller photograph.")
        raw = upload.read(MAX_BYTES + 1)
        digest = sha256(raw).hexdigest()
        try:
            image = _open(raw, {"JPEG", "PNG", "WEBP"}).convert("RGB")
            image.thumbnail((1280, 1280), Image.Resampling.LANCZOS)
            preview = BytesIO()
            image.save(preview, format="JPEG", quality=82)
            if preview.tell() > 1024 * 1024:
                raise ValueError
        except (OSError, ValueError) as exc:
            raise serializers.ValidationError("Choose a readable JPEG, PNG or WebP photograph.") from exc
        part = data.validated_data["part"]
        with transaction.atomic():
            event = get_object_or_404(PlantEvent.objects.select_for_update(), pk=event_id,
                                      plant_id=plant_id, plant__in=accessible_plants(request.user))
            if not can_record(request.user, event.plant):
                raise PermissionDenied
            photos = event.visit_photographs
            existing = photos.filter(author=request.user, submission_id=data.validated_data["submission_id"]).first()
            if existing:
                if existing.source_sha256 != digest or existing.part != part:
                    return Response({"detail": "This submission refers to a different photograph."}, status=409)
                return Response({"id": existing.pk, "event_id": event.pk})
            if not event.plant.is_active or photos.count() >= 6:
                return Response({"detail": "This visit cannot accept another view."}, status=409)
            saved = photos.create(author=request.user, submission_id=data.validated_data["submission_id"],
                                 part=part, jpeg=preview.getvalue(), source_sha256=digest)
            return Response({"id": saved.pk, "event_id": event.pk}, status=201)
