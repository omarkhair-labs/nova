from django.urls import path

from .views import (
    RoomDetailView,
    RoomItemDetailView,
    RoomItemsView,
    RoomListView,
    PublicRoomFollowView,
    PublicRoomMembershipView,
    RoomReminderView,
    RoomTonightView,
)


urlpatterns = [
    path("rooms/", RoomListView.as_view(), name="rooms"),
    path("rooms/tonight/", RoomTonightView.as_view(), name="rooms-tonight"),
    path("rooms/<int:conversation_id>/membership/", PublicRoomMembershipView.as_view(), name="room-membership"),
    path("rooms/<int:conversation_id>/follow/", PublicRoomFollowView.as_view(), name="room-follow"),
    path("rooms/<int:conversation_id>/", RoomDetailView.as_view(), name="room-detail"),
    path("rooms/<int:conversation_id>/items/", RoomItemsView.as_view(), name="room-items"),
    path(
        "rooms/<int:conversation_id>/items/<int:item_id>/",
        RoomItemDetailView.as_view(),
        name="room-item-detail",
    ),
    path(
        "rooms/<int:conversation_id>/items/<int:item_id>/reminder/",
        RoomReminderView.as_view(),
        name="room-reminder",
    ),
]
