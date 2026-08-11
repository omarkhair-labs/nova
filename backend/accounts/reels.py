from django.db.models import Count, Exists, OuterRef, Q
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Follow, Notification
from .push import send_notification_push
from .reels_models import Reel, ReelComment, ReelLike, ReelRepost
from .reels_ranking import encode_rank_cursor, parse_rank_cursor, ranked_reels_for
from .trust_safety import blocked_user_ids, users_blocked


REELS_PAGE_SIZE = 24
MAX_REEL_VIDEO_BYTES = 120 * 1024 * 1024
MAX_REEL_CAPTION_LENGTH = 500
MAX_REEL_COMMENT_LENGTH = 300
REEL_LIKE_NOTIFICATION = "reel_like"
REEL_COMMENT_NOTIFICATION = "reel_comment"
REEL_REPOST_NOTIFICATION = "reel_repost"


def _absolute_media_url(request, field):
    if not field:
        return ""
    return request.build_absolute_uri(field.url)


def _author_payload(request, user):
    return {
        "id": user.pk,
        "username": user.username,
        "name": user.name,
        "avatar_url": _absolute_media_url(request, user.avatar),
    }


def visible_reels_for(user):
    blocked_ids = blocked_user_ids(user)
    followed_ids = Follow.objects.filter(follower=user).values_list("following_id", flat=True)
    liked = ReelLike.objects.filter(reel_id=OuterRef("pk"), user=user)
    reposted = ReelRepost.objects.filter(reel_id=OuterRef("pk"), user=user)
    return (
        Reel.objects.select_related("author")
        .filter(author__is_active=True)
        .exclude(author_id__in=blocked_ids)
        .filter(
            Q(author=user)
            | Q(author_id__in=followed_ids)
            | Q(author__account_privacy__isnull=True)
            | Q(author__account_privacy__is_private=False)
        )
        .annotate(
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
            is_liked_value=Exists(liked),
            is_reposted_value=Exists(reposted),
        )
        .distinct()
    )


def _visible_reel(request, reel_id):
    return get_object_or_404(visible_reels_for(request.user), pk=reel_id)


def _reel_payload(request, reel):
    likes_count = getattr(reel, "likes_count_value", None)
    comments_count = getattr(reel, "comments_count_value", None)
    reposts_count = getattr(reel, "reposts_count_value", None)
    is_liked = getattr(reel, "is_liked_value", None)
    is_reposted = getattr(reel, "is_reposted_value", None)
    if likes_count is None:
        likes_count = reel.likes.count()
    if comments_count is None:
        comments_count = reel.comments.count()
    if reposts_count is None:
        reposts_count = reel.reposts.count()
    if is_liked is None:
        is_liked = reel.likes.filter(user=request.user).exists()
    if is_reposted is None:
        is_reposted = reel.reposts.filter(user=request.user).exists()
    return {
        "id": reel.pk,
        "author": _author_payload(request, reel.author),
        "video_url": _absolute_media_url(request, reel.video),
        "caption": reel.caption,
        "created_at": reel.created_at.isoformat(),
        "is_mine": reel.author_id == request.user.pk,
        "likes_count": likes_count,
        "comments_count": comments_count,
        "reposts_count": reposts_count,
        "is_liked": bool(is_liked),
        "is_reposted": bool(is_reposted),
    }


def _comment_payload(request, comment):
    return {
        "id": comment.pk,
        "author": _author_payload(request, comment.author),
        "body": comment.body,
        "created_at": comment.created_at.isoformat(),
        "is_mine": comment.author_id == request.user.pk,
    }


def _create_reel_notification(*, reel, actor, kind, dedupe_key):
    if reel.author_id == actor.pk or users_blocked(reel.author, actor):
        return None
    notification, created = Notification.objects.get_or_create(
        dedupe_key=dedupe_key,
        defaults={
            "recipient": reel.author,
            "actor": actor,
            "kind": kind,
        },
    )
    if created:
        send_notification_push(notification)
    return notification


class ReelFeedView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        raw_cursor = str(request.query_params.get("cursor") or "").strip()
        try:
            offset, legacy_pk_cursor = parse_rank_cursor(raw_cursor)
        except ValueError:
            return Response(
                {"detail": "Invalid Reels cursor."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        queryset = visible_reels_for(request.user)
        if legacy_pk_cursor is not None:
            queryset = queryset.filter(pk__lt=legacy_pk_cursor)
        queryset = ranked_reels_for(request.user, queryset)

        rows = list(queryset[offset : offset + REELS_PAGE_SIZE + 1])
        has_more = len(rows) > REELS_PAGE_SIZE
        page = rows[:REELS_PAGE_SIZE]
        next_cursor = (
            encode_rank_cursor(offset + REELS_PAGE_SIZE)
            if has_more and page
            else None
        )
        return Response(
            {
                "results": [_reel_payload(request, reel) for reel in page],
                "next_cursor": next_cursor,
            }
        )

    def post(self, request):
        video = request.FILES.get("video")
        caption = str(request.data.get("caption") or "").strip()
        if video is None:
            return Response(
                {"detail": "Choose a video for your Reel."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(caption) > MAX_REEL_CAPTION_LENGTH:
            return Response(
                {"detail": f"Reel caption must be {MAX_REEL_CAPTION_LENGTH} characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        content_type = str(getattr(video, "content_type", "") or "").lower()
        if not content_type.startswith("video/"):
            return Response(
                {"detail": "Reels support video files only."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if video.size > MAX_REEL_VIDEO_BYTES:
            return Response(
                {"detail": "Reel video must be 120 MB or smaller."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        reel = Reel.objects.create(author=request.user, video=video, caption=caption)
        return Response(_reel_payload(request, reel), status=status.HTTP_201_CREATED)


class ReelDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, reel_id):
        return Response(_reel_payload(request, _visible_reel(request, reel_id)))

    def delete(self, request, reel_id):
        reel = get_object_or_404(Reel, pk=reel_id, author=request.user)
        reel.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class ReelLikeView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, reel_id):
        reel = _visible_reel(request, reel_id)
        _, created = ReelLike.objects.get_or_create(reel=reel, user=request.user)
        if created:
            _create_reel_notification(
                reel=reel,
                actor=request.user,
                kind=REEL_LIKE_NOTIFICATION,
                dedupe_key=f"reel_like:{request.user.pk}:{reel.pk}",
            )
        reel = _visible_reel(request, reel_id)
        return Response(_reel_payload(request, reel))

    def delete(self, request, reel_id):
        reel = _visible_reel(request, reel_id)
        ReelLike.objects.filter(reel=reel, user=request.user).delete()
        reel = _visible_reel(request, reel_id)
        return Response(_reel_payload(request, reel))


class ReelRepostView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, reel_id):
        reel = _visible_reel(request, reel_id)
        _, created = ReelRepost.objects.get_or_create(reel=reel, user=request.user)
        if created:
            _create_reel_notification(
                reel=reel,
                actor=request.user,
                kind=REEL_REPOST_NOTIFICATION,
                dedupe_key=f"reel_repost:{request.user.pk}:{reel.pk}",
            )
        reel = _visible_reel(request, reel_id)
        return Response(_reel_payload(request, reel))

    def delete(self, request, reel_id):
        reel = _visible_reel(request, reel_id)
        ReelRepost.objects.filter(reel=reel, user=request.user).delete()
        reel = _visible_reel(request, reel_id)
        return Response(_reel_payload(request, reel))


class ReelCommentsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, reel_id):
        reel = _visible_reel(request, reel_id)
        comments = list(reel.comments.select_related("author").order_by("created_at", "id")[:250])
        return Response({"results": [_comment_payload(request, comment) for comment in comments]})

    def post(self, request, reel_id):
        reel = _visible_reel(request, reel_id)
        body = str(request.data.get("body") or "").strip()
        if not body:
            return Response(
                {"detail": "Write a comment first."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(body) > MAX_REEL_COMMENT_LENGTH:
            return Response(
                {"detail": f"Reel comments must be {MAX_REEL_COMMENT_LENGTH} characters or fewer."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        comment = ReelComment.objects.create(reel=reel, author=request.user, body=body)
        _create_reel_notification(
            reel=reel,
            actor=request.user,
            kind=REEL_COMMENT_NOTIFICATION,
            dedupe_key=f"reel_comment:{comment.pk}:{reel.pk}",
        )
        reel = _visible_reel(request, reel_id)
        return Response(
            {
                "comment": _comment_payload(request, comment),
                "reel": _reel_payload(request, reel),
            },
            status=status.HTTP_201_CREATED,
        )


class ReelCommentDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, comment_id):
        comment = get_object_or_404(
            ReelComment.objects.select_related("reel"),
            pk=comment_id,
            author=request.user,
        )
        reel_id = comment.reel_id
        comment.delete()
        reel = _visible_reel(request, reel_id)
        return Response({"reel": _reel_payload(request, reel)})
