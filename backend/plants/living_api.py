"""Authenticated, opt-in field research within the existing plant and space authorities."""
from hashlib import sha256

from django.conf import settings
from django.core import signing
from django.db import transaction
from django.db.models import OuterRef, Subquery
from django.http import Http404
from django.shortcuts import get_object_or_404
from rest_framework import serializers
from rest_framework.exceptions import PermissionDenied
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .living_space import build_room
from .living_regions import measure_trace, polygon
from .living_segmentation import METHOD as OUTLINE_METHOD, NoOutline, VisionUnavailable, propose_outline, vision_available
from .living_vision import _open
from .living_daylight import window_scenario
from .living_temporal import compare_projected_areas
from .vision_evidence import MAX_BYTES, inspect_photo
from .models import CareGuideVersion, EvidenceReview, PlantEvent, PlantUnit, RoomRevision, Space, SpaceMember, TracedRegion
from .views import accessible_plants, accessible_spaces, can_record


class ResearchView(APIView):
    permission_classes = [IsAuthenticated]

    def initial(self, request, *args, **kwargs):
        super().initial(request, *args, **kwargs)
        if not settings.PLANTUPIA_LIVING_SYSTEMS_ENABLED:
            raise Http404

    def finalize_response(self, *args, **kwargs):
        response = super().finalize_response(*args, **kwargs)
        response["Cache-Control"] = "private, no-store"
        return response


class Placement(serializers.Serializer):
    plant_id = serializers.UUIDField()
    x_m = serializers.FloatField()
    y_m = serializers.FloatField()


class Window(serializers.Serializer):
    id = serializers.CharField(max_length=128)
    x_m = serializers.FloatField()
    y_m = serializers.FloatField()


class RoomInput(serializers.Serializer):
    base_revision = serializers.IntegerField(min_value=0)
    width_m = serializers.FloatField()
    length_m = serializers.FloatField()
    plants = serializers.ListField(child=Placement(), max_length=1000)
    windows = serializers.ListField(child=Window(), max_length=100)


class RoomRevisionQuery(serializers.Serializer):
    revision = serializers.IntegerField(min_value=1, max_value=2_147_483_647, required=False)


class RoomView(ResearchView):
    def get(self, request, space_id):
        space = get_object_or_404(accessible_spaces(request.user), pk=space_id)
        query = RoomRevisionQuery(data=request.query_params)
        query.is_valid(raise_exception=True)
        revisions = space.room_revisions.order_by("-revision")
        requested = query.validated_data.get("revision")
        historical = requested is not None
        selected = get_object_or_404(revisions, revision=requested) if historical else revisions.first()
        # Current installation authority applies to every revision. As-of timelines
        # also exclude records created later and backdated into this layout.
        latest_event = PlantEvent.objects.filter(plant_id=OuterRef("pk"))
        if historical:
            latest_event = latest_event.filter(observed_at__lte=selected.created_at, created_at__lte=selected.created_at)
        latest_event = latest_event.order_by("-observed_at", "-created_at", "-pk")
        geometry = dict(selected.geometry) if selected else None
        if geometry:
            ids = [row["plant_id"] for row in geometry["plants"]]
            units = PlantUnit.objects.filter(space=space, pk__in=ids, is_active=True).annotate(
                last_event_id=Subquery(latest_event.values("pk")[:1]),
                last_event_at=Subquery(latest_event.values("observed_at")[:1]),
            )
            current = {str(unit.pk): unit for unit in units}
            geometry["plants"] = [
                {**row, "label": current[key].label, "latest_event_id": current[key].last_event_id,
                 "latest_observed_at": current[key].last_event_at}
                for row in geometry["plants"] if (key := row["plant_id"]) in current
            ]
        return Response({"space_id": space.pk, "revision": selected.revision if selected else 0,
                         "geometry_recorded_at": selected.created_at if selected else None, "room": geometry,
                         "historical": historical, "layout_revisions": list(revisions.values("revision", "created_at")[:30])})

    @transaction.atomic
    def post(self, request, space_id):
        space = get_object_or_404(Space.objects.select_for_update().filter(
            pk__in=accessible_spaces(request.user).values("pk")), pk=space_id)
        if space.owner_id != request.user.pk and not space.memberships.filter(
                user=request.user, role=SpaceMember.Role.MANAGER).exists():
            raise PermissionDenied
        data = RoomInput(data=request.data)
        data.is_valid(raise_exception=True)
        fields = data.validated_data
        latest = space.room_revisions.order_by("-revision").first()
        revision = latest.revision if latest else 0
        placements = [{**row, "plant_id": str(row["plant_id"])} for row in fields["plants"]]
        try:
            room = build_room(str(space.pk), fields["width_m"], fields["length_m"], placements, fields["windows"])
        except ValueError as error:
            raise serializers.ValidationError(str(error)) from error
        ids = [row["plant_id"] for row in placements]
        if PlantUnit.objects.filter(space=space, is_active=True, pk__in=ids).count() != len(ids):
            raise serializers.ValidationError("Choose active plants in this space.")
        if latest and fields["base_revision"] + 1 == revision and latest.actor_id == request.user.pk and latest.geometry == room:
            return Response({"revision": revision})
        if fields["base_revision"] != revision:
            return Response({"detail": "Refresh the room before editing.", "revision": revision}, status=409)
        RoomRevision.objects.create(space=space, revision=revision + 1, actor=request.user, geometry=room)
        return Response({"revision": revision + 1}, status=201)


class TriageInput(serializers.Serializer):
    symptom = serializers.ChoiceField(choices=["wilting", "discoloration", "spots", "damage", "uncertain"])
    substrate_report = serializers.ChoiceField(choices=["dry", "wet", "unknown"], default="unknown")


def inspection_options(symptom, substrate):
    if symptom == "wilting":
        options = [("limited_water_availability", "dry", "wet", "Check moisture below the surface."),
                   ("sustained_saturation", "wet", "dry", "Inspect drainage and check for standing water.")]
        return [{"hypothesis": name, "symptom_report": symptom,
                 "supporting_report": substrate if substrate == support else None,
                 "contradictory_report": substrate if substrate == against else None,
                 "inspection": step} for name, support, against, step in options]
    checks = {"spots": "Inspect both leaf surfaces and record the distribution.",
              "discoloration": "Compare older and newer leaves under the same light.",
              "damage": "Inspect edges and contact points and photograph the affected region."}
    return [{"hypothesis": None, "inspection": checks[symptom]}] if symptom in checks else []


class FieldEvidenceView(ResearchView):
    def event(self, request, plant_id, event_id):
        plant = get_object_or_404(accessible_plants(request.user), pk=plant_id)
        return get_object_or_404(PlantEvent, plant=plant, pk=event_id)

    def get(self, request, plant_id, event_id):
        event = self.event(request, plant_id, event_id)
        reviews = event.evidence_reviews.order_by("-pk")
        quality = event.capture_quality
        if not quality and event.photo:
            # Existing journal photos predate the capture metadata. Read only
            # this authorized image and leave the original record untouched.
            try:
                with event.photo.open("rb") as source:
                    quality = inspect_photo(source.read(MAX_BYTES + 1))
            except (ValueError, OSError):
                quality = None
        return Response({"event_id": event.pk, "quality": quality,
                         "can_suggest_region": bool(event.photo and event.plant.is_active and
                             can_record(request.user, event.plant) and vision_available()),
                         "can_review": event.plant.is_active and request.user.has_perm("plants.review_visual_evidence") and can_record(request.user, event.plant),
                         "reviews": list(reviews.values("id", "finding", "rationale", "created_at")[:20]),
                         "regions": list(event.traced_regions.order_by("-pk").values(
                             "id", "source_sha256", "annotation", "measurements", "created_at")[:20]),
                         "region_count": event.traced_regions.count(), "review_count": reviews.count()})

    def post(self, request, plant_id, event_id):
        event = self.event(request, plant_id, event_id)
        data = TriageInput(data=request.data)
        data.is_valid(raise_exception=True)
        plant = event.plant
        species_id = plant.verified_species_id or plant.selected_species_id
        guide = CareGuideVersion.objects.filter(species_id=species_id, published=True).order_by("-version").first()
        maintenance = plant.events.filter(kind=PlantEvent.Kind.CARE, observed_at__lte=event.observed_at)
        return Response({"event_id": event.pk, "observed_at": event.observed_at, "input_source": "user_report",
                         "species_basis": "verified" if plant.verified_species_id else "selected" if species_id else "unknown",
                         "guide_version": guide.version if guide else None,
                         "recent_care": list(maintenance.order_by("-observed_at", "-pk").values("id", "observed_at", "note")[:3]),
                         "inspection_options": inspection_options(data.validated_data["symptom"], data.validated_data["substrate_report"]),
                         "diagnosis": None, "model": "inspection-rules-01"})


class ReviewInput(serializers.Serializer):
    submission_id = serializers.UUIDField()
    finding = serializers.ChoiceField(choices=["supported_observation", "corrected_observation", "insufficient_evidence"])
    rationale = serializers.CharField(max_length=1000)


class EvidenceReviewConflict(Exception):
    """A review cannot change a previous acknowledged record or an archive."""


@transaction.atomic
def save_evidence_review(user, plant_id, event_id, fields):
    """One append-only specialist review transaction, shared by Android and staff web."""
    if not settings.PLANTUPIA_LIVING_SYSTEMS_ENABLED:
        raise Http404
    event = get_object_or_404(PlantEvent.objects.select_for_update(), pk=event_id, plant_id=plant_id)
    if not user.has_perm("plants.review_visual_evidence") or not can_record(user, event.plant):
        raise PermissionDenied
    if not event.plant.is_active:
        raise EvidenceReviewConflict("Archived history is read-only.")
    review, created = EvidenceReview.objects.get_or_create(event=event, reviewer=user,
        submission_id=fields["submission_id"], defaults=fields)
    if not created and any(getattr(review, k) != v for k, v in fields.items()):
        raise EvidenceReviewConflict("Submission ID already used for another review.")
    return review, created


class ReviewView(FieldEvidenceView):
    http_method_names = ["post", "options"]

    @transaction.atomic
    def post(self, request, plant_id, event_id):
        event = self.event(request, plant_id, event_id)
        if not request.user.has_perm("plants.review_visual_evidence") or not can_record(request.user, event.plant):
            raise PermissionDenied
        if not event.plant.is_active:
            return Response({"detail": "Archived history is read-only."}, status=409)
        data = ReviewInput(data=request.data)
        data.is_valid(raise_exception=True)
        try:
            review, created = save_evidence_review(request.user, plant_id, event.pk, data.validated_data)
        except EvidenceReviewConflict as exc:
            return Response({"detail": str(exc)}, status=409)
        return Response({"id": review.pk}, status=201 if created else 200)


class ScaleReference(serializers.Serializer):
    rectified_coplanar = serializers.BooleanField()
    reference_cm = serializers.FloatField(min_value=0.0001, max_value=10000)
    reference_px = serializers.FloatField(min_value=1, max_value=50000)
    check_cm = serializers.FloatField(min_value=0.0001, max_value=10000)
    check_px = serializers.FloatField(min_value=1, max_value=50000)


OUTLINE_SALT = "plantupia.private-region-proposal.v1"


class OutlineInput(serializers.Serializer):
    foreground = serializers.ListField(min_length=1, max_length=6, child=serializers.ListField(
        min_length=2, max_length=2, child=serializers.IntegerField(min_value=0, max_value=20000000)))
    background = serializers.ListField(min_length=1, max_length=6, child=serializers.ListField(
        min_length=2, max_length=2, child=serializers.IntegerField(min_value=0, max_value=20000000)))


class RegionSuggestionView(FieldEvidenceView):
    http_method_names = ["post", "options"]

    def post(self, request, plant_id, event_id):
        event = self.event(request, plant_id, event_id)
        if not can_record(request.user, event.plant):
            raise PermissionDenied
        if not event.plant.is_active or not event.photo:
            return Response({"detail": "An active plant and a recorded photo are required."}, status=409)
        data = OutlineInput(data=request.data)
        data.is_valid(raise_exception=True)
        try:
            with event.photo.open("rb") as source:
                raw = source.read(MAX_BYTES + 1)
            image = _open(raw, {"JPEG", "PNG", "WEBP"})
            points = propose_outline(image, **data.validated_data)
            try:
                polygon(points, image.width, image.height)
            except ValueError as exc:
                raise NoOutline("The proposed boundary is not a simple outline. Trace manually.") from exc
        except NoOutline as exc:
            return Response({"status": "abstained", "reason": str(exc)})
        except VisionUnavailable as exc:
            return Response({"detail": str(exc)}, status=503)
        except (ValueError, OSError) as exc:
            raise serializers.ValidationError("Photo or outline points could not be read.") from exc
        # Only explicit later acceptance can write a trace. This signed envelope
        # prevents clients from assigning a computed method to arbitrary vertices.
        envelope = {"actor": str(request.user.pk), "event": str(event.pk),
                    "source": sha256(raw).hexdigest(), "vertices": points,
                    "method": OUTLINE_METHOD, "prompts": dict(data.validated_data)}
        return Response({"status": "candidate", "method": OUTLINE_METHOD, "vertices": points,
                         "suggestion_token": signing.dumps(envelope, salt=OUTLINE_SALT, compress=True),
                         "identity": "not_assessed", "health": "not_assessed"})


class PointScaleReference(serializers.Serializer):
    face_on_coplanar = serializers.BooleanField()
    points = serializers.ListField(min_length=4, max_length=4, child=serializers.ListField(
        min_length=2, max_length=2, child=serializers.IntegerField(min_value=0, max_value=20000000)))
    reference_cm = serializers.FloatField(min_value=.001, max_value=10000)
    check_cm = serializers.FloatField(min_value=.001, max_value=10000)


class RegionInput(serializers.Serializer):
    submission_id = serializers.UUIDField()
    part = serializers.ChoiceField(choices=["plant", "leaf", "stem", "flower", "pot", "soil", "exposed_root", "visible_damage"])
    vertices = serializers.ListField(min_length=3, max_length=64, child=serializers.ListField(
        min_length=2, max_length=2, child=serializers.IntegerField(min_value=0, max_value=20000000)))
    reference = ScaleReference(required=False)
    point_reference = PointScaleReference(required=False)
    suggestion_token = serializers.CharField(required=False, max_length=8192)

    def validate(self, attrs):
        if "reference" in attrs and "point_reference" in attrs:
            raise serializers.ValidationError("Use only one scale reference method.")
        return attrs


class RegionView(FieldEvidenceView):
    http_method_names = ["post", "options"]

    def post(self, request, plant_id, event_id):
        event = self.event(request, plant_id, event_id)
        if not can_record(request.user, event.plant):
            raise PermissionDenied
        if not event.plant.is_active or not event.photo:
            return Response({"detail": "An active plant and a recorded photo are required."}, status=409)
        data = RegionInput(data=request.data)
        data.is_valid(raise_exception=True)
        annotation = dict(data.validated_data)
        key = annotation.pop("submission_id")
        token = annotation.pop("suggestion_token", None)
        if token:
            annotation["assistance"] = {"method": OUTLINE_METHOD, "proposal_digest": sha256(token.encode()).hexdigest()}
        lookup = dict(event=event, author=request.user, submission_id=key)
        def replay(existing):
            if existing.annotation != annotation:
                return Response({"detail": "This submission ID refers to a different trace."}, status=409)
            return Response({"id": existing.pk, "measurements": existing.measurements})
        prior = TracedRegion.objects.filter(**lookup).first()
        if prior:
            return replay(prior)
        try:
            with event.photo.open("rb") as f:
                raw = f.read(MAX_BYTES+1)
            digest = sha256(raw).hexdigest()
            if token:
                try:
                    envelope = signing.loads(token, salt=OUTLINE_SALT, max_age=900)
                except signing.BadSignature:
                    return Response({"detail": "The proposal expired or changed. Find an outline again."}, status=409)
                expected = {"actor": str(request.user.pk), "event": str(event.pk),
                            "source": digest, "vertices": annotation["vertices"], "method": OUTLINE_METHOD}
                if not isinstance(envelope, dict) or any(envelope.get(k) != v for k, v in expected.items()):
                    return Response({"detail": "The proposal does not match this photograph and outline."}, status=409)
            result = measure_trace(raw, annotation["vertices"], annotation.get("reference"),
                                   annotation.get("point_reference"))
        except (ValueError, OSError) as exc:
            raise serializers.ValidationError("Photo or region could not be measured.") from exc
        result.update(method=OUTLINE_METHOD if token else "upright-operator-trace-v1", identity_basis="operator_label")
        if token:
            result["assistance"] = {"operator_accepted": True, "prompts": envelope["prompts"]}
        trace, created = TracedRegion.objects.get_or_create(**lookup, defaults=dict(
            annotation=annotation, measurements=result, source_sha256=sha256(raw).hexdigest()))
        return Response({"id": trace.pk, "measurements": result}, status=201) if created else replay(trace)


class ProjectedComparisonInput(serializers.Serializer):
    earlier_trace_id = serializers.IntegerField(min_value=1, max_value=9223372036854775807)
    later_trace_id = serializers.IntegerField(min_value=1, max_value=9223372036854775807)
    same_part = serializers.BooleanField()
    comparable_capture = serializers.BooleanField()


class ProjectedComparisonView(ResearchView):
    http_method_names = ["post", "options"]

    def post(self, request, plant_id):
        plant = get_object_or_404(accessible_plants(request.user), pk=plant_id)
        data = ProjectedComparisonInput(data=request.data)
        data.is_valid(raise_exception=True)
        fields = data.validated_data
        traces = TracedRegion.objects.filter(event__plant=plant).select_related("event")
        earlier = get_object_or_404(traces, pk=fields["earlier_trace_id"])
        later = get_object_or_404(traces, pk=fields["later_trace_id"])
        return Response(compare_projected_areas(earlier, later,
            same_part=fields["same_part"], comparable_capture=fields["comparable_capture"]))


class ScenarioInput(serializers.Serializer):
    revision = serializers.IntegerField(min_value=1)
    window_id = serializers.CharField(max_length=128)
    point = serializers.ListField(min_length=3, max_length=3, child=serializers.FloatField())
    top_normal_deg = serializers.FloatField(min_value=0, max_value=360)
    width_m = serializers.FloatField(min_value=.001, max_value=1000)
    sill_m = serializers.FloatField(min_value=0, max_value=1000)
    head_m = serializers.FloatField(min_value=.001, max_value=1000)
    latitude = serializers.FloatField(min_value=-90, max_value=90)
    longitude = serializers.FloatField(min_value=-180, max_value=180)
    timestamps = serializers.ListField(min_length=1, max_length=96, child=serializers.CharField(max_length=40))


class DaylightView(ResearchView):
    def post(self, request, space_id):
        from datetime import datetime
        space = get_object_or_404(accessible_spaces(request.user), pk=space_id)
        data = ScenarioInput(data=request.data)
        data.is_valid(raise_exception=True)
        fields = dict(data.validated_data)
        latest = space.room_revisions.order_by("-revision").first()
        if not latest or latest.revision != fields.pop("revision"):
            return Response({"detail": "Refresh the room revision before simulating."}, status=409)
        try:
            fields["timestamps"] = [datetime.fromisoformat(s) for s in fields["timestamps"]]
            result = window_scenario(latest.geometry, **fields)
        except ValueError as error:
            raise serializers.ValidationError(str(error)) from error
        return Response({**result, "revision": latest.revision})
