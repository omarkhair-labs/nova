from unittest.mock import patch

from django.contrib.auth import get_user_model
from rest_framework import status
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import AccessToken

from . import account_security


User = get_user_model()
PASSWORD = "StrongNovaPass2026!"
NEW_PASSWORD = "EvenStrongerNovaPass2026!"


class AccountSecurityV10Tests(APITestCase):
    def setUp(self):
        account_security._local_reset_store.clear()
        self.user = User.objects.create_user(
            email="security-v10@example.com",
            username="security-v10",
            password=PASSWORD,
            name="Security V10",
        )

    def login(self, password=PASSWORD):
        response = self.client.post(
            "/api/v1/auth/login/",
            {"email": self.user.email, "password": password},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        return response.data

    def authorize(self, access):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")

    @patch("accounts.account_security._redis_client", return_value=None)
    @patch("accounts.account_security.recovery_email_configured", return_value=True)
    def test_reset_request_is_generic_for_known_and_unknown_email(
        self,
        _configured,
        _redis,
    ):
        sent = []

        def capture(user, code):
            sent.append((user.email, code))
            return True

        with patch("accounts.account_security.send_password_reset_code", side_effect=capture):
            known = self.client.post(
                "/api/v1/auth/password/reset/request/",
                {"email": self.user.email},
                format="json",
            )
            unknown = self.client.post(
                "/api/v1/auth/password/reset/request/",
                {"email": "nobody-v10@example.com"},
                format="json",
            )

        self.assertEqual(known.status_code, status.HTTP_200_OK)
        self.assertEqual(unknown.status_code, status.HTTP_200_OK)
        self.assertEqual(known.data["detail"], unknown.data["detail"])
        self.assertEqual(len(sent), 1)
        self.assertEqual(sent[0][0], self.user.email)
        self.assertEqual(len(sent[0][1]), 6)
        self.assertTrue(sent[0][1].isdigit())

    @patch("accounts.account_security._redis_client", return_value=None)
    @patch("accounts.account_security.recovery_email_configured", return_value=True)
    def test_reset_code_changes_password_and_invalidates_v10_tokens(
        self,
        _configured,
        _redis,
    ):
        old_session = self.login()
        code_holder = {}

        def capture(user, code):
            code_holder["code"] = code
            return True

        with patch("accounts.account_security.send_password_reset_code", side_effect=capture):
            requested = self.client.post(
                "/api/v1/auth/password/reset/request/",
                {"email": self.user.email},
                format="json",
            )
        self.assertEqual(requested.status_code, status.HTTP_200_OK)

        confirmed = self.client.post(
            "/api/v1/auth/password/reset/confirm/",
            {
                "email": self.user.email,
                "code": code_holder["code"],
                "new_password": NEW_PASSWORD,
            },
            format="json",
        )
        self.assertEqual(confirmed.status_code, status.HTTP_200_OK)

        self.authorize(old_session["access"])
        stale = self.client.get("/api/v1/me/")
        self.assertEqual(stale.status_code, status.HTTP_401_UNAUTHORIZED)

        self.client.credentials()
        old_login = self.client.post(
            "/api/v1/auth/login/",
            {"email": self.user.email, "password": PASSWORD},
            format="json",
        )
        self.assertEqual(old_login.status_code, status.HTTP_401_UNAUTHORIZED)

        new_login = self.client.post(
            "/api/v1/auth/login/",
            {"email": self.user.email, "password": NEW_PASSWORD},
            format="json",
        )
        self.assertEqual(new_login.status_code, status.HTTP_200_OK)

    @patch("accounts.account_security._redis_client", return_value=None)
    @patch("accounts.account_security.recovery_email_configured", return_value=True)
    def test_wrong_reset_code_is_limited(self, _configured, _redis):
        code_holder = {}

        def capture(user, code):
            code_holder["code"] = code
            return True

        with patch("accounts.account_security.send_password_reset_code", side_effect=capture):
            self.client.post(
                "/api/v1/auth/password/reset/request/",
                {"email": self.user.email},
                format="json",
            )

        for _ in range(account_security.RESET_MAX_ATTEMPTS):
            response = self.client.post(
                "/api/v1/auth/password/reset/confirm/",
                {
                    "email": self.user.email,
                    "code": "000000" if code_holder["code"] != "000000" else "999999",
                    "new_password": NEW_PASSWORD,
                },
                format="json",
            )
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        correct_after_lock = self.client.post(
            "/api/v1/auth/password/reset/confirm/",
            {
                "email": self.user.email,
                "code": code_holder["code"],
                "new_password": NEW_PASSWORD,
            },
            format="json",
        )
        self.assertEqual(correct_after_lock.status_code, status.HTTP_400_BAD_REQUEST)

    def test_change_password_returns_fresh_session_and_revokes_old_session(self):
        old_session = self.login()
        old_access = old_session["access"]
        old_refresh = old_session["refresh"]
        self.authorize(old_access)

        changed = self.client.post(
            "/api/v1/auth/password/change/",
            {
                "current_password": PASSWORD,
                "new_password": NEW_PASSWORD,
            },
            format="json",
        )
        self.assertEqual(changed.status_code, status.HTTP_200_OK)
        self.assertIn("access", changed.data)
        self.assertIn("refresh", changed.data)

        self.authorize(old_access)
        stale_access = self.client.get("/api/v1/me/")
        self.assertEqual(stale_access.status_code, status.HTTP_401_UNAUTHORIZED)

        self.client.credentials()
        stale_refresh = self.client.post(
            "/api/v1/auth/refresh/",
            {"refresh": old_refresh},
            format="json",
        )
        self.assertEqual(stale_refresh.status_code, status.HTTP_401_UNAUTHORIZED)

        self.authorize(changed.data["access"])
        fresh = self.client.get("/api/v1/me/")
        self.assertEqual(fresh.status_code, status.HTTP_200_OK)

    def test_revoke_other_sessions_keeps_password_but_reissues_current_session(self):
        first = self.login()
        second = self.login()

        self.authorize(second["access"])
        revoked = self.client.post(
            "/api/v1/auth/sessions/revoke-others/",
            {"current_password": PASSWORD},
            format="json",
        )
        self.assertEqual(revoked.status_code, status.HTTP_200_OK)

        self.authorize(first["access"])
        old_device = self.client.get("/api/v1/me/")
        self.assertEqual(old_device.status_code, status.HTTP_401_UNAUTHORIZED)

        self.client.credentials()
        password_still_works = self.client.post(
            "/api/v1/auth/login/",
            {"email": self.user.email, "password": PASSWORD},
            format="json",
        )
        self.assertEqual(password_still_works.status_code, status.HTTP_200_OK)

    def test_new_tokens_are_password_bound(self):
        session = self.login()
        access = AccessToken(session["access"])
        self.assertTrue(access.get("pwdv"))
