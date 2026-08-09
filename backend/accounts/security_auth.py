from django.contrib.auth import get_user_model
from rest_framework.exceptions import AuthenticationFailed
from rest_framework_simplejwt.authentication import JWTAuthentication
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError
from rest_framework_simplejwt.serializers import TokenRefreshSerializer
from rest_framework_simplejwt.settings import api_settings
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView

from .security_models import UserSecurityState
from .serializers import NovaTokenObtainPairSerializer


User = get_user_model()
SECURITY_VERSION_CLAIM = "sec_v"


def security_version(user):
    try:
        return int(user.security_state.token_version)
    except UserSecurityState.DoesNotExist:
        return 0


def secure_refresh_for_user(user):
    refresh = RefreshToken.for_user(user)
    refresh[SECURITY_VERSION_CLAIM] = security_version(user)
    return refresh


class NovaSecureTokenObtainPairSerializer(NovaTokenObtainPairSerializer):
    @classmethod
    def get_token(cls, user):
        token = super().get_token(user)
        token[SECURITY_VERSION_CLAIM] = security_version(user)
        return token


class NovaSecureTokenObtainPairView(TokenObtainPairView):
    permission_classes = []
    authentication_classes = []
    serializer_class = NovaSecureTokenObtainPairSerializer


class NovaSecureTokenRefreshSerializer(TokenRefreshSerializer):
    def validate(self, attrs):
        try:
            refresh = RefreshToken(attrs["refresh"])
        except TokenError as exc:
            raise InvalidToken(exc.args[0]) from exc

        user_id = refresh.payload.get(api_settings.USER_ID_CLAIM)
        if user_id is None:
            raise InvalidToken("Token contained no recognizable user identification.")

        user = User.objects.filter(
            **{api_settings.USER_ID_FIELD: user_id},
            is_active=True,
        ).first()
        if user is None:
            raise InvalidToken("User is unavailable.")

        raw_version = refresh.payload.get(SECURITY_VERSION_CLAIM, 0)
        try:
            token_version = int(raw_version)
        except (TypeError, ValueError):
            raise InvalidToken("Invalid session security version.")

        if token_version != security_version(user):
            raise InvalidToken("This session has been revoked. Please log in again.")

        return super().validate(attrs)


class NovaSecureTokenRefreshView(TokenRefreshView):
    permission_classes = []
    authentication_classes = []
    serializer_class = NovaSecureTokenRefreshSerializer


class NovaJWTAuthentication(JWTAuthentication):
    """JWT authentication with a per-user revocation version check."""

    def get_user(self, validated_token):
        user = super().get_user(validated_token)
        raw_version = validated_token.payload.get(SECURITY_VERSION_CLAIM, 0)
        try:
            token_version = int(raw_version)
        except (TypeError, ValueError):
            raise AuthenticationFailed(
                "Invalid session security version.",
                code="session_revoked",
            )

        if token_version != security_version(user):
            raise AuthenticationFailed(
                "This session has been revoked. Please log in again.",
                code="session_revoked",
            )
        return user
