from django.contrib.auth import get_user_model
from django.db.models import Count, Exists, OuterRef, Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView

from .models import Comment, Follow, Like, Notification, Post
from .serializers import (
    CommentSerializer,
    NotificationSerializer,
    NovaTokenObtainPairSerializer,
    PersonSerializer,
    PostSerializer,
    RegisterSerializer,
    UserSerializer,
)

User = get_user_model()
FEED_PAGE_SIZE = 20
NOTIFICATION_PAGE_SIZE = 30


def post_queryset(request):
    return Post.objects.select_related("author").annotate(
        likes_count_value=Count("likes", distinct=True),
        comments_count_value=Count("comments", distinct=True),
        is_liked_value=Exists(
            Like.objects.filter(post_id=OuterRef("pk"), user=request.user)
        ),
    )


def public_post_queryset(request):
    return post_queryset(request).filter(author__is_active=True)


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


def create_notification(*, recipient, actor, kind, dedupe_key, post=None, comment=None):
    if recipient.pk == actor.pk:
        return None

    notification, _ = Notification.objects.get_or_create(
        dedupe_key=dedupe_key,
        defaults={
            "recipient": recipient,
            "actor": actor,
            "kind": kind,
            "post": post,
            "comment": comment,
        },
    )
    return notification


class RegisterView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        refresh = RefreshToken.for_user(user)
        return Response(
            {
                "access": str(refresh.access_token),
                "refresh": str(refresh),
                "user": UserSerializer(user, context={"request": request}).data,
            },
            status=status.HTTP_201_CREATED,
        )


class NovaTokenObtainPairView(TokenObtainPairView):
    permission_classes = [AllowAny]
    authentication_classes = []
    serializer_class = NovaTokenObtainPairSerializer


class MeView(generics.RetrieveUpdateAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = UserSerializer

    def get_object(self):
        return self.request.user


class PeopleView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        query = request.query_params.get("q", "").strip()
        people = User.objects.filter(is_active=True).exclude(pk=request.user.pk)

        if query:
            people = people.filter(
                Q(username__icontains=query) | Q(name__icontains=query)
            )

        people = people.order_by("username")[:50]
        serializer = PersonSerializer(
            people,
            many=True,
            context={"request": request},
        )
        return Response({"results": serializer.data})


class PersonView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, username):
        person = get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.lower(),
        )
        return Response(
            PersonSerializer(person, context={"request": request}).data
        )


class PersonPostsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, username):
        person = get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.lower(),
        )
        posts = public_post_queryset(request).filter(author=person).order_by(
            "-created_at",
            "-id",
        )[:50]
        return Response(
            {
                "results": PostSerializer(
                    posts,
                    many=True,
                    context={"request": request},
                ).data
            }
        )


class FollowView(APIView):
    permission_classes = [IsAuthenticated]

    def _person(self, username):
        return get_object_or_404(
            User.objects.filter(is_active=True),
            username=username.lower(),
        )

    def post(self, request, username):
        person = self._person(username)
        if person.pk == request.user.pk:
            return Response(
                {"detail": "You can't follow yourself."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        _, created = Follow.objects.get_or_create(
            follower=request.user,
            following=person,
        )
        if created:
            create_notification(
                recipient=person,
                actor=request.user,
                kind=Notification.Kind.FOLLOW,
                dedupe_key=f"follow:{request.user.pk}:{person.pk}",
            )

        return Response(
            PersonSerializer(person, context={"request": request}).data
        )

    def delete(self, request, username):
        person = self._person(username)
        Follow.objects.filter(follower=request.user, following=person).delete()
        return Response(
            PersonSerializer(person, context={"request": request}).data
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


class PostCommentsView(APIView):
    permission_classes = [IsAuthenticated]

    def _post(self, request, post_id):
        return get_object_or_404(public_post_queryset(request), pk=post_id)

    def get(self, request, post_id):
        post = self._post(request, post_id)
        comments = post.comments.select_related("author").order_by("created_at", "id")[:100]
        return Response(
            {
                "results": CommentSerializer(
                    comments,
                    many=True,
                    context={"request": request},
                ).data
            }
        )

    def post(self, request, post_id):
        post = self._post(request, post_id)
        serializer = CommentSerializer(
            data=request.data,
            context={"request": request},
        )
        serializer.is_valid(raise_exception=True)
        comment = serializer.save(post=post, author=request.user)
        create_notification(
            recipient=post.author,
            actor=request.user,
            kind=Notification.Kind.COMMENT,
            dedupe_key=f"comment:{comment.pk}",
            post=post,
            comment=comment,
        )

        refreshed = post_queryset(request).get(pk=post.pk)
        return Response(
            {
                "comment": CommentSerializer(
                    comment,
                    context={"request": request},
                ).data,
                "post": PostSerializer(
                    refreshed,
                    context={"request": request},
                ).data,
            },
            status=status.HTTP_201_CREATED,
        )


class CommentDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, comment_id):
        comment = get_object_or_404(
            Comment.objects.select_related("post"),
            pk=comment_id,
            author=request.user,
        )
        post_id = comment.post_id
        comment.delete()
        refreshed = post_queryset(request).get(pk=post_id)
        return Response(
            {
                "post": PostSerializer(
                    refreshed,
                    context={"request": request},
                ).data
            }
        )


class NotificationsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        notifications = Notification.objects.filter(
            recipient=request.user,
        ).select_related("actor", "post", "comment")

        cursor = request.query_params.get("cursor", "").strip()
        if cursor:
            try:
                cursor_id = int(cursor)
            except ValueError:
                return Response(
                    {"detail": "Invalid notification cursor."},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            notifications = notifications.filter(id__lt=cursor_id)

        page_with_extra = list(
            notifications.order_by("-id")[: NOTIFICATION_PAGE_SIZE + 1]
        )
        has_more = len(page_with_extra) > NOTIFICATION_PAGE_SIZE
        page = page_with_extra[:NOTIFICATION_PAGE_SIZE]
        next_cursor = str(page[-1].id) if has_more and page else None
        unread_count = Notification.objects.filter(
            recipient=request.user,
            read_at__isnull=True,
        ).count()

        return Response(
            {
                "results": NotificationSerializer(
                    page,
                    many=True,
                    context={"request": request},
                ).data,
                "next_cursor": next_cursor,
                "unread_count": unread_count,
            }
        )


class NotificationsReadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        marked_read = Notification.objects.filter(
            recipient=request.user,
            read_at__isnull=True,
        ).update(read_at=timezone.now())

        return Response(
            {
                "marked_read": marked_read,
                "unread_count": 0,
            }
        )
