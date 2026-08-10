from django.db import transaction
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.utils import timezone

from .models import CallSession, Conversation, Message


TERMINAL_CALL_STATUSES = {
    CallSession.Status.DECLINED,
    CallSession.Status.CANCELED,
    CallSession.Status.ENDED,
    CallSession.Status.MISSED,
    CallSession.Status.FAILED,
}
CALL_HISTORY_CLIENT_PREFIX = "call:"


def _duration_label(call):
    if not call.answered_at or not call.ended_at:
        return ""
    seconds = max(int((call.ended_at - call.answered_at).total_seconds()), 0)
    minutes, seconds = divmod(seconds, 60)
    hours, minutes = divmod(minutes, 60)
    if hours:
        return f"{hours}:{minutes:02d}:{seconds:02d}"
    return f"{minutes}:{seconds:02d}"


def _history_body(call):
    kind = "Video call" if call.kind == CallSession.Kind.VIDEO else "Voice call"
    if call.status == CallSession.Status.ENDED:
        duration = _duration_label(call)
        return f"{kind} · {duration}" if duration else f"{kind} · Ended"
    status_label = {
        CallSession.Status.DECLINED: "Declined",
        CallSession.Status.CANCELED: "Canceled",
        CallSession.Status.MISSED: "No answer",
        CallSession.Status.FAILED: "Failed",
    }.get(call.status, "Ended")
    return f"{kind} · {status_label}"


@receiver(post_save, sender=CallSession)
def persist_terminal_call_in_conversation(sender, instance, **kwargs):
    call = instance
    if call.status not in TERMINAL_CALL_STATUSES:
        return

    event_time = call.ended_at or timezone.now()
    message, created = Message.objects.get_or_create(
        sender_id=call.caller_id,
        client_id=f"{CALL_HISTORY_CLIENT_PREFIX}{call.pk}",
        defaults={
            "conversation_id": call.conversation_id,
            "recipient_id": call.callee_id,
            "body": _history_body(call),
            # Call history is a system event, not an unread chat message.
            "delivered_at": event_time,
            "read_at": event_time,
        },
    )
    if not created:
        return

    Conversation.objects.filter(pk=call.conversation_id).update(updated_at=event_time)

    def publish():
        # Import lazily so app startup/model registration cannot form a cycle.
        from .messaging_realtime import broadcast_message_created

        broadcast_message_created(message)

    transaction.on_commit(publish)
