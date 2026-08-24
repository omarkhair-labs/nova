import secrets

from django.utils.crypto import salted_hmac
from rest_framework_simplejwt.authentication import JWTAuthentication
from rest_framework_simplejwt.exceptions import AuthenticationFailed


def password_fingerprint(user):
    """Return a keyed fingerprint of the current Django password hash.

    The raw password hash is never embedded in a token. Changing or re-hashing
    the password changes this fingerprint and therefore invalidates V10 JWTs.
    """

    return salted_hmac("nova.jwt.password", user.password).hexdigest()


def token_matches_current_password(validated_token, user, *, allow_legacy_access=False):
    claim = validated_token.get("pwdv")
    if claim:
        return secrets.compare_digest(str(claim), password_fingerprint(user))
    return allow_legacy_access


class NovaJWTAuthentication(JWTAuthentication):
    def get_user(self, validated_token):
        user = super().get_user(validated_token)
        if not token_matches_current_password(
            validated_token,
            user,
            allow_legacy_access=True,
        ):
            raise AuthenticationFailed("Session expired. Please log in again.")
        session_key = str(validated_token.get("sid") or "")
        if session_key:
            from ..auth_session_models import AuthSessionRecord

            session = AuthSessionRecord.objects.filter(
                session_key=session_key,
                user=user,
                is_active=True,
            ).first()
            if session is None:
                raise AuthenticationFailed("Session expired. Please log in again.")
            session.save(update_fields=("last_seen_at",))
        return user
