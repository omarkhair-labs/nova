from django.db.models import OuterRef, Subquery
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .privacy import can_view_user_content
from .reels import _attach_repost_context, _reel_payload, visible_reels_for
from .reels_models import ReelRepost
from .trust_safety import active_person_for


PROFILE_REELS_PAGE_SIZE = 12


def _positive_cursor(raw_cursor, message):
    if not raw_cursor:
        return None
    try:
        value = int(raw_cursor)
    except (TypeError, ValueError):
        value = 0
    if value <= 0:
        return Response({"detail": message}, status=status.HTTP_400_BAD_REQUEST)
    return value


class ProfileReelsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, username):
        clean_username = str(username or "").strip().lower()
        raw_cursor = str(request.query_params.get("cursor") or "").strip()
        source = str(request.query_params.get("source") or "authored").strip().lower()
        if source not in {"authored", "reposted"}:
            return Response(
                {"detail": "Invalid profile Reels source."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        person = active_person_for(request.user, clean_username)
        if not can_view_user_content(request.user, person):
            return Response(
                {"detail": "This account is private. Follow to see Reels."},
                status=status.HTTP_403_FORBIDDEN,
            )

        if source == "reposted":
            repost_id = ReelRepost.objects.filter(
                user=person,
                reel_id=OuterRef("pk"),
            ).values("id")[:1]
            queryset = (
                visible_reels_for(request.user)
                .annotate(profile_repost_id=Subquery(repost_id))
                .filter(profile_repost_id__isnull=False)
                .order_by("-profile_repost_id")
            )
            cursor = _positive_cursor(raw_cursor, "Invalid profile Reel-repost cursor.")
            if isinstance(cursor, Response):
                return cursor
            if cursor is not None:
                queryset = queryset.filter(profile_repost_id__lt=cursor)
        else:
            queryset = (
                visible_reels_for(request.user)
                .filter(author=person)
                .order_by("-created_at", "-id")
            )
            cursor = _positive_cursor(raw_cursor, "Invalid profile Reels cursor.")
            if isinstance(cursor, Response):
                return cursor
            if cursor is not None:
                queryset = queryset.filter(pk__lt=cursor)

        rows = list(queryset[: PROFILE_REELS_PAGE_SIZE + 1])
        has_more = len(rows) > PROFILE_REELS_PAGE_SIZE
        page = _attach_repost_context(rows[:PROFILE_REELS_PAGE_SIZE])
        if source == "reposted":
            # In a profile Reposts tab the profile owner is the relevant reposter,
            # even when feed-specific followed-reposter context points elsewhere.
            for reel in page:
                reel.feed_reposted_by_value = person
            next_cursor = (
                str(page[-1].profile_repost_id)
                if has_more and page
                else None
            )
        else:
            next_cursor = str(page[-1].pk) if has_more and page else None

        return Response(
            {
                "username": clean_username,
                "source": source,
                "results": [_reel_payload(request, reel) for reel in page],
                "next_cursor": next_cursor,
            }
        )
