from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .service import build_weekly_memory
from .window import parse_utc_offset, parse_weeks_ago


class WeeklyMemoryView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        utc_offset_minutes = parse_utc_offset(
            request.query_params.get("utc_offset_minutes", "0")
        )
        if utc_offset_minutes is None:
            return Response(
                {"detail": "Invalid UTC offset for Memories."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        weeks_ago = parse_weeks_ago(request.query_params.get("weeks_ago", "0"))
        if weeks_ago is None:
            return Response(
                {"detail": "weeks_ago must be between 0 and 51."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        return Response(
            build_weekly_memory(
                request=request,
                utc_offset_minutes=utc_offset_minutes,
                weeks_ago=weeks_ago,
                now=timezone.now(),
            )
        )
