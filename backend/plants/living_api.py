"""Authenticated, opt-in field research within the existing plant and space authorities."""
from django.conf import settings
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
from .models import CareGuideVersion, EvidenceReview, PlantEvent, PlantUnit, RoomRevision, Space, SpaceMember
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


class RoomView(ResearchView):
    def get(self, request, space_id):
        space = get_object_or_404(accessible_spaces(request.user), pk=space_id)
        latest = space.room_revisions.order_by("-revision").first()
        # Re-read current membership of each plant and its actual latest history on every sync.
        # Historic placement JSON alone may never expose a unit transferred out of this space.
        latest_event = PlantEvent.objects.filter(plant_id=OuterRef("pk")).order_by("-observed_at", "-created_at", "-pk")
        geometry = dict(latest.geometry) if latest else None
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
        return Response({"space_id": space.pk, "revision": latest.revision if latest else 0,
                         "geometry_recorded_at": latest.created_at if latest else None, "room": geometry})

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
        return Response({"event_id": event.pk, "quality": event.capture_quality,
                         "reviews": list(reviews.values("id", "finding", "rationale", "created_at")[:20]),
                         "review_count": reviews.count()})

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
        PlantEvent.objects.select_for_update().get(pk=event.pk)
        review, created = EvidenceReview.objects.get_or_create(event=event, reviewer=request.user,
            submission_id=data.validated_data["submission_id"], defaults=data.validated_data)
        if not created and any(getattr(review, k) != v for k, v in data.validated_data.items()):
            return Response({"detail": "Submission ID already used for another review."}, status=409)
        return Response({"id": review.pk}, status=201 if created else 200)
