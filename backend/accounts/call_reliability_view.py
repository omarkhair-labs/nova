from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from .calls import (
    ACTIVE_CALL_STATUSES,
    call_is_live,
    clear_call_liveness,
    expire_stale_call_locks,
    serialize_call,
)
from .models import CallSession, Conversation, User
from .push import send_call_push, send_call_state_push
from .trust_safety import users_blocked


class ReliableCallSessionCreateView(APIView):
    """Create one outgoing call without letting a dead retry lock both users."""

    def post(self, request):
        try:
            conversation_id = int(request.data.get("conversation_id"))
        except (TypeError, ValueError):
            return Response(
                {"detail": "A valid conversation is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        kind = str(request.data.get("kind") or "").strip().lower()
        if kind not in CallSession.Kind.values:
            return Response(
                {"detail": "Call kind must be audio or video."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        conversation = get_object_or_404(
            Conversation.objects.select_related("participant_one", "participant_two"),
            pk=conversation_id,
        )
        if conversation.kind != Conversation.Kind.DIRECT:
            return Response(
                {"detail": "Group calls aren't supported yet."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        caller = request.user
        if caller.pk not in (conversation.participant_one_id, conversation.participant_two_id):
            return Response(status=status.HTTP_404_NOT_FOUND)
        callee = (
            conversation.participant_two
            if conversation.participant_one_id == caller.pk
            else conversation.participant_one
        )
        if callee is None or not callee.is_active or users_blocked(caller, callee):
            return Response(
                {"detail": "You can't call this account."},
                status=status.HTTP_403_FORBIDDEN,
            )

        superseded_ids = []
        with transaction.atomic():
            list(
                User.objects.select_for_update()
                .filter(pk__in=(caller.pk, callee.pk))
                .order_by("pk")
                .values_list("pk", flat=True)
            )

            previous_attempts = list(
                CallSession.objects.select_for_update()
                .filter(
                    caller_id=caller.pk,
                    callee_id=callee.pk,
                    status=CallSession.Status.RINGING,
                )
                .order_by("created_at")
            )
            now = timezone.now()
            for previous in previous_attempts:
                if call_is_live(previous.pk) is not False:
                    continue
                previous.status = CallSession.Status.FAILED
                previous.ended_at = now
                previous.ended_by_id = caller.pk
                previous.end_reason = "replaced_by_retry"
                previous.save(
                    update_fields=("status", "ended_at", "ended_by", "end_reason")
                )
                clear_call_liveness(previous.pk)
                superseded_ids.append(str(previous.pk))

            expire_stale_call_locks((caller.pk, callee.pk))
            busy = CallSession.objects.filter(status__in=ACTIVE_CALL_STATUSES).filter(
                Q(caller_id__in=(caller.pk, callee.pk))
                | Q(callee_id__in=(caller.pk, callee.pk))
            ).exists()
            if busy:
                return Response(
                    {"detail": "One of you is already in another call."},
                    status=status.HTTP_409_CONFLICT,
                )

            call = CallSession.objects.create(
                conversation=conversation,
                caller=caller,
                callee=callee,
                kind=kind,
            )

        for old_call_id in superseded_ids:
            send_call_state_push(old_call_id, callee.pk)

        push_count = send_call_push(call.pk)
        push_unavailable = isinstance(push_count, int) and push_count <= 0
        if push_unavailable:
            with transaction.atomic():
                failed = (
                    CallSession.objects.select_for_update()
                    .select_related("caller", "callee", "conversation")
                    .filter(pk=call.pk)
                    .first()
                )
                if failed and failed.status == CallSession.Status.RINGING:
                    failed.status = CallSession.Status.FAILED
                    failed.ended_at = timezone.now()
                    failed.ended_by_id = caller.pk
                    failed.end_reason = "push_unavailable"
                    failed.save(
                        update_fields=("status", "ended_at", "ended_by", "end_reason")
                    )
                    clear_call_liveness(failed.pk)
            return Response(
                {
                    "detail": (
                        f"Nova couldn't reach @{callee.username}'s device for this call. "
                        "They may need to open Nova once so push calling can reconnect."
                    )
                },
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        return Response(
            serialize_call(call, viewer_id=caller.pk),
            status=status.HTTP_201_CREATED,
        )
