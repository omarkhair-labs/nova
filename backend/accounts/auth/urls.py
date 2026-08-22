from django.urls import path

from .security import (
    ChangePasswordView,
    PasswordResetConfirmView,
    PasswordResetRequestView,
    RevokeOtherSessionsView,
    SecureRegisterView,
    SecureTokenObtainPairView,
    SecureTokenRefreshView,
)
from .views import MeView


urlpatterns = [
    path("auth/register/", SecureRegisterView.as_view(), name="register"),
    path("auth/login/", SecureTokenObtainPairView.as_view(), name="login"),
    path("auth/refresh/", SecureTokenRefreshView.as_view(), name="token-refresh"),
    path(
        "auth/password/reset/request/",
        PasswordResetRequestView.as_view(),
        name="password-reset-request",
    ),
    path(
        "auth/password/reset/confirm/",
        PasswordResetConfirmView.as_view(),
        name="password-reset-confirm",
    ),
    path(
        "auth/password/change/",
        ChangePasswordView.as_view(),
        name="password-change",
    ),
    path(
        "auth/sessions/revoke-others/",
        RevokeOtherSessionsView.as_view(),
        name="revoke-other-sessions",
    ),
    path("me/", MeView.as_view(), name="me"),
]
