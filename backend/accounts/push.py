import json
import logging
import os
from functools import lru_cache

from .models import DevicePushToken, Notification

logger = logging.getLogger(__name__)


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

    tokens = list(
        DevicePushToken.objects.filter(
            user=notification.recipient,
            active=True,
        ).values_list("token", flat=True)
    )
    if not tokens:
        return 0

    title, body = _title_and_body(notification)
    data = {
        "notification_id": str(notification.pk),
        "kind": notification.kind,
        "actor_username": notification.actor.username,
        "post_id": str(notification.post_id or ""),
    }

    sent = 0
    for token in tokens:
        message = messaging.Message(
            token=token,
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
            DevicePushToken.objects.filter(token=token).update(active=False)
        except Exception:
            logger.exception("Nova failed to send an FCM push notification.")

    return sent
