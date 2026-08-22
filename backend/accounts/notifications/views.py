from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ..models import DevicePushToken, Notification
from ..serializers import NotificationSerializer
from ..trust_safety import blocked_user_ids

NOTIFICATION_PAGE_SIZE = 30


class NotificationsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        notifications = Notification.objects.filter(
            recipient=request.user,
        ).exclude(actor_id__in=blocked_user_ids(request.user)).select_related(
            "actor", "post", "comment"
        )

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
        unread_count = notifications.filter(read_at__isnull=True).count()

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


class DevicePushTokenView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        token = str(request.data.get("token", "")).strip()
        platform = str(request.data.get("platform", "android")).strip().lower() or "android"

        if len(token) < 20 or len(token) > 512:
            return Response(
                {"detail": "A valid push token is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if platform not in {"android"}:
            return Response(
                {"detail": "Unsupported push platform."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        device, created = DevicePushToken.objects.get_or_create(
            token=token,
            defaults={
                "user": request.user,
                "platform": platform,
                "active": True,
            },
        )
        if not created:
            changed = False
            if device.user_id != request.user.pk:
                device.user = request.user
                changed = True
            if device.platform != platform:
                device.platform = platform
                changed = True
            if not device.active:
                device.active = True
                changed = True
            if changed:
                device.save(update_fields=("user", "platform", "active", "updated_at"))

        return Response(
            {"registered": True},
            status=status.HTTP_201_CREATED if created else status.HTTP_200_OK,
        )

    def delete(self, request):
        token = str(request.data.get("token", "")).strip()
        if not token:
            return Response(
                {"detail": "Push token is required."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        deleted, _ = DevicePushToken.objects.filter(
            user=request.user,
            token=token,
        ).delete()
        return Response({"removed": bool(deleted)})
