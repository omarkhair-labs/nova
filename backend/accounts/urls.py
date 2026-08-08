from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from .views import (
    FollowView,
    MeView,
    NovaTokenObtainPairView,
    PeopleView,
    PersonView,
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
]
