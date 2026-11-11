"""One authenticated calendar projection over real Living History observations.

This is read-only. It never invents tasks, infers watering, grants access to
another person's plant, or copies care events to a second datastore.
"""
from datetime import date, timedelta

from django.db.models import Count
from django.db.models.functions import TruncDate
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import PlantEvent, PlantNickname
from .views import accessible_plants


class PlantCareCalendarHistoryView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        start_text = request.query_params.get("from", "")
        end_text = request.query_params.get("to", "")
        try:
            start = date.fromisoformat(start_text)
            end = date.fromisoformat(end_text)
            if not (start <= end <= start + timedelta(days=41)):
                raise ValueError
        except (ValueError, TypeError, OverflowError):
            return Response({"detail": "Select a valid inclusive date range of at most 42 days."}, status=400)

        # Public post activity never leaks private plant history. Membership
        # and ownership are checked using the same My Plants ACL.
        visible = accessible_plants(request.user).values("pk")
        rows = list(
            PlantEvent.objects.filter(
                plant_id__in=visible,
                observed_at__date__range=(start, end),
            ).annotate(day=TruncDate("observed_at"))
            .values("day", "plant_id", "plant__label", "kind")
            .annotate(count=Count("id"))
            .order_by("day", "plant_id", "kind")
        )
        plant_ids = {row["plant_id"] for row in rows}
        nicknames = dict(
            PlantNickname.objects.filter(user=request.user, plant_id__in=plant_ids)
            .values_list("plant_id", "nickname")
        )
        result = [
            {
                "date": row["day"].isoformat(),
                "plant_id": str(row["plant_id"]),
                "plant_name": nicknames.get(row["plant_id"]) or row["plant__label"],
                "kind": row["kind"],
                "count": row["count"],
                "state": "recorded",
            }
            for row in rows
        ]
        return Response(
            {"results": result},
            headers={"Cache-Control": "private, no-store", "Vary": "Authorization, Cookie"},
        )
