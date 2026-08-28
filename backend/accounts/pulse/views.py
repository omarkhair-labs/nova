import uuid

from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ..trust_safety import blocked_user_ids
from .models import Follow, Pulse, PulseReaction, PulseView
from .serializers import PulseSerializer


MAX_PULSE_IMAGE_BYTES = 15 * 1024 * 1024
MAX_PULSE_VIDEO_BYTES = 60 * 1024 * 1024
MAX_PULSES_IN_FEED = 100
MAX_PULSES_IN_CHAIN = 100


def visible_pulses_for(user):
    followed_ids = Follow.objects.filter(follower=user).values_list("following_id", flat=True)
    author_ids = [user.pk, *followed_ids]
    blocked_ids = blocked_user_ids(user)
    return (
        Pulse.objects.select_related("author", "reply_to", "chain_root")
        .filter(
            Q(author_id__in=author_ids) | Q(reply_to__author=user),
            expires_at__gt=timezone.now(),
            author__is_active=True,
        )
        .exclude(author_id__in=blocked_ids)
        .filter(
            Q(author=user)
            | Q(reply_to__author=user)
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


def pulse_create_values(request):
    media = request.FILES.get("media")
    thumbnail = request.FILES.get("thumbnail")
    note = str(request.data.get("note") or "").strip()
    audience = str(
        request.data.get("audience") or Pulse.Audience.FOLLOWERS
    ).strip().lower()
    requested_media_type = str(request.data.get("media_type") or "").strip().lower()
    category = str(request.data.get("category") or Pulse.Category.VIBES).strip().lower()

    if audience not in Pulse.Audience.values:
        return None, Response(
            {"detail": "Choose a valid Pulse audience."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    if category not in Pulse.Category.values:
        return None, Response(
            {"detail": "Choose a valid Pulse category."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    if len(note) > 180:
        return None, Response(
            {"detail": "Pulse note must be 180 characters or fewer."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    if media is None:
        if requested_media_type not in ("", Pulse.MediaType.TEXT):
            return None, Response(
                {"detail": "Photo and video Pulses require a media file."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if not note:
            return None, Response(
                {"detail": "Add a photo, video, or note to your Pulse."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        media_type = Pulse.MediaType.TEXT
    else:
        if requested_media_type == Pulse.MediaType.TEXT:
            return None, Response(
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
            return None, Response(
                {"detail": "Pulses support photos and videos only."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if requested_media_type and requested_media_type != media_type:
            return None, Response(
                {"detail": "Pulse media type doesn't match the uploaded file."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if media.size > max_bytes:
            return None, Response(
                {"detail": size_message},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if media_type == Pulse.MediaType.VIDEO and content_type != "video/mp4":
            return None, Response(
                {"detail": "Pulse videos must be compatible MP4 files."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if thumbnail is not None:
            thumbnail_type = str(getattr(thumbnail, "content_type", "") or "").lower()
            if not thumbnail_type.startswith("image/") or thumbnail.size > 2 * 1024 * 1024:
                return None, Response(
                    {"detail": "Pulse video thumbnail must be an image up to 2 MB."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

    return {
        "media": media or "",
        "thumbnail": thumbnail or "",
        "media_type": media_type,
        "audience": audience,
        "category": category,
        "note": note,
        "expires_at": None,
    }, None


def create_pulse_response(request, reply_to=None):
    raw_publish_id = str(request.data.get("client_publish_id") or "").strip()
    if raw_publish_id:
        try:
            publish_id = uuid.UUID(raw_publish_id)
        except ValueError:
            return Response(
                {"detail": "Invalid publish identity."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        existing = Pulse.objects.filter(
            author=request.user,
            client_publish_id=publish_id,
        ).first()
        if existing is not None:
            return Response(PulseSerializer(existing, context={"request": request}).data)
    else:
        publish_id = None

    values, error_response = pulse_create_values(request)
    if error_response is not None:
        return error_response

    pulse = Pulse.objects.create(
        author=request.user,
        reply_to=reply_to,
        client_publish_id=publish_id,
        **values,
    )
    return Response(
        PulseSerializer(pulse, context={"request": request}).data,
        status=status.HTTP_201_CREATED,
    )


class PulseFeedView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        pulses = visible_pulses_for(request.user)
        category = str(request.query_params.get("category") or "").strip().lower()
        if category:
            if category not in Pulse.Category.values:
                return Response(
                    {"detail": "Choose a valid Pulse category."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            pulses = pulses.filter(category=category)
        pulses = pulses.order_by("-created_at", "-id")[
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
        return create_pulse_response(request)


class PulseDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pulse_id):
        pulse = visible_pulse_for_request(request, pulse_id)
        return Response(PulseSerializer(pulse, context={"request": request}).data)

    def delete(self, request, pulse_id):
        pulse = get_object_or_404(Pulse, pk=pulse_id, author=request.user)
        pulse.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class PulseReplyView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pulse_id):
        parent = visible_pulse_for_request(request, pulse_id)
        return create_pulse_response(request, reply_to=parent)


class PulseViewView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pulse_id):
        pulse = visible_pulse_for_request(request, pulse_id)
        PulseView.objects.update_or_create(
            pulse=pulse,
            user=request.user,
            defaults={},
        )
        return Response(PulseSerializer(pulse, context={"request": request}).data)


class PulseReactionView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pulse_id):
        pulse = visible_pulse_for_request(request, pulse_id)
        enabled = request.data.get("enabled", True)
        if not isinstance(enabled, bool):
            return Response(
                {"detail": "enabled must be true or false."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if enabled:
            PulseReaction.objects.get_or_create(pulse=pulse, user=request.user)
        else:
            PulseReaction.objects.filter(pulse=pulse, user=request.user).delete()
        return Response(PulseSerializer(pulse, context={"request": request}).data)


class PulseChainView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pulse_id):
        pulse = visible_pulse_for_request(request, pulse_id)
        root = pulse.chain_root or pulse.reply_to or pulse
        members = (
            visible_pulses_for(request.user)
            .filter(Q(pk=root.pk) | Q(chain_root=root))
            .order_by("created_at", "id")[:MAX_PULSES_IN_CHAIN]
        )
        return Response(
            {
                "root_id": root.pk,
                "results": PulseSerializer(
                    members,
                    many=True,
                    context={"request": request},
                ).data,
            }
        )
