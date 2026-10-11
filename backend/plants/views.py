"""Account-scoped API; possession of a plant ID never grants access."""
from django.db import transaction
from django.db.models import OuterRef, Q, Subquery
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import PlantDiscussion, PlantEvent, PlantNickname, PlantUnit, Space, SpaceMember
from .serializers import (PlantCreateSerializer, PlantEventSerializer, PlantSerializer, SpaceCreateSerializer, SpaceSerializer)
from .media import sanitize_public_plant_photo


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

    @transaction.atomic
    def post(self, request):
        # A signed-in person can start a space without an admin invitation.
        # Never accept an owner field from the request.
        serializer = SpaceCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        space = serializer.save(owner=request.user)
        return Response(SpaceSerializer(space).data, status=status.HTTP_201_CREATED)


class PlantsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        qs = accessible_plants(request.user)
        space_id = request.query_params.get("space")
        if space_id:
            qs = qs.filter(space_id=space_id)

        # Show verified activity rather than inventing a health score. Subqueries
        # keep this bounded when a commercial space has many planting units.
        latest = PlantEvent.objects.filter(plant_id=OuterRef("pk")).order_by(
            "-observed_at", "-created_at"
        )
        qs = qs.annotate(
            last_event_kind=Subquery(latest.values("kind")[:1]),
            last_event_at=Subquery(latest.values("observed_at")[:1]),
        )
        editable_space_ids = set(
            Space.objects.filter(
                Q(owner=request.user)
                | Q(
                    memberships__user=request.user,
                    memberships__role__in=[
                        SpaceMember.Role.CARER, SpaceMember.Role.MANAGER
                    ],
                )
            ).values_list("pk", flat=True)
        )
        return Response({
            "results": PlantSerializer(
                qs, many=True,
                context={
                    "request": request,
                    "editable_space_ids": editable_space_ids,
                },
            ).data
        })


    @transaction.atomic
    def post(self, request):
        serializer = PlantCreateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        space_id = serializer.validated_data.pop("space")
        writable_spaces = Space.objects.filter(
            Q(owner=request.user)
            | Q(memberships__user=request.user, memberships__role=SpaceMember.Role.MANAGER)
        ).distinct()
        # 404 prevents disclosure of another customer's space.
        space = get_object_or_404(writable_spaces, pk=space_id)
        plant = serializer.save(space=space)
        return Response(
            PlantSerializer(
                plant,
                context={"request": request, "editable_space_ids": {space.pk}},
            ).data,
            status=status.HTTP_201_CREATED,
        )


class PlantEventsView(APIView):
    permission_classes = [IsAuthenticated]

    def get_plant(self, request, plant_id):
        return get_object_or_404(accessible_plants(request.user), pk=plant_id)

    def get(self, request, plant_id):
        plant = self.get_plant(request, plant_id)
        raw_offset = request.query_params.get("offset", "0")
        try:
            offset = int(raw_offset)
            if offset < 0 or offset > 1000000:
                raise ValueError
        except (TypeError, ValueError):
            return Response({"detail": "Invalid event history offset."}, status=400)
        # Years of observed history must not silently end after 100 entries.
        size = 20
        rows = list(plant.events.order_by("-observed_at", "-created_at", "-pk")[offset:offset + size + 1])
        return Response({
            "results": PlantEventSerializer(rows[:size], many=True, context={"request": request}).data,
            "next_offset": offset + size if len(rows) > size else None,
        })

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
        # Public Nova posts must not preserve the original photo's GPS/EXIF,
        # even when the user consents to publishing the visible image.
        try:
            clean_photo = sanitize_public_plant_photo(request.FILES.get("image"))
        except ValueError as error:
            return Response({"image": str(error)}, status=400)
        if clean_photo is None:
            return Response({"image": "Select a photo to share publicly."}, status=400)
        data = {
            "caption": caption,
            "media_type": "image",
            "image": clean_photo,
        }
        serializer = PostSerializer(data=data, context={"request": request})
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
