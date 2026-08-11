from collections import defaultdict

from django.db.models import Q
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .comment_reply_models import PostCommentReply, ReelCommentReply
from .models import Comment, Notification
from .push import send_notification_push
from .reels import (
    _author_payload,
    _create_reel_notification,
    _reel_payload,
    _visible_reel,
    visible_reels_for,
)
from .reels_models import ReelComment
from .serializers import CommentSerializer, PostAuthorSerializer, PostSerializer
from .trust_safety import blocked_user_ids, users_blocked
from .views import create_notification, post_queryset, public_post_queryset


MAX_COMMENT_LENGTH = 300
POST_REPLY_NOTIFICATION = "comment_reply"
REEL_REPLY_NOTIFICATION = "reel_reply"


def _clean_body(request):
    body = str(request.data.get("body") or "").strip()
    if not body:
        return None, Response(
            {"detail": "Comment can't be empty."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    if len(body) > MAX_COMMENT_LENGTH:
        return None, Response(
            {"detail": f"Comments must be {MAX_COMMENT_LENGTH} characters or fewer."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    return body, None


def _parent_id(request):
    raw = request.data.get("parent_id")
    if raw in (None, "", 0, "0"):
        return None, None
    try:
        value = int(raw)
    except (TypeError, ValueError):
        return None, Response(
            {"detail": "Invalid parent comment."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    if value <= 0:
        return None, Response(
            {"detail": "Invalid parent comment."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    return value, None


def _post_reply_payload(request, reply):
    return {
        "id": reply.pk,
        "author": PostAuthorSerializer(reply.author, context={"request": request}).data,
        "body": reply.body,
        "created_at": reply.created_at.isoformat(),
        "is_mine": reply.author_id == request.user.pk,
        "parent_id": reply.comment_id,
        "replies_count": 0,
        "replies": [],
    }


def _post_comment_payload(request, comment, replies=None):
    visible_replies = list(replies or [])
    data = CommentSerializer(comment, context={"request": request}).data
    data["parent_id"] = None
    data["replies_count"] = len(visible_replies)
    data["replies"] = [_post_reply_payload(request, reply) for reply in visible_replies]
    return data


def _post_threads(request, post):
    blocked_ids = blocked_user_ids(request.user)
    comments = list(
        post.comments.select_related("author")
        .exclude(author_id__in=blocked_ids)
        .order_by("created_at", "id")[:100]
    )
    if not comments:
        return []
    reply_rows = (
        PostCommentReply.objects.select_related("author")
        .filter(comment_id__in=[comment.pk for comment in comments], author__is_active=True)
        .exclude(author_id__in=blocked_ids)
        .order_by("created_at", "id")
    )
    grouped = defaultdict(list)
    for reply in reply_rows:
        grouped[reply.comment_id].append(reply)
    return [
        _post_comment_payload(request, comment, grouped.get(comment.pk, []))
        for comment in comments
    ]


def _reel_reply_payload(request, reply):
    return {
        "id": reply.pk,
        "author": _author_payload(request, reply.author),
        "body": reply.body,
        "created_at": reply.created_at.isoformat(),
        "is_mine": reply.author_id == request.user.pk,
        "parent_id": reply.comment_id,
        "replies_count": 0,
        "replies": [],
    }


def _reel_comment_payload(request, comment, replies=None):
    visible_replies = list(replies or [])
    return {
        "id": comment.pk,
        "author": _author_payload(request, comment.author),
        "body": comment.body,
        "created_at": comment.created_at.isoformat(),
        "is_mine": comment.author_id == request.user.pk,
        "parent_id": None,
        "replies_count": len(visible_replies),
        "replies": [_reel_reply_payload(request, reply) for reply in visible_replies],
    }


def _reel_threads(request, reel):
    blocked_ids = blocked_user_ids(request.user)
    comments = list(
        reel.comments.select_related("author")
        .filter(author__is_active=True)
        .exclude(author_id__in=blocked_ids)
        .order_by("created_at", "id")[:250]
    )
    if not comments:
        return []
    reply_rows = (
        ReelCommentReply.objects.select_related("author")
        .filter(comment_id__in=[comment.pk for comment in comments], author__is_active=True)
        .exclude(author_id__in=blocked_ids)
        .order_by("created_at", "id")
    )
    grouped = defaultdict(list)
    for reply in reply_rows:
        grouped[reply.comment_id].append(reply)
    return [
        _reel_comment_payload(request, comment, grouped.get(comment.pk, []))
        for comment in comments
    ]


def _create_reel_reply_notification(*, reel, parent, reply, actor):
    recipient = parent.author
    if recipient.pk == actor.pk or users_blocked(recipient, actor):
        return None
    notification, created = Notification.objects.get_or_create(
        dedupe_key=f"reel_reply:{reply.pk}:{reel.pk}",
        defaults={
            "recipient": recipient,
            "actor": actor,
            "kind": REEL_REPLY_NOTIFICATION,
        },
    )
    if created:
        send_notification_push(notification)
    return notification


class ThreadPostCommentsView(APIView):
    permission_classes = [IsAuthenticated]

    def _post(self, request, post_id):
        return get_object_or_404(public_post_queryset(request), pk=post_id)

    def get(self, request, post_id):
        post = self._post(request, post_id)
        return Response({"results": _post_threads(request, post)})

    def post(self, request, post_id):
        post = self._post(request, post_id)
        body, body_error = _clean_body(request)
        if body_error is not None:
            return body_error
        parent_id, parent_error = _parent_id(request)
        if parent_error is not None:
            return parent_error

        if parent_id is None:
            comment = Comment.objects.create(post=post, author=request.user, body=body)
            create_notification(
                recipient=post.author,
                actor=request.user,
                kind=Notification.Kind.COMMENT,
                dedupe_key=f"comment:{comment.pk}",
                post=post,
                comment=comment,
            )
            comment_payload = _post_comment_payload(request, comment, [])
        else:
            parent = get_object_or_404(
                Comment.objects.select_related("author").filter(post=post),
                pk=parent_id,
            )
            if parent.author_id in blocked_user_ids(request.user):
                return Response(
                    {"detail": "You can't interact with this comment."},
                    status=status.HTTP_403_FORBIDDEN,
                )
            reply = PostCommentReply.objects.create(
                comment=parent,
                author=request.user,
                body=body,
            )
            create_notification(
                recipient=parent.author,
                actor=request.user,
                kind=POST_REPLY_NOTIFICATION,
                dedupe_key=f"comment_reply:{reply.pk}",
                post=post,
                comment=parent,
            )
            comment_payload = _post_reply_payload(request, reply)

        refreshed = post_queryset(request).get(pk=post.pk)
        return Response(
            {
                "comment": comment_payload,
                "post": PostSerializer(refreshed, context={"request": request}).data,
            },
            status=status.HTTP_201_CREATED,
        )


class ThreadCommentDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, comment_id):
        comment = get_object_or_404(
            Comment.objects.select_related("post"),
            pk=comment_id,
            author=request.user,
        )
        post_id = comment.post_id
        reply_ids = list(comment.thread_replies.values_list("id", flat=True))
        if reply_ids:
            Notification.objects.filter(
                kind=POST_REPLY_NOTIFICATION,
                dedupe_key__in=[f"comment_reply:{reply_id}" for reply_id in reply_ids],
            ).delete()
        comment.delete()
        refreshed = post_queryset(request).get(pk=post_id)
        return Response(
            {"post": PostSerializer(refreshed, context={"request": request}).data}
        )


class PostCommentReplyDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, reply_id):
        reply = get_object_or_404(
            PostCommentReply.objects.select_related("comment__post"),
            pk=reply_id,
            author=request.user,
        )
        post_id = reply.comment.post_id
        Notification.objects.filter(
            kind=POST_REPLY_NOTIFICATION,
            dedupe_key=f"comment_reply:{reply.pk}",
        ).delete()
        reply.delete()
        refreshed = post_queryset(request).get(pk=post_id)
        return Response(
            {"post": PostSerializer(refreshed, context={"request": request}).data}
        )


class ThreadReelCommentsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, reel_id):
        reel = _visible_reel(request, reel_id)
        return Response({"results": _reel_threads(request, reel)})

    def post(self, request, reel_id):
        reel = _visible_reel(request, reel_id)
        body, body_error = _clean_body(request)
        if body_error is not None:
            return body_error
        parent_id, parent_error = _parent_id(request)
        if parent_error is not None:
            return parent_error

        if parent_id is None:
            comment = ReelComment.objects.create(reel=reel, author=request.user, body=body)
            _create_reel_notification(
                reel=reel,
                actor=request.user,
                kind="reel_comment",
                dedupe_key=f"reel_comment:{comment.pk}:{reel.pk}",
            )
            comment_payload = _reel_comment_payload(request, comment, [])
        else:
            parent = get_object_or_404(
                ReelComment.objects.select_related("author").filter(reel=reel),
                pk=parent_id,
            )
            if parent.author_id in blocked_user_ids(request.user):
                return Response(
                    {"detail": "You can't interact with this comment."},
                    status=status.HTTP_403_FORBIDDEN,
                )
            reply = ReelCommentReply.objects.create(
                comment=parent,
                author=request.user,
                body=body,
            )
            _create_reel_reply_notification(
                reel=reel,
                parent=parent,
                reply=reply,
                actor=request.user,
            )
            comment_payload = _reel_reply_payload(request, reply)

        refreshed = visible_reels_for(request.user).get(pk=reel.pk)
        return Response(
            {
                "comment": comment_payload,
                "reel": _reel_payload(request, refreshed),
            },
            status=status.HTTP_201_CREATED,
        )


class ThreadReelCommentDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, comment_id):
        comment = get_object_or_404(
            ReelComment.objects.select_related("reel"),
            pk=comment_id,
            author=request.user,
        )
        reel_id = comment.reel_id
        reply_ids = list(comment.thread_replies.values_list("id", flat=True))
        Notification.objects.filter(
            Q(kind="reel_comment", dedupe_key=f"reel_comment:{comment.pk}:{reel_id}")
            | Q(
                kind=REEL_REPLY_NOTIFICATION,
                dedupe_key__in=[f"reel_reply:{reply_id}:{reel_id}" for reply_id in reply_ids],
            )
        ).delete()
        comment.delete()
        reel = visible_reels_for(request.user).get(pk=reel_id)
        return Response({"reel": _reel_payload(request, reel)})


class ReelCommentReplyDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, reply_id):
        reply = get_object_or_404(
            ReelCommentReply.objects.select_related("comment__reel"),
            pk=reply_id,
            author=request.user,
        )
        reel_id = reply.comment.reel_id
        Notification.objects.filter(
            kind=REEL_REPLY_NOTIFICATION,
            dedupe_key=f"reel_reply:{reply.pk}:{reel_id}",
        ).delete()
        reply.delete()
        reel = visible_reels_for(request.user).get(pk=reel_id)
        return Response({"reel": _reel_payload(request, reel)})
