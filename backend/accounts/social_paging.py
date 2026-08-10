from django.db.models import Q
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import User
from .serializers import PersonSerializer, PostSerializer
from .trust_safety import active_person_for, blocked_user_ids, visible_active_users_for
from .views import public_post_queryset

SOCIAL_PAGE_SIZE = 24
PROFILE_POST_PAGE_SIZE = 24


def _people_cursor(request):
    raw = request.query_params.get("cursor", "").strip()
    if not raw:
        return None
    try:
        username, raw_id = raw.rsplit(":", 1)
        person_id = int(raw_id)
    except (TypeError, ValueError):
        return Response(
            {"detail": "Invalid people cursor."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    if not username or person_id <= 0:
        return Response(
            {"detail": "Invalid people cursor."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    return username, person_id


def _people_page(request, queryset):
    query = request.query_params.get("q", "").strip()
    if query:
        queryset = queryset.filter(
            Q(username__icontains=query) | Q(name__icontains=query)
        )

    cursor = _people_cursor(request)
    if isinstance(cursor, Response):
        return cursor
    if cursor is not None:
        username, person_id = cursor
        queryset = queryset.filter(
            Q(username__gt=username) | Q(username=username, id__gt=person_id)
        )

    page_with_extra = list(
        queryset.order_by("username", "id")[: SOCIAL_PAGE_SIZE + 1]
    )
    has_more = len(page_with_extra) > SOCIAL_PAGE_SIZE
    page = page_with_extra[:SOCIAL_PAGE_SIZE]
    next_cursor = (
        f"{page[-1].username}:{page[-1].id}"
        if has_more and page
        else None
    )
    return Response(
        {
            "results": PersonSerializer(
                page,
                many=True,
                context={"request": request},
            ).data,
            "next_cursor": next_cursor,
        }
    )


class PaginatedPeopleView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return _people_page(request, visible_active_users_for(request.user))


class SocialConnectionsView(APIView):
    permission_classes = [IsAuthenticated]
    mode = None

    def get(self, request, username):
        person = active_person_for(request.user, username)
        blocked_ids = blocked_user_ids(request.user)
        visible_people = User.objects.filter(is_active=True).exclude(pk__in=blocked_ids)

        if self.mode == "followers":
            people = visible_people.filter(
                following_relationships__following=person,
            )
        elif self.mode == "following":
            people = visible_people.filter(
                follower_relationships__follower=person,
            )
        else:
            return Response(status=status.HTTP_404_NOT_FOUND)

        return _people_page(request, people.distinct())


class FollowersView(SocialConnectionsView):
    mode = "followers"


class FollowingView(SocialConnectionsView):
    mode = "following"


class PaginatedPersonPostsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, username):
        person = active_person_for(request.user, username)
        posts = public_post_queryset(request).filter(author=person)

        raw_cursor = request.query_params.get("cursor", "").strip()
        if raw_cursor:
            try:
                cursor_id = int(raw_cursor)
            except ValueError:
                return Response(
                    {"detail": "Invalid profile-post cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            posts = posts.filter(id__lt=cursor_id)

        page_with_extra = list(
            posts.order_by("-id")[: PROFILE_POST_PAGE_SIZE + 1]
        )
        has_more = len(page_with_extra) > PROFILE_POST_PAGE_SIZE
        page = page_with_extra[:PROFILE_POST_PAGE_SIZE]
        next_cursor = str(page[-1].id) if has_more and page else None
        return Response(
            {
                "results": PostSerializer(
                    page,
                    many=True,
                    context={"request": request},
                ).data,
                "next_cursor": next_cursor,
            }
        )
