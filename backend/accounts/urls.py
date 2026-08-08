from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from .views import (
    FeedView,
    FollowView,
    MeView,
    NovaTokenObtainPairView,
    PeopleView,
    PersonView,
    PostDetailView,
    PostsView,
    RegisterView,
)

urlpatterns = [
    path("auth/register/", RegisterView.as_view(), name="register"),
    path("auth/login/", NovaTokenObtainPairView.as_view(), name="login"),
    path("auth/refresh/", TokenRefreshView.as_view(), name="token-refresh"),
    path("me/", MeView.as_view(), name="me"),
    path("people/", PeopleView.as_view(), name="people"),
    path("people/<str:username>/", PersonView.as_view(), name="person-detail"),
    path("people/<str:username>/follow/", FollowView.as_view(), name="person-follow"),
    path("posts/", PostsView.as_view(), name="posts"),
    path("posts/<int:post_id>/", PostDetailView.as_view(), name="post-detail"),
    path("feed/", FeedView.as_view(), name="feed"),
]
