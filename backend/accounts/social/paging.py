from django.db.models import OuterRef, Q, Subquery
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import User
from .privacy import can_view_user_content
from .serializers import PersonSerializer, PostSerializer
from .sharing_models import Repost
from .trust_safety import active_person_for, blocked_user_ids, visible_active_users_for
from .views import public_post_queryset

SOCIAL_PAGE_SIZE = 24
PROFILE_POST_PAGE_SIZE = 24
PROFILE_REPOST_PAGE_SIZE = 24


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

    discovery_filter = request.query_params.get("filter", "").strip().lower()
    if discovery_filter == "nearby":
        location = request.user.location.strip()
        queryset = queryset.filter(location__iexact=location) if location else queryset.none()
    elif discovery_filter == "interests":
        interests = [str(value).strip() for value in request.user.interests if str(value).strip()]
        if not interests:
            queryset = queryset.none()
        else:
            interest_query = Q()
            for interest in interests:
                interest_query |= Q(interests__icontains=interest)
            queryset = queryset.filter(interest_query)
    elif discovery_filter == "verified":
        queryset = queryset.filter(is_verified=True)
    elif discovery_filter == "new":
        queryset = queryset.order_by("-date_joined", "-id")
        page = list(queryset.select_related("account_privacy")[:SOCIAL_PAGE_SIZE])
        return Response(
            {
                "results": PersonSerializer(page, many=True, context={"request": request}).data,
                "next_cursor": None,
            }
        )
    elif discovery_filter not in {"", "people"}:
        return Response(
            {"detail": "Unknown discovery filter."},
            status=status.HTTP_400_BAD_REQUEST,
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
        queryset.select_related("account_privacy").order_by("username", "id")[: SOCIAL_PAGE_SIZE + 1]
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


def _profile_content_allowed(request, person, noun):
    if can_view_user_content(request.user, person):
        return None
    return Response(
        {"detail": f"This account is private. Follow to see {noun}."},
        status=status.HTTP_403_FORBIDDEN,
    )


def _positive_id_cursor(request, error_message):
    raw_cursor = request.query_params.get("cursor", "").strip()
    if not raw_cursor:
        return None
    try:
        cursor_id = int(raw_cursor)
    except ValueError:
        cursor_id = 0
    if cursor_id <= 0:
        return Response(
            {"detail": error_message},
            status=status.HTTP_400_BAD_REQUEST,
        )
    return cursor_id


class PaginatedPeopleView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return _people_page(request, visible_active_users_for(request.user))


class SocialConnectionsView(APIView):
    permission_classes = [IsAuthenticated]
    mode = None

    def get(self, request, username):
        person = active_person_for(request.user, username)
        if not can_view_user_content(request.user, person):
            return Response(
                {"detail": "Follow this private account to see its connections."},
                status=status.HTTP_403_FORBIDDEN,
            )

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
        denied = _profile_content_allowed(request, person, "posts")
        if denied is not None:
            return denied

        posts = public_post_queryset(request).filter(author=person)
        cursor = _positive_id_cursor(request, "Invalid profile-post cursor.")
        if isinstance(cursor, Response):
            return cursor
        if cursor is not None:
            posts = posts.filter(id__lt=cursor)

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


class PaginatedPersonRepostsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, username):
        person = active_person_for(request.user, username)
        denied = _profile_content_allowed(request, person, "reposts")
        if denied is not None:
            return denied

        repost_id = Repost.objects.filter(
            user=person,
            post_id=OuterRef("pk"),
        ).values("id")[:1]
        posts = (
            public_post_queryset(request)
            .annotate(profile_repost_id=Subquery(repost_id))
            .filter(profile_repost_id__isnull=False)
        )

        cursor = _positive_id_cursor(request, "Invalid profile-repost cursor.")
        if isinstance(cursor, Response):
            return cursor
        if cursor is not None:
            posts = posts.filter(profile_repost_id__lt=cursor)

        page_with_extra = list(
            posts.order_by("-profile_repost_id")[: PROFILE_REPOST_PAGE_SIZE + 1]
        )
        has_more = len(page_with_extra) > PROFILE_REPOST_PAGE_SIZE
        page = page_with_extra[:PROFILE_REPOST_PAGE_SIZE]
        next_cursor = (
            str(page[-1].profile_repost_id)
            if has_more and page
            else None
        )

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
