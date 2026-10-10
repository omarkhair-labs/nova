"""Account-scoped API; possession of a plant ID never grants access."""
from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import PlantDiscussion, PlantEvent, PlantNickname, PlantUnit, Space, SpaceMember
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


# The questions API deliberately reuses Nova Post/Comment media and moderation.
# Sharing a public photo is an EXPLICIT user action. No private plant data is
# serialized in Circle; public_topic is authored by the user, not inferred.
class PlantQuestionsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, plant_id):
        from accounts.posts.views import public_post_queryset
        from accounts.serializers import PostSerializer

        plant = get_object_or_404(accessible_plants(request.user), pk=plant_id)
        allowed_post_ids = public_post_queryset(request).values("pk")
        links = PlantDiscussion.objects.filter(plant=plant).filter(
            Q(post__author=request.user) | Q(post_id__in=allowed_post_ids)
        ).select_related("post", "post__author")[:50]
        return Response({"results": [
            {
                "id": str(link.pk),
                "topic": link.public_topic,
                "resolved": bool(link.resolved_at),
                "post": PostSerializer(link.post, context={"request": request}).data,
            }
            for link in links
        ]})

    @transaction.atomic
    def post(self, request, plant_id):
        from accounts.serializers import PostSerializer

        plant = get_object_or_404(accessible_plants(request.user), pk=plant_id)
        if not can_record(request.user, plant):
            return Response({"detail": "Read-only space membership."}, status=403)
        if request.data.get("public_share_consent") not in ("true", "True", True):
            return Response({"detail": "Confirm public photo sharing before posting."}, status=400)
        topic = str(request.data.get("public_topic", "")).strip()
        if not 1 <= len(topic) <= 80:
            return Response({"public_topic": "Choose a public topic of 1–80 characters."}, status=400)
        caption = str(request.data.get("caption", "")).strip()
        if not 1 <= len(caption) <= 500:
            return Response({"caption": "Write a question of 1–500 characters."}, status=400)
        serializer = PostSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        post = serializer.save(author=request.user)
        link = PlantDiscussion.objects.create(plant=plant, post=post, public_topic=topic)
        return Response({
            "id": str(link.pk),
            "post_id": post.pk,
            "topic": topic,
            "resolved": False,
        }, status=201)


class PlantQuestionResolveView(APIView):
    permission_classes = [IsAuthenticated]

    @transaction.atomic
    def post(self, request, plant_id, discussion_id):
        plant = get_object_or_404(accessible_plants(request.user), pk=plant_id)
        link = get_object_or_404(
            PlantDiscussion.objects.select_for_update().select_related("post"),
            pk=discussion_id, plant=plant,
        )
        if link.post.author_id != request.user.pk or not can_record(request.user, plant):
            return Response({"detail": "Only the question author with care access may record its outcome."}, status=403)
        if link.resolved_at is not None:
            return Response({"detail": "This question already has a recorded outcome."}, status=409)
        note = request.data.get("note")
        if not isinstance(note, str) or not 1 <= len(note.strip()) <= 1500:
            return Response({"note": "Describe the observed outcome (1–1500 characters)."}, status=400)
        event = PlantEvent.objects.create(
            plant=plant, actor=request.user, kind=PlantEvent.Kind.OUTCOME,
            evidence_type=PlantEvent.Evidence.OBSERVED, note=note.strip(),
            observed_at=timezone.now(),
        )
        link.resolution_event = event
        link.resolved_at = timezone.now()
        link.save(update_fields=("resolved_at", "resolution_event"))
        return Response({"id": str(link.pk), "resolved": True, "event_id": str(event.pk)})


class PlantCircleView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        from accounts.posts.views import public_post_queryset
        from accounts.serializers import PostSerializer

        visible = public_post_queryset(request).filter(plantupia_discussion__isnull=False)
        raw_cursor = request.query_params.get("cursor", "").strip()
        if raw_cursor:
            try:
                cursor = int(raw_cursor)
                if cursor <= 0:
                    raise ValueError
            except ValueError:
                return Response({"detail": "Invalid cursor."}, status=400)
            visible = visible.filter(id__lt=cursor)
        rows = list(visible.select_related("plantupia_discussion").order_by("-id")[:21])
        page, has_more = rows[:20], len(rows) > 20
        return Response({
            "results": [
                {
                    "id": str(post.plantupia_discussion.pk),
                    "topic": post.plantupia_discussion.public_topic,
                    "resolved": bool(post.plantupia_discussion.resolved_at),
                    "post": PostSerializer(post, context={"request": request}).data,
                }
                for post in page
            ],
            "next_cursor": str(page[-1].id) if has_more and page else None,
        })
