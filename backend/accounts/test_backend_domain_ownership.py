import importlib

from django.test import SimpleTestCase
from django.urls import resolve


class BackendDomainOwnershipTests(SimpleTestCase):
    def test_auth_social_privacy_and_trust_routes_use_domain_owners(self):
        expected = {
            "/api/v1/auth/register/": "accounts.auth.security",
            "/api/v1/auth/login/": "accounts.auth.security",
            "/api/v1/auth/refresh/": "accounts.auth.security",
            "/api/v1/auth/password/reset/request/": "accounts.auth.security",
            "/api/v1/auth/password/reset/confirm/": "accounts.auth.security",
            "/api/v1/auth/password/change/": "accounts.auth.security",
            "/api/v1/auth/sessions/revoke-others/": "accounts.auth.security",
            "/api/v1/me/": "accounts.auth.views",
            "/api/v1/people/": "accounts.social.paging",
            "/api/v1/people/alice/": "accounts.social.views",
            "/api/v1/people/alice/posts/": "accounts.social.paging",
            "/api/v1/people/alice/reposts/": "accounts.social.paging",
            "/api/v1/people/alice/followers/": "accounts.social.paging",
            "/api/v1/people/alice/following/": "accounts.social.paging",
            "/api/v1/people/alice/follow/": "accounts.social.views",
            "/api/v1/privacy/": "accounts.privacy.views",
            "/api/v1/follow-requests/": "accounts.privacy.views",
            "/api/v1/follow-requests/1/accept/": "accounts.privacy.views",
            "/api/v1/follow-requests/1/decline/": "accounts.privacy.views",
            "/api/v1/close-friends/": "accounts.privacy.views",
            "/api/v1/close-friends/alice/": "accounts.privacy.views",
            "/api/v1/auth/account/delete/": "accounts.trust_safety",
            "/api/v1/auth/blocks/": "accounts.trust_safety",
            "/api/v1/people/alice/block/": "accounts.trust_safety",
            "/api/v1/people/alice/report/": "accounts.trust_safety",
        }
        for path, module_name in expected.items():
            with self.subTest(path=path):
                match = resolve(path)
                self.assertEqual(match.func.view_class.__module__, module_name)

    def test_posts_notifications_and_sharing_routes_use_domain_owners(self):
        expected = {
            "/api/v1/posts/": "accounts.posts.views",
            "/api/v1/posts/1/": "accounts.posts.views",
            "/api/v1/posts/1/like/": "accounts.posts.views",
            "/api/v1/posts/1/comments/": "accounts.posts.comments",
            "/api/v1/comments/1/": "accounts.posts.comments",
            "/api/v1/comment-replies/1/": "accounts.posts.comments",
            "/api/v1/posts/1/repost/": "accounts.sharing.views",
            "/api/v1/feed/": "accounts.sharing.views",
            "/api/v1/shares/messages/": "accounts.sharing.views",
            "/api/v1/notifications/": "accounts.notifications.views",
            "/api/v1/notifications/read/": "accounts.notifications.views",
            "/api/v1/push/devices/": "accounts.notifications.views",
        }
        for path, module_name in expected.items():
            with self.subTest(path=path):
                match = resolve(path)
                self.assertEqual(match.func.view_class.__module__, module_name)

    def test_legacy_module_paths_alias_new_owners(self):
        aliases = (
            ("accounts.account_security", "accounts.auth.security"),
            ("accounts.jwt_auth", "accounts.auth.jwt_auth"),
            ("accounts.social_paging", "accounts.social.paging"),
            ("accounts.privacy_views", "accounts.privacy.views"),
            ("accounts.sharing_views", "accounts.sharing.views"),
        )
        for legacy_name, owner_name in aliases:
            with self.subTest(legacy=legacy_name):
                legacy = importlib.import_module(legacy_name)
                owner = importlib.import_module(owner_name)
                self.assertIs(legacy, owner)

    def test_collision_modules_are_now_real_domain_packages(self):
        privacy = importlib.import_module("accounts.privacy")
        trust_safety = importlib.import_module("accounts.trust_safety")
        self.assertTrue(hasattr(privacy, "__path__"))
        self.assertTrue(hasattr(trust_safety, "__path__"))
