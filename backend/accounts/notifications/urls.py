from django.urls import path

from ..views import DevicePushTokenView, NotificationsReadView, NotificationsView


urlpatterns = [
    path("notifications/", NotificationsView.as_view(), name="notifications"),
    path("notifications/read/", NotificationsReadView.as_view(), name="notifications-read"),
    path("push/devices/", DevicePushTokenView.as_view(), name="push-devices"),
]
