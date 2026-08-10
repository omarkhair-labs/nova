from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from .calls import (
    ACTIVE_CALL_STATUSES,
    clear_call_liveness,
    expire_stale_call_locks,
    serialize_call,
)
from .models import CallSession, Conversation, User
from .push import send_call_push, send_call_state_push


class ReliableCallSessionCreateView(APIView):
    """Create one outgoing call without letting a dead retry lock both users.

    Starting a second call from the same caller to the same callee means the
    previous ringing attempt is no longer the UI the caller is using. Close that
    orphan before the global busy check. This is deliberately narrower than
    clearing arbitrary busy calls so a real call with somebody else stays safe.
    """

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
        caller = request.user
        if caller.pk not in (conversation.participant_one_id, conversation.participant_two_id):
            return Response(status=status.HTTP_404_NOT_FOUND)
        callee = (
            conversation.participant_two
            if conversation.participant_one_id == caller.pk
            else conversation.participant_one
        )

        superseded_ids = []
        with transaction.atomic():
            # Lock both people in deterministic order so simultaneous calls still
            # cannot both pass the busy check.
            list(
                User.objects.select_for_update()
                .filter(pk__in=(caller.pk, callee.pk))
                .order_by("pk")
                .values_list("pk", flat=True)
            )

            # A retry from the same caller supersedes their previous ringing
            # attempt. This is the stale row that previously produced the false
            # "already in another call" message after CallActivity had exited.
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

        # Dismiss any stale incoming UI from an attempt we just superseded.
        for old_call_id in superseded_ids:
            send_call_state_push(old_call_id, callee.pk)

        # Incoming calls have no global app socket; the data-only FCM is what
        # tells the callee which call to open. If Firebase accepts zero devices,
        # do not pretend the phone is ringing and do not leave another busy row.
        push_count = send_call_push(call.pk)
        if push_count <= 0:
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
