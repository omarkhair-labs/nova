from django.db.models import Q
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
        if not can_view_user_content(request.user, person):
            return Response(
                {"detail": "This account is private. Follow to see posts."},
                status=status.HTTP_403_FORBIDDEN,
            )

        content_kind = request.query_params.get("kind", "posts").strip().lower()
        if content_kind not in {"posts", "reposts"}:
            return Response(
                {"detail": "Unsupported profile content kind."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        raw_cursor = request.query_params.get("cursor", "").strip()
        cursor_id = None
        if raw_cursor:
            try:
                cursor_id = int(raw_cursor)
            except ValueError:
                return Response(
                    {"detail": "Invalid profile-content cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if cursor_id <= 0:
                return Response(
                    {"detail": "Invalid profile-content cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        if content_kind == "reposts":
            visible_posts = public_post_queryset(request)
            reposts = (
                Repost.objects.filter(user=person, post__in=visible_posts)
                .select_related("post", "post__author")
                .order_by("-id")
            )
            if cursor_id is not None:
                reposts = reposts.filter(id__lt=cursor_id)

            page_with_extra = list(reposts[: PROFILE_POST_PAGE_SIZE + 1])
            has_more = len(page_with_extra) > PROFILE_POST_PAGE_SIZE
            page_reposts = page_with_extra[:PROFILE_POST_PAGE_SIZE]
            page = [item.post for item in page_reposts]
            next_cursor = (
                str(page_reposts[-1].id)
                if has_more and page_reposts
                else None
            )
        else:
            posts = public_post_queryset(request).filter(author=person)
            if cursor_id is not None:
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
