import hashlib
import json
import logging
import os
import secrets
import time

from django.contrib.auth import get_user_model
from django.contrib.auth.hashers import check_password, make_password
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.core.mail import get_connection, send_mail
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.exceptions import InvalidToken
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer, TokenRefreshSerializer
from rest_framework_simplejwt.settings import api_settings
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView

from .jwt_auth import password_fingerprint, token_matches_current_password
from .models import DevicePushToken
from .serializers import RegisterSerializer, UserSerializer

logger = logging.getLogger(__name__)
User = get_user_model()

RESET_CODE_TTL_SECONDS = 10 * 60
RESET_REQUEST_COOLDOWN_SECONDS = 60
RESET_MAX_ATTEMPTS = 5
RESET_CODE_DIGITS = 6
RESET_KEY_PREFIX = "nova:password-reset"


def normalize_email(value):
    return User.objects.normalize_email(str(value or "").strip()).lower()


def issue_session(user, request=None):
    refresh = RefreshToken.for_user(user)
    refresh["pwdv"] = password_fingerprint(user)
    access = refresh.access_token
    access["pwdv"] = password_fingerprint(user)
    return {
        "access": str(access),
        "refresh": str(refresh),
        "user": UserSerializer(user, context={"request": request}).data,
    }


class SecureTokenObtainPairSerializer(TokenObtainPairSerializer):
    @classmethod
    def get_token(cls, user):
        token = super().get_token(user)
        token["pwdv"] = password_fingerprint(user)
        return token

    def validate(self, attrs):
        data = super().validate(attrs)
        data["user"] = UserSerializer(self.user, context=self.context).data
        return data


class SecureTokenRefreshSerializer(TokenRefreshSerializer):
    def validate(self, attrs):
        try:
            refresh = RefreshToken(attrs["refresh"])
        except Exception as exc:
            raise InvalidToken("Token is invalid or expired") from exc

        # Deliberately reject pre-V10 refresh tokens. Existing users can keep
        # using their old access token until its normal <=15 minute expiry,
        # then log in once to enter the password-bound session system.
        if not refresh.get("pwdv"):
            raise InvalidToken("Session expired. Please log in again.")

        user_id = refresh.get(api_settings.USER_ID_CLAIM)
        if user_id is None:
            raise InvalidToken("Session expired. Please log in again.")

        user = User.objects.filter(
            **{api_settings.USER_ID_FIELD: user_id},
            is_active=True,
        ).first()
        if user is None or not token_matches_current_password(refresh, user):
            raise InvalidToken("Session expired. Please log in again.")

        access = refresh.access_token
        access["pwdv"] = password_fingerprint(user)
        return {"access": str(access)}


class SecureTokenObtainPairView(TokenObtainPairView):
    permission_classes = [AllowAny]
    authentication_classes = []
    serializer_class = SecureTokenObtainPairSerializer


class SecureTokenRefreshView(TokenRefreshView):
    permission_classes = [AllowAny]
    authentication_classes = []
    serializer_class = SecureTokenRefreshSerializer


class SecureRegisterView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        return Response(
            issue_session(user, request=request),
            status=status.HTTP_201_CREATED,
        )


def _reset_key(email):
    digest = hashlib.sha256(email.encode("utf-8")).hexdigest()
    return f"{RESET_KEY_PREFIX}:challenge:{digest}"


def _cooldown_key(email):
    digest = hashlib.sha256(email.encode("utf-8")).hexdigest()
    return f"{RESET_KEY_PREFIX}:cooldown:{digest}"


def _redis_client():
    url = os.getenv("REDIS_URL", "").strip()
    if not url:
        return None
    try:
        import redis

        return redis.Redis.from_url(url, decode_responses=True)
    except Exception:
        logger.exception("Nova could not initialize Redis for password recovery.")
        return None


_local_reset_store = {}


def _store_get(key):
    client = _redis_client()
    if client is not None:
        try:
            return client.get(key)
        except Exception:
            logger.exception("Nova password recovery Redis read failed.")

    item = _local_reset_store.get(key)
    if not item:
        return None
    value, expires_at = item
    if expires_at <= time.time():
        _local_reset_store.pop(key, None)
        return None
    return value


def _store_set(key, value, ttl_seconds, *, only_if_absent=False):
    client = _redis_client()
    if client is not None:
        try:
            result = client.set(key, value, ex=max(int(ttl_seconds), 1), nx=only_if_absent)
            return bool(result)
        except Exception:
            logger.exception("Nova password recovery Redis write failed.")

    now = time.time()
    current = _local_reset_store.get(key)
    if only_if_absent and current and current[1] > now:
        return False
    _local_reset_store[key] = (value, now + max(int(ttl_seconds), 1))
    return True


def _store_delete(key):
    client = _redis_client()
    if client is not None:
        try:
            client.delete(key)
        except Exception:
            logger.exception("Nova password recovery Redis delete failed.")
    _local_reset_store.pop(key, None)


def recovery_email_configured():
    return bool(os.getenv("EMAIL_HOST", "").strip() and os.getenv("DEFAULT_FROM_EMAIL", "").strip())


def send_password_reset_code(user, code):
    host = os.getenv("EMAIL_HOST", "").strip()
    from_email = os.getenv("DEFAULT_FROM_EMAIL", "").strip()
    if not host or not from_email:
        return False

    try:
        port = int(os.getenv("EMAIL_PORT", "587"))
    except ValueError:
        port = 587

    use_tls = os.getenv("EMAIL_USE_TLS", "1") == "1"
    use_ssl = os.getenv("EMAIL_USE_SSL", "0") == "1"
    if use_ssl:
        use_tls = False

    connection = get_connection(
        backend="django.core.mail.backends.smtp.EmailBackend",
        host=host,
        port=port,
        username=os.getenv("EMAIL_HOST_USER", "").strip() or None,
        password=os.getenv("EMAIL_HOST_PASSWORD", "") or None,
        use_tls=use_tls,
        use_ssl=use_ssl,
        timeout=15,
    )
    display_name = user.name.strip() or user.username
    return bool(
        send_mail(
            subject="Your Nova password reset code",
            message=(
                f"Hi {display_name},\n\n"
                f"Your Nova password reset code is: {code}\n\n"
                "This code expires in 10 minutes. If you didn't request it, "
                "you can ignore this email.\n\nNova"
            ),
            from_email=from_email,
            recipient_list=[user.email],
            connection=connection,
            fail_silently=False,
        )
    )


class PasswordResetRequestView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        email = normalize_email(request.data.get("email"))
        if not email or "@" not in email:
            return Response(
                {"detail": "Enter a valid email address."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not recovery_email_configured():
            return Response(
                {"detail": "Password recovery email is temporarily unavailable."},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        generic = {
            "detail": "If an account exists for that email, a reset code has been sent."
        }

        if not _store_set(
            _cooldown_key(email),
            "1",
            RESET_REQUEST_COOLDOWN_SECONDS,
            only_if_absent=True,
        ):
            return Response(generic)

        user = User.objects.filter(email__iexact=email, is_active=True).first()
        if user is None:
            return Response(generic)

        code = f"{secrets.randbelow(10 ** RESET_CODE_DIGITS):0{RESET_CODE_DIGITS}d}"
        payload = {
            "user_id": user.pk,
            "code_hash": make_password(code),
            "attempts": 0,
            "expires_at": time.time() + RESET_CODE_TTL_SECONDS,
        }
        _store_set(
            _reset_key(email),
            json.dumps(payload),
            RESET_CODE_TTL_SECONDS,
        )

        try:
            delivered = send_password_reset_code(user, code)
        except Exception:
            delivered = False
            logger.exception("Nova failed to send a password reset email.")

        if not delivered:
            _store_delete(_reset_key(email))
        return Response(generic)


class PasswordResetConfirmView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        email = normalize_email(request.data.get("email"))
        code = str(request.data.get("code") or "").strip()
        new_password = str(request.data.get("new_password") or "")

        invalid = Response(
            {"detail": "That reset code is invalid or expired."},
            status=status.HTTP_400_BAD_REQUEST,
        )
        if not email or len(code) != RESET_CODE_DIGITS or not code.isdigit():
            return invalid

        raw = _store_get(_reset_key(email))
        if not raw:
            return invalid
        try:
            payload = json.loads(raw)
        except (TypeError, ValueError):
            _store_delete(_reset_key(email))
            return invalid

        remaining = int(float(payload.get("expires_at", 0)) - time.time())
        attempts = int(payload.get("attempts", 0))
        if remaining <= 0 or attempts >= RESET_MAX_ATTEMPTS:
            _store_delete(_reset_key(email))
            return invalid

        if not check_password(code, str(payload.get("code_hash") or "")):
            attempts += 1
            payload["attempts"] = attempts
            if attempts >= RESET_MAX_ATTEMPTS:
                _store_delete(_reset_key(email))
            else:
                _store_set(_reset_key(email), json.dumps(payload), remaining)
            return invalid

        user = User.objects.filter(
            pk=payload.get("user_id"),
            email__iexact=email,
            is_active=True,
        ).first()
        if user is None:
            _store_delete(_reset_key(email))
            return invalid

        try:
            validate_password(new_password, user=user)
        except ValidationError as exc:
            return Response(
                {"detail": " ".join(exc.messages)},
                status=status.HTTP_400_BAD_REQUEST,
            )

        user.set_password(new_password)
        user.last_login = timezone.now()
        user.save(update_fields=("password", "last_login"))
        DevicePushToken.objects.filter(user=user, active=True).update(active=False)
        _store_delete(_reset_key(email))

        return Response(
            {"detail": "Password reset. Log in with your new password."}
        )


class ChangePasswordView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        current_password = str(request.data.get("current_password") or "")
        new_password = str(request.data.get("new_password") or "")
        user = request.user

        if not user.check_password(current_password):
            return Response(
                {"detail": "Current password is incorrect."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if current_password == new_password:
            return Response(
                {"detail": "Choose a new password different from your current password."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        try:
            validate_password(new_password, user=user)
        except ValidationError as exc:
            return Response(
                {"detail": " ".join(exc.messages)},
                status=status.HTTP_400_BAD_REQUEST,
            )

        user.set_password(new_password)
        user.last_login = timezone.now()
        user.save(update_fields=("password", "last_login"))
        DevicePushToken.objects.filter(user=user, active=True).update(active=False)
        return Response(issue_session(user, request=request))


class RevokeOtherSessionsView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        current_password = str(request.data.get("current_password") or "")
        user = request.user
        if not user.check_password(current_password):
            return Response(
                {"detail": "Current password is incorrect."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        user.set_password(current_password)
        user.last_login = timezone.now()
        user.save(update_fields=("password", "last_login"))
        DevicePushToken.objects.filter(user=user, active=True).update(active=False)
        return Response(issue_session(user, request=request))
