from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ..pulse.serializers import PulseAuthorSerializer, PulseSerializer
from ..pulse.views import visible_pulses_for
from .window import parse_utc_offset, tonight_window


def _parse_offset(request):
    return parse_utc_offset(request.query_params.get("utc_offset_minutes", "0"))


class TonightView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        utc_offset_minutes = _parse_offset(request)
        if utc_offset_minutes is None:
            return Response(
                {"detail": "Invalid UTC offset for Tonight."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        now = timezone.now()
        window = tonight_window(now, utc_offset_minutes)
        base_payload = {
            "is_tonight": window["is_tonight"],
            "local_hour": window["local_hour"],
            "utc_offset_minutes": utc_offset_minutes,
            "starts_at": window["starts_at"].isoformat(),
            "ends_at": window["ends_at"].isoformat(),
        }

        if not window["is_tonight"]:
            return Response(
                {
                    **base_payload,
                    "people_count": 0,
                    "moments_count": 0,
                    "my_moments_count": 0,
                    "people": [],
                }
            )

        pulses = list(
            visible_pulses_for(request.user)
            .filter(
                created_at__gte=window["starts_at"],
                created_at__lt=window["ends_at"],
            )
            .select_related("author")
            .order_by("-created_at", "-id")
        )

        my_moments_count = 0
        people = {}
        visible_people_moments = 0
        for pulse in pulses:
            if pulse.author_id == request.user.pk:
                my_moments_count += 1
                continue

            visible_people_moments += 1
            row = people.get(pulse.author_id)
            if row is None:
                people[pulse.author_id] = {
                    "person": PulseAuthorSerializer(
                        pulse.author,
                        context={"request": request},
                    ).data,
                    "moments_count": 1,
                    "latest_pulse": PulseSerializer(
                        pulse,
                        context={"request": request},
                    ).data,
                    "_latest_at": pulse.created_at,
                }
            else:
                row["moments_count"] += 1

        ordered_people = sorted(
            people.values(),
            key=lambda row: row["_latest_at"],
            reverse=True,
        )
        for row in ordered_people:
            row.pop("_latest_at", None)

        return Response(
            {
                **base_payload,
                "people_count": len(ordered_people),
                "moments_count": visible_people_moments,
                "my_moments_count": my_moments_count,
                "people": ordered_people,
            }
        )
