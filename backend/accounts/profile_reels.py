from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .reels import _reel_payload, visible_reels_for


PROFILE_REELS_PAGE_SIZE = 12


class ProfileReelsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, username):
        clean_username = str(username or "").strip().lower()
        raw_cursor = str(request.query_params.get("cursor") or "").strip()

        queryset = (
            visible_reels_for(request.user)
            .filter(author__username=clean_username)
            .order_by("-created_at", "-id")
        )

        if raw_cursor:
            try:
                cursor = int(raw_cursor)
            except (TypeError, ValueError):
                return Response(
                    {"detail": "Invalid profile Reels cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            queryset = queryset.filter(pk__lt=cursor)

        rows = list(queryset[: PROFILE_REELS_PAGE_SIZE + 1])
        has_more = len(rows) > PROFILE_REELS_PAGE_SIZE
        page = rows[:PROFILE_REELS_PAGE_SIZE]
        next_cursor = str(page[-1].pk) if has_more and page else None

        return Response(
            {
                "username": clean_username,
                "results": [_reel_payload(request, reel) for reel in page],
                "next_cursor": next_cursor,
            }
        )
