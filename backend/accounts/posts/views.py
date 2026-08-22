from django.db.models import Count, Exists, OuterRef, Q
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ..models import Follow, Like, Notification, Post
from ..privacy import accessible_content_owner_filter
from ..serializers import PostSerializer
from ..sharing_models import Repost
from ..trust_safety import blocked_user_ids
from ..views import create_notification

FEED_PAGE_SIZE = 20


def post_queryset(request):
    blocked_ids = blocked_user_ids(request.user)
    return Post.objects.select_related("author").annotate(
        likes_count_value=Count(
            "likes",
            filter=~Q(likes__user_id__in=blocked_ids),
            distinct=True,
        ),
        comments_count_value=Count(
            "comments",
            filter=~Q(comments__author_id__in=blocked_ids),
            distinct=True,
        ),
        reposts_count_value=Count(
            "reposts",
            filter=~Q(reposts__user_id__in=blocked_ids),
            distinct=True,
        ),
        is_liked_value=Exists(
            Like.objects.filter(post_id=OuterRef("pk"), user=request.user)
        ),
        is_reposted_value=Exists(
            Repost.objects.filter(post_id=OuterRef("pk"), user=request.user)
        ),
    )


def public_post_queryset(request):
    return (
        post_queryset(request)
        .filter(author__is_active=True)
        .exclude(author_id__in=blocked_user_ids(request.user))
        .filter(accessible_content_owner_filter(request.user))
    )


def visible_post_queryset(request):
    followed_ids = Follow.objects.filter(follower=request.user).values_list(
        "following_id",
        flat=True,
    )
    return public_post_queryset(request).filter(
        Q(author=request.user) | Q(author_id__in=followed_ids)
    )


def paginated_feed_response(request, queryset):
    cursor = request.query_params.get("cursor", "").strip()
    if cursor:
        try:
            cursor_id = int(cursor)
        except ValueError:
            return Response(
                {"detail": "Invalid feed cursor."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        queryset = queryset.filter(id__lt=cursor_id)

    page_with_extra = list(queryset.order_by("-id")[: FEED_PAGE_SIZE + 1])
    has_more = len(page_with_extra) > FEED_PAGE_SIZE
    page = page_with_extra[:FEED_PAGE_SIZE]
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


class PostsView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = PostSerializer(
            data=request.data,
            context={"request": request},
        )
        serializer.is_valid(raise_exception=True)
        post = serializer.save(author=request.user)
        post = post_queryset(request).get(pk=post.pk)
        return Response(
            PostSerializer(post, context={"request": request}).data,
            status=status.HTTP_201_CREATED,
        )


class FeedView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return paginated_feed_response(request, visible_post_queryset(request))


class PostDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, post_id):
        post = get_object_or_404(public_post_queryset(request), pk=post_id)
        return Response(PostSerializer(post, context={"request": request}).data)

    def delete(self, request, post_id):
        post = get_object_or_404(Post, pk=post_id, author=request.user)
        post.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class PostLikeView(APIView):
    permission_classes = [IsAuthenticated]

    def _post(self, request, post_id):
        return get_object_or_404(public_post_queryset(request), pk=post_id)

    def post(self, request, post_id):
        post = self._post(request, post_id)
        _, created = Like.objects.get_or_create(post_id=post.pk, user=request.user)
        if created:
            create_notification(
                recipient=post.author,
                actor=request.user,
                kind=Notification.Kind.LIKE,
                dedupe_key=f"like:{request.user.pk}:{post.pk}",
                post=post,
            )

        refreshed = post_queryset(request).get(pk=post.pk)
        return Response(PostSerializer(refreshed, context={"request": request}).data)

    def delete(self, request, post_id):
        post = self._post(request, post_id)
        Like.objects.filter(post_id=post.pk, user=request.user).delete()
        refreshed = post_queryset(request).get(pk=post.pk)
        return Response(PostSerializer(refreshed, context={"request": request}).data)
