import logging
import secrets
from datetime import timedelta

from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.hashers import check_password, make_password
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.core.mail import send_mail
from django.db import transaction
from django.utils import timezone
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import DevicePushToken
from .security_auth import secure_refresh_for_user
from .security_models import PasswordResetChallenge, UserSecurityState
from .serializers import RegisterSerializer, UserSerializer


logger = logging.getLogger(__name__)
User = get_user_model()
RESET_LIFETIME = timedelta(minutes=10)
RESET_COOLDOWN = timedelta(seconds=60)
RESET_MAX_ATTEMPTS = 5
GENERIC_RESET_DETAIL = (
    "If an active Nova account exists for that email, a 6-digit reset code has been sent."
)


def normalized_email(raw):
    return User.objects.normalize_email(str(raw or "").strip()).lower()


def invalidate_all_sessions(user):
    """Increment the user's JWT security version and disable push registrations."""

    state, _ = UserSecurityState.objects.select_for_update().get_or_create(
        user=user,
        defaults={"token_version": 0},
    )
    state.token_version += 1
    state.save(update_fields=("token_version", "updated_at"))
    DevicePushToken.objects.filter(user=user, active=True).update(active=False)
    return state.token_version


def password_error(password, user):
    try:
        validate_password(password, user=user)
    except ValidationError as exc:
        return exc.messages[0] if exc.messages else "Choose a stronger password."
    return None


class SecureRegisterView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        refresh = secure_refresh_for_user(user)
        return Response(
            {
                "access": str(refresh.access_token),
                "refresh": str(refresh),
                "user": UserSerializer(user, context={"request": request}).data,
            },
            status=status.HTTP_201_CREATED,
        )


class PasswordResetRequestView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        email = normalized_email(request.data.get("email"))
        if not email or "@" not in email or len(email) > 254:
            return Response(
                {"detail": "Enter a valid email address."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not getattr(settings, "NOVA_PASSWORD_RESET_EMAIL_READY", False):
            return Response(
                {"detail": "Password recovery email is temporarily unavailable."},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        user = User.objects.filter(email=email, is_active=True).first()
        if user is None:
            return Response({"detail": GENERIC_RESET_DETAIL})

        now = timezone.now()
        latest = PasswordResetChallenge.objects.filter(user=user).first()
        if latest is not None and latest.created_at >= now - RESET_COOLDOWN:
            return Response({"detail": GENERIC_RESET_DETAIL})

        code = f"{secrets.randbelow(1_000_000):06d}"
        PasswordResetChallenge.objects.filter(
            user=user,
            consumed_at__isnull=True,
        ).update(consumed_at=now)
        challenge = PasswordResetChallenge.objects.create(
            user=user,
            code_hash=make_password(code),
            expires_at=now + RESET_LIFETIME,
        )

        try:
            send_mail(
                subject="Your Nova password reset code",
                message=(
                    f"Your Nova password reset code is {code}.\n\n"
                    "It expires in 10 minutes. If you didn't request this, you can ignore this email."
                ),
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[user.email],
                fail_silently=False,
            )
        except Exception:
            logger.exception("Nova could not send a password reset email.")
            PasswordResetChallenge.objects.filter(pk=challenge.pk).update(consumed_at=timezone.now())

        return Response({"detail": GENERIC_RESET_DETAIL})


class PasswordResetCompleteView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        email = normalized_email(request.data.get("email"))
        code = str(request.data.get("code") or "").strip()
        new_password = str(request.data.get("new_password") or "")

        if not email or "@" not in email:
            return Response(
                {"detail": "Enter a valid email address."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if len(code) != 6 or not code.isdigit():
            return Response(
                {"detail": "Enter the 6-digit reset code."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if not new_password:
            return Response(
                {"detail": "Enter a new password."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        user = User.objects.filter(email=email, is_active=True).first()
        if user is None:
            return Response(
                {"detail": "That reset code is invalid or expired."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        now = timezone.now()
        challenge = PasswordResetChallenge.objects.filter(
            user=user,
            consumed_at__isnull=True,
        ).first()
        if challenge is None or challenge.expires_at <= now or challenge.attempts >= RESET_MAX_ATTEMPTS:
            if challenge is not None and challenge.consumed_at is None:
                challenge.consumed_at = now
                challenge.save(update_fields=("consumed_at",))
            return Response(
                {"detail": "That reset code is invalid or expired."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not check_password(code, challenge.code_hash):
            challenge.attempts += 1
            update_fields = ["attempts"]
            if challenge.attempts >= RESET_MAX_ATTEMPTS:
                challenge.consumed_at = now
                update_fields.append("consumed_at")
            challenge.save(update_fields=update_fields)
            return Response(
                {"detail": "That reset code is invalid or expired."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if user.check_password(new_password):
            return Response(
                {"detail": "Choose a password you haven't just been using."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        validation_error = password_error(new_password, user)
        if validation_error:
            return Response(
                {"detail": validation_error},
                status=status.HTTP_400_BAD_REQUEST,
            )

        with transaction.atomic():
            locked_user = User.objects.select_for_update().get(pk=user.pk)
            locked_user.set_password(new_password)
            locked_user.save(update_fields=("password",))
            PasswordResetChallenge.objects.filter(
                user=locked_user,
                consumed_at__isnull=True,
            ).update(consumed_at=timezone.now())
            invalidate_all_sessions(locked_user)

        return Response(
            {"detail": "Password reset. You can log in with your new password now."}
        )


class ChangePasswordView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        current_password = str(request.data.get("current_password") or "")
        new_password = str(request.data.get("new_password") or "")

        if not request.user.check_password(current_password):
            return Response(
                {"detail": "Your current password is incorrect."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if not new_password:
            return Response(
                {"detail": "Enter a new password."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        if request.user.check_password(new_password):
            return Response(
                {"detail": "Your new password must be different from your current password."},
                status=status.HTTP_400_BAD_REQUEST,
            )
        validation_error = password_error(new_password, request.user)
        if validation_error:
            return Response(
                {"detail": validation_error},
                status=status.HTTP_400_BAD_REQUEST,
            )

        with transaction.atomic():
            user = User.objects.select_for_update().get(pk=request.user.pk)
            user.set_password(new_password)
            user.save(update_fields=("password",))
            invalidate_all_sessions(user)

        return Response(
            {"detail": "Password changed. For your security, every Nova session was signed out."}
        )


class LogoutAllSessionsView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        with transaction.atomic():
            user = User.objects.select_for_update().get(pk=request.user.pk)
            invalidate_all_sessions(user)
        return Response(
            {"detail": "All Nova sessions were signed out."}
        )
