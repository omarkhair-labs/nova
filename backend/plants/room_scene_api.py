"""Scoped room revision photo sets, held privately only while reconstruction is pending."""
from base64 import b64encode
from hashlib import sha256
from io import BytesIO
from zipfile import ZipFile

from django.conf import settings
from django.db import transaction
from django.http import Http404
from django.shortcuts import get_object_or_404
from PIL import Image
from rest_framework import serializers
from rest_framework.exceptions import PermissionDenied
from rest_framework.response import Response

from .living_api import ResearchView
from .living_vision import _open
from .models import PlantUnit, RoomPhotoScene, RoomRevision, SpaceMember
from .views import accessible_spaces
from .vision_evidence import MAX_BYTES


def describe(scene):
    if scene is None:
        return {"state": "empty"}
    return {"digest": scene.digest, "state": scene.state, "views": scene.submitted_views, **scene.summary}


def prepare_views(files):
    if not 5 <= len(files) <= 8 or sum(f.size for f in files) > 24 * 1024 * 1024:
        raise serializers.ValidationError("Choose 5 to 8 overlapping photos, up to 24 MB together.")
    seen = set()
    archive = BytesIO()
    try:
        with ZipFile(archive, "w") as out:
            for i, upload in enumerate(files):
                image = _open(upload.read(MAX_BYTES + 1), {"JPEG", "PNG", "WEBP"}).convert("RGB")
                if min(image.size) < 240:
                    raise ValueError
                image.thumbnail((1280, 1280), Image.Resampling.LANCZOS)
                stream = BytesIO()
                image.save(stream, format="JPEG", quality=82)
                payload = stream.getvalue()
                digest = sha256(payload).digest()
                if digest in seen:
                    raise serializers.ValidationError("Choose different viewpoints of the same scene.")
                seen.add(digest)
                out.writestr(f"view-{i}.jpg", payload)
    except (ValueError, OSError) as exc:
        raise serializers.ValidationError("Choose readable photographs at least 240 pixels across.") from exc
    return sha256(b"".join(sorted(seen))).hexdigest(), archive.getvalue()


class RoomPhotoSceneView(ResearchView):
    def revision(self, request, space_id, number, write=False):
        space = get_object_or_404(accessible_spaces(request.user), pk=space_id)
        if write and space.owner_id != request.user.pk and not space.memberships.filter(
                user=request.user, role=SpaceMember.Role.MANAGER).exists():
            raise PermissionDenied
        qs = RoomRevision.objects.filter(space=space)
        revision = get_object_or_404(qs.select_for_update() if write else qs, revision=number)
        plant = request.query_params.get("plant")
        if plant:
            from uuid import UUID
            try:
                plant = UUID(plant)
            except ValueError as exc:
                raise Http404 from exc
            plant = get_object_or_404(PlantUnit, pk=plant, space=space, is_active=True)
        return revision, str(plant.pk) if plant else "room"

    def get(self, request, space_id, number):
        revision, subject = self.revision(request, space_id, number)
        scene = RoomPhotoScene.objects.defer("inputs", "ply", "surface").filter(revision=revision, subject=subject).first()
        data = describe(scene)
        if scene and scene.state == "ready" and request.query_params.get("geometry") == "1":
            data["surface"] = scene.surface
            data["ply"] = b64encode(scene.ply).decode("ascii")
        data["can_build"] = settings.PLANTUPIA_ROOM_SFM_ENABLED
        return Response(data)

    @transaction.atomic
    def post(self, request, space_id, number):
        if not settings.PLANTUPIA_ROOM_SFM_ENABLED:
            raise Http404
        revision, subject = self.revision(request, space_id, number, write=True)
        if revision.space.room_revisions.order_by("-revision").values_list("pk", flat=True).first() != revision.pk:
            return Response({"detail": "Start a new scene from the current layout."}, status=409)
        files = list(request.FILES.values())
        digest, archive = prepare_views(files)
        scene = RoomPhotoScene.objects.defer("inputs", "ply", "surface").filter(revision=revision, subject=subject).first()
        if scene and scene.digest == digest and scene.state in {"queued", "processing", "ready"}:
            return Response(describe(scene))
        if scene and scene.state != "failed":
            return Response({"detail": "Remove the existing scene before replacing its photos."}, status=409)
        scene, _ = RoomPhotoScene.objects.update_or_create(revision=revision, subject=subject, defaults=dict(digest=digest,
            submitted_views=len(files), inputs=archive, state="queued", ply=b"", summary={}, lease=None))
        return Response(describe(scene), status=202)

    @transaction.atomic
    def delete(self, request, space_id, number):
        revision, subject = self.revision(request, space_id, number, write=True)
        scene = RoomPhotoScene.objects.only("digest").filter(revision=revision, subject=subject).first()
        if scene and request.query_params.get("expected") != scene.digest:
            return Response({"detail": "Refresh the scene before removing it."}, status=409)
        if scene:
            scene.delete()
        return Response(status=204)
