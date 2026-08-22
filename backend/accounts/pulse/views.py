from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ..trust_safety import blocked_user_ids
from .models import Follow, Pulse
from .serializers import PulseSerializer


MAX_PULSE_IMAGE_BYTES = 15 * 1024 * 1024
MAX_PULSE_VIDEO_BYTES = 60 * 1024 * 1024
MAX_PULSES_IN_FEED = 100


def visible_pulses_for(user):
    followed_ids = Follow.objects.filter(follower=user).values_list("following_id", flat=True)
    author_ids = [user.pk, *followed_ids]
    blocked_ids = blocked_user_ids(user)
    return (
        Pulse.objects.select_related("author")
        .filter(
            expires_at__gt=timezone.now(),
            author__is_active=True,
            author_id__in=author_ids,
        )
        .exclude(author_id__in=blocked_ids)
        .filter(
            Q(author=user)
            | Q(audience=Pulse.Audience.FOLLOWERS)
            | Q(
                audience=Pulse.Audience.CLOSE_FRIENDS,
                author__close_friends_created__member=user,
            )
        )
        .distinct()
    )


def visible_pulse_for_request(request, pulse_id):
    return get_object_or_404(visible_pulses_for(request.user), pk=pulse_id)


class PulseFeedView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        pulses = visible_pulses_for(request.user).order_by("-created_at", "-id")[
            :MAX_PULSES_IN_FEED
        ]
        return Response(
            {
                "results": PulseSerializer(
                    pulses,
                    many=True,
                    context={"request": request},
                ).data
            }
        )

    def post(self, request):
        media = request.FILES.get("media")
        note = str(request.data.get("note") or "").strip()
        audience = str(
            request.data.get("audience") or Pulse.Audience.FOLLOWERS
        ).strip().lower()
        requested_media_type = str(request.data.get("media_type") or "").strip().lower()

        if audience not in Pulse.Audience.values:
            return Response(
                {"detail": "Choose a valid Pulse audience."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(note) > 180:
            return Response(
                {"detail": "Pulse note must be 180 characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if media is None:
            if requested_media_type not in ("", Pulse.MediaType.TEXT):
                return Response(
                    {"detail": "Photo and video Pulses require a media file."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if not note:
                return Response(
                    {"detail": "Add a photo, video, or note to your Pulse."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            media_type = Pulse.MediaType.TEXT
        else:
            if requested_media_type == Pulse.MediaType.TEXT:
                return Response(
                    {"detail": "Text Pulses can't include a media file."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            content_type = str(getattr(media, "content_type", "") or "").lower()
            if content_type.startswith("image/"):
                media_type = Pulse.MediaType.IMAGE
                max_bytes = MAX_PULSE_IMAGE_BYTES
                size_message = "Pulse photo must be 15 MB or smaller."
            elif content_type.startswith("video/"):
                media_type = Pulse.MediaType.VIDEO
                max_bytes = MAX_PULSE_VIDEO_BYTES
                size_message = "Pulse video must be 60 MB or smaller."
            else:
                return Response(
                    {"detail": "Pulses support photos and videos only."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if requested_media_type and requested_media_type != media_type:
                return Response(
                    {"detail": "Pulse media type doesn't match the uploaded file."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if media.size > max_bytes:
                return Response(
                    {"detail": size_message},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        pulse = Pulse.objects.create(
            author=request.user,
            media=media or "",
            media_type=media_type,
            audience=audience,
            note=note,
            expires_at=None,
        )
        return Response(
            PulseSerializer(pulse, context={"request": request}).data,
            status=status.HTTP_201_CREATED,
        )


class PulseDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pulse_id):
        pulse = visible_pulse_for_request(request, pulse_id)
        return Response(PulseSerializer(pulse, context={"request": request}).data)

    def delete(self, request, pulse_id):
        pulse = get_object_or_404(Pulse, pk=pulse_id, author=request.user)
        pulse.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
