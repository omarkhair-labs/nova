import json
import logging
import os
from datetime import timedelta
from functools import lru_cache

from .messaging_models import ConversationPreference, GroupMembership
from .models import CallSession, Conversation, DevicePushToken, Notification
from .trust_safety import blocked_user_ids

logger = logging.getLogger(__name__)

REEL_NOTIFICATION_KINDS = {"reel_like", "reel_comment", "reel_repost", "reel_reply"}


@lru_cache(maxsize=1)
def _firebase_app():
    raw = os.getenv("FIREBASE_SERVICE_ACCOUNT_JSON", "").strip()
    if not raw:
        return None

    try:
        import firebase_admin
        from firebase_admin import credentials

        if firebase_admin._apps:
            return firebase_admin.get_app()

        service_account = json.loads(raw)
        return firebase_admin.initialize_app(credentials.Certificate(service_account))
    except Exception:
        logger.exception("Nova could not initialize Firebase Admin.")
        return None


def _notification_reel_id(notification):
    if notification.kind not in REEL_NOTIFICATION_KINDS:
        return None
    try:
        reel_id = int(str(notification.dedupe_key).rsplit(":", 1)[-1])
    except (TypeError, ValueError):
        return None
    return reel_id if reel_id > 0 else None


def _notification_reel_author_username(notification):
    reel_id = _notification_reel_id(notification)
    if reel_id is None:
        return ""
    from .reels_models import Reel

    return (
        Reel.objects.filter(pk=reel_id, author__is_active=True)
        .values_list("author__username", flat=True)
        .first()
        or ""
    )


def _title_and_body(notification):
    actor_name = notification.actor.name.strip() or f"@{notification.actor.username}"

    if notification.kind == Notification.Kind.FOLLOW:
        return "New follower", f"{actor_name} started following you"

    if notification.kind == Notification.Kind.LIKE:
        return "New like", f"{actor_name} liked your post"

    if notification.kind == Notification.Kind.COMMENT:
        preview = ""
        if notification.comment_id and notification.comment:
            preview = notification.comment.body.strip()
        if preview:
            preview = preview[:90] + ("…" if len(preview) > 90 else "")
            return "New comment", f"{actor_name}: {preview}"
        return "New comment", f"{actor_name} commented on your post"

    if notification.kind == "comment_reply":
        return "New reply", f"{actor_name} replied to your comment"

    if notification.kind == "reel_like":
        return "New Reel like", f"{actor_name} liked your Reel"

    if notification.kind == "reel_comment":
        return "New Reel comment", f"{actor_name} commented on your Reel"

    if notification.kind == "reel_repost":
        return "New Reel repost", f"{actor_name} reposted your Reel"

    if notification.kind == "reel_reply":
        return "New Reel reply", f"{actor_name} replied to your Reel comment"

    return "Nova activity", f"{actor_name} interacted with you"


def send_notification_push(notification):
    app = _firebase_app()
    if app is None:
        return 0

    try:
        from firebase_admin import messaging
    except Exception:
        logger.exception("Firebase messaging is unavailable.")
        return 0

    fids = list(
        DevicePushToken.objects.filter(
            user=notification.recipient,
            active=True,
        ).values_list("token", flat=True)
    )
    if not fids:
        return 0

    title, body = _title_and_body(notification)
    reel_id = _notification_reel_id(notification)
    data = {
        "notification_id": str(notification.pk),
        "kind": notification.kind,
        "actor_username": notification.actor.username,
        "post_id": str(notification.post_id or ""),
        "reel_id": str(reel_id or ""),
        "reel_author_username": _notification_reel_author_username(notification),
    }

    sent = 0
    for fid in fids:
        message = messaging.Message(
            fid=fid,
            notification=messaging.Notification(title=title, body=body),
            data=data,
            android=messaging.AndroidConfig(
                priority="high",
                notification=messaging.AndroidNotification(
                    channel_id="nova_activity",
                    sound="default",
                ),
            ),
        )
        try:
            messaging.send(message, app=app)
            sent += 1
        except messaging.UnregisteredError:
            DevicePushToken.objects.filter(token=fid).update(active=False)
        except Exception:
            logger.exception("Nova failed to send an FCM push notification.")

    return sent


def _message_preview(message):
    preview = message.body.strip()
    if not preview and message.audio:
        preview = "🎤 Voice message"
    elif not preview and message.image:
        preview = "📷 Photo"
    return preview[:120] + ("…" if len(preview) > 120 else "")


def send_message_push(message):
    """Deliver direct or group message pushes without Activity rows."""

    conversation = message.conversation
    actor_name = message.sender.name.strip() or f"@{message.sender.username}"
    preview = _message_preview(message)
    actor_avatar_url = ""
    if message.sender.avatar:
        try:
            actor_avatar_url = message.sender.avatar.url
        except Exception:
            actor_avatar_url = ""

    if conversation.kind == Conversation.Kind.GROUP:
        muted_ids = set(
            ConversationPreference.objects.filter(
                conversation=conversation,
                muted=True,
            ).values_list("user_id", flat=True)
        )
        hidden_ids = blocked_user_ids(message.sender)
        recipient_ids = list(
            GroupMembership.objects.filter(
                conversation=conversation,
                user__is_active=True,
            )
            .exclude(user_id=message.sender_id)
            .exclude(user_id__in=muted_ids)
            .exclude(user_id__in=hidden_ids)
            .values_list("user_id", flat=True)
        )
        if not recipient_ids:
            return 0
        fids = list(
            DevicePushToken.objects.filter(
                user_id__in=recipient_ids,
                active=True,
            ).values_list("token", flat=True)
        )
        notification_title = conversation.title.strip() or "Nova group"
        notification_body = f"{actor_name}: {preview or 'Sent a message'}"
        conversation_kind = "group"
    else:
        if message.recipient_id is None:
            return 0
        if ConversationPreference.objects.filter(
            conversation_id=message.conversation_id,
            user_id=message.recipient_id,
            muted=True,
        ).exists():
            return 0
        fids = list(
            DevicePushToken.objects.filter(
                user=message.recipient,
                active=True,
            ).values_list("token", flat=True)
        )
        notification_title = actor_name
        notification_body = preview or "Sent you a message"
        conversation_kind = "direct"

    if not fids:
        return 0

    app = _firebase_app()
    if app is None:
        return 0

    try:
        from firebase_admin import messaging
    except Exception:
        logger.exception("Firebase messaging is unavailable.")
        return 0

    data = {
        "notification_id": str(message.pk),
        "kind": "message",
        "conversation_id": str(message.conversation_id),
        "conversation_kind": conversation_kind,
        "group_title": conversation.title if conversation_kind == "group" else "",
        "message_id": str(message.pk),
        "actor_username": message.sender.username,
        "actor_name": actor_name,
        "actor_avatar_url": actor_avatar_url,
        "message_preview": preview,
    }

    sent = 0
    for fid in fids:
        push_message = messaging.Message(
            fid=fid,
            notification=messaging.Notification(
                title=notification_title,
                body=notification_body,
            ),
            data=data,
            android=messaging.AndroidConfig(
                priority="high",
                notification=messaging.AndroidNotification(
                    channel_id="nova_messages",
                    sound="default",
                ),
            ),
        )
        try:
            messaging.send(push_message, app=app)
            sent += 1
        except messaging.UnregisteredError:
            DevicePushToken.objects.filter(token=fid).update(active=False)
        except Exception:
            logger.exception("Nova failed to send a message push notification.")

    return sent


def send_call_push(call_id):
    """Wake the callee for an incoming call using a short-lived data-only FCM."""

    app = _firebase_app()
    if app is None:
        return 0

    try:
        from firebase_admin import messaging
    except Exception:
        logger.exception("Firebase messaging is unavailable.")
        return 0

    call = (
        CallSession.objects.select_related("caller", "callee")
        .filter(pk=call_id, status=CallSession.Status.RINGING)
        .first()
    )
    if call is None:
        return 0

    fids = list(
        DevicePushToken.objects.filter(
            user=call.callee,
            active=True,
        ).values_list("token", flat=True)
    )
    if not fids:
        return 0

    caller_name = call.caller.name.strip() or f"@{call.caller.username}"
    caller_avatar_url = ""
    if call.caller.avatar:
        try:
            caller_avatar_url = call.caller.avatar.url
        except Exception:
            caller_avatar_url = ""

    data = {
        "kind": "incoming_call",
        "call_id": str(call.pk),
        "call_kind": call.kind,
        "conversation_id": str(call.conversation_id),
        "caller_id": str(call.caller_id),
        "caller_username": call.caller.username,
        "caller_name": caller_name,
        "caller_avatar_url": caller_avatar_url,
    }

    sent = 0
    for fid in fids:
        push_message = messaging.Message(
            fid=fid,
            data=data,
            android=messaging.AndroidConfig(
                priority="high",
                ttl=timedelta(seconds=60),
                direct_boot_ok=False,
            ),
        )
        try:
            messaging.send(message=push_message, app=app)
            sent += 1
        except messaging.UnregisteredError:
            DevicePushToken.objects.filter(token=fid).update(active=False)
        except Exception:
            logger.exception("Nova failed to send an incoming-call FCM push.")

    return sent


def send_call_state_push(call_id, target_user_id):
    """Wake a participant so stale incoming/ongoing call UI can be dismissed."""

    app = _firebase_app()
    if app is None:
        return 0

    try:
        from firebase_admin import messaging
    except Exception:
        logger.exception("Firebase messaging is unavailable.")
        return 0

    call = CallSession.objects.filter(pk=call_id).first()
    if call is None or target_user_id not in (call.caller_id, call.callee_id):
        return 0

    fids = list(
        DevicePushToken.objects.filter(
            user_id=target_user_id,
            active=True,
        ).values_list("token", flat=True)
    )
    if not fids:
        return 0

    data = {
        "kind": "call_state",
        "call_id": str(call.pk),
        "call_status": call.status,
        "call_kind": call.kind,
        "conversation_id": str(call.conversation_id),
    }

    sent = 0
    for fid in fids:
        push_message = messaging.Message(
            fid=fid,
            data=data,
            android=messaging.AndroidConfig(
                priority="high",
                ttl=timedelta(seconds=60),
                direct_boot_ok=False,
            ),
        )
        try:
            messaging.send(push_message, app=app)
            sent += 1
        except messaging.UnregisteredError:
            DevicePushToken.objects.filter(token=fid).update(active=False)
        except Exception:
            logger.exception("Nova failed to send a call-state FCM push.")

    return sent
