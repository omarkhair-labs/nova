from django.urls import path

from .views import RoomDetailView, RoomItemDetailView, RoomItemsView, RoomListView


urlpatterns = [
    path("rooms/", RoomListView.as_view(), name="rooms"),
    path("rooms/<int:conversation_id>/", RoomDetailView.as_view(), name="room-detail"),
    path("rooms/<int:conversation_id>/items/", RoomItemsView.as_view(), name="room-items"),
    path(
        "rooms/<int:conversation_id>/items/<int:item_id>/",
        RoomItemDetailView.as_view(),
        name="room-item-detail",
    ),
]
