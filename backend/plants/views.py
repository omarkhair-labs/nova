"""Account-scoped API; possession of a plant ID never grants access."""
from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import PlantEvent, PlantNickname, PlantUnit, Space, SpaceMember
from .serializers import PlantEventSerializer, PlantSerializer, SpaceSerializer


def accessible_spaces(user):
    return Space.objects.filter(Q(owner=user) | Q(memberships__user=user)).distinct()


def accessible_plants(user):
    return PlantUnit.objects.filter(space__in=accessible_spaces(user)).select_related("space").prefetch_related("nicknames")


def can_record(user, plant):
    if plant.space.owner_id == user.pk:
        return True
    return SpaceMember.objects.filter(
        space=plant.space, user=user,
        role__in=[SpaceMember.Role.CARER, SpaceMember.Role.MANAGER]
    ).exists()


class SpacesView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response({"results": SpaceSerializer(accessible_spaces(request.user), many=True).data})


class PlantsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        qs = accessible_plants(request.user)
        space_id = request.query_params.get("space")
        if space_id:
            qs = qs.filter(space_id=space_id)
        return Response({"results": PlantSerializer(qs, many=True, context={"request": request}).data})


class PlantEventsView(APIView):
    permission_classes = [IsAuthenticated]

    def get_plant(self, request, plant_id):
        return get_object_or_404(accessible_plants(request.user), pk=plant_id)

    def get(self, request, plant_id):
        plant = self.get_plant(request, plant_id)
        return Response({"results": PlantEventSerializer(plant.events.all()[:100], many=True, context={"request": request}).data})

    def post(self, request, plant_id):
        plant = self.get_plant(request, plant_id)
        if not can_record(request.user, plant):
            return Response({"detail": "Read-only space membership."}, status=status.HTTP_403_FORBIDDEN)
        s = PlantEventSerializer(data=request.data, context={"request": request})
        s.is_valid(raise_exception=True)
        event = s.save(plant=plant, actor=request.user)
        return Response(PlantEventSerializer(event, context={"request": request}).data, status=status.HTTP_201_CREATED)


class PlantNicknameView(APIView):
    permission_classes = [IsAuthenticated]

    def put(self, request, plant_id):
        plant = get_object_or_404(accessible_plants(request.user), pk=plant_id)
        name = request.data.get("nickname")
        if not isinstance(name, str) or not name.strip() or len(name.strip()) > 80:
            return Response({"nickname": "Provide a name of 1–80 characters."}, status=400)
        PlantNickname.objects.update_or_create(
            plant=plant, user=request.user, defaults={"nickname": name.strip()}
        )
        return Response({"id": str(plant.pk), "my_name": name.strip()})
