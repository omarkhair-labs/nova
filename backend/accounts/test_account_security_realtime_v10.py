from asgiref.sync import async_to_sync
from django.contrib.auth import get_user_model
from django.test import TestCase

from .account_security import issue_session
from .realtime_auth import SecureJwtAuthMiddleware


User = get_user_model()
PASSWORD = "StrongNovaPass2026!"
NEW_PASSWORD = "EvenStrongerNovaPass2026!"


async def _inner(scope, receive, send):
    return scope.get("user")


class AccountSecurityRealtimeV10Tests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            email="security-realtime-v10@example.com",
            username="security-realtime-v10",
            password=PASSWORD,
            name="Realtime Security",
        )
        self.middleware = SecureJwtAuthMiddleware(_inner)

    def scope_for(self, access_token):
        return {
            "type": "websocket",
            "headers": [
                (b"authorization", f"Bearer {access_token}".encode("latin1")),
            ],
        }

    def authenticate(self, access_token):
        return async_to_sync(self.middleware._authenticate)(self.scope_for(access_token))

    def test_websocket_rejects_password_revoked_access_token(self):
        old_session = issue_session(self.user)
        authenticated = self.authenticate(old_session["access"])
        self.assertTrue(authenticated.is_authenticated)
        self.assertEqual(authenticated.pk, self.user.pk)

        self.user.set_password(NEW_PASSWORD)
        self.user.save(update_fields=("password",))

        stale = self.authenticate(old_session["access"])
        self.assertFalse(stale.is_authenticated)

        fresh_session = issue_session(self.user)
        fresh = self.authenticate(fresh_session["access"])
        self.assertTrue(fresh.is_authenticated)
        self.assertEqual(fresh.pk, self.user.pk)

    def test_websocket_without_bearer_token_is_anonymous(self):
        anonymous = async_to_sync(self.middleware._authenticate)(
            {"type": "websocket", "headers": []}
        )
        self.assertFalse(anonymous.is_authenticated)
