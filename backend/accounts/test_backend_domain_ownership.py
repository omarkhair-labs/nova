import importlib

from django.test import SimpleTestCase
from django.urls import resolve

from .messaging.routing import websocket_urlpatterns as messaging_websocket_urlpatterns


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

    def test_stories_and_reels_routes_use_domain_owners(self):
        expected = {
            "/api/v1/stories/": "accounts.stories",
            "/api/v1/stories/1/": "accounts.stories",
            "/api/v1/stories/1/view/": "accounts.stories",
            "/api/v1/stories/1/viewers/": "accounts.stories",
            "/api/v1/stories/1/reaction/": "accounts.stories",
            "/api/v1/stories/1/reply/": "accounts.stories",
            "/api/v1/reels/": "accounts.reels",
            "/api/v1/reels/profile/alice/": "accounts.reels.profile",
            "/api/v1/reels/1/": "accounts.reels",
            "/api/v1/reels/1/watch/": "accounts.reels",
            "/api/v1/reels/1/like/": "accounts.reels",
            "/api/v1/reels/1/repost/": "accounts.reels",
            "/api/v1/reels/1/comments/": "accounts.reels.comments",
            "/api/v1/reel-comments/1/": "accounts.reels.comments",
            "/api/v1/reel-comment-replies/1/": "accounts.reels.comments",
        }
        for path, module_name in expected.items():
            with self.subTest(path=path):
                match = resolve(path)
                self.assertEqual(match.func.view_class.__module__, module_name)

    def test_messaging_routes_use_domain_owners(self):
        expected = {
            "/api/v1/conversations/": "accounts.messaging.paging",
            "/api/v1/conversations/groups/": "accounts.messaging.group_messaging",
            "/api/v1/conversations/1/group/": "accounts.messaging.group_messaging",
            "/api/v1/conversations/1/group/manage/": "accounts.messaging.group_management",
            "/api/v1/conversations/1/group/members/": "accounts.messaging.group_messaging",
            "/api/v1/conversations/1/group/members/alice/": "accounts.messaging.group_messaging",
            "/api/v1/conversations/1/group/members/alice/role/": "accounts.messaging.group_management",
            "/api/v1/conversations/1/messages/": "accounts.messaging.messaging_views",
            "/api/v1/conversations/1/messages/search/": "accounts.messaging.tools",
            "/api/v1/conversations/1/messages/context/": "accounts.messaging.tools",
            "/api/v1/conversations/1/media/": "accounts.messaging.tools",
            "/api/v1/conversations/1/preferences/": "accounts.messaging.tools",
            "/api/v1/conversations/1/read/": "accounts.messaging.messaging_views",
            "/api/v1/messages/1/": "accounts.messaging.mutation",
            "/api/v1/messages/1/reaction/": "accounts.messaging.messaging_views",
        }
        for path, module_name in expected.items():
            with self.subTest(path=path):
                match = resolve(path)
                self.assertEqual(match.func.view_class.__module__, module_name)

    def test_messaging_websocket_consumers_use_domain_owner(self):
        self.assertEqual(len(messaging_websocket_urlpatterns), 2)
        owners = tuple(
            pattern.callback.consumer_class.__module__
            for pattern in messaging_websocket_urlpatterns
        )
        self.assertEqual(
            owners,
            ("accounts.messaging.realtime", "accounts.messaging.realtime"),
        )

    def test_legacy_module_paths_alias_new_owners(self):
        aliases = (
            ("accounts.account_security", "accounts.auth.security"),
            ("accounts.jwt_auth", "accounts.auth.jwt_auth"),
            ("accounts.social_paging", "accounts.social.paging"),
            ("accounts.privacy_views", "accounts.privacy.views"),
            ("accounts.sharing_views", "accounts.sharing.views"),
            ("accounts.profile_reels", "accounts.reels.profile"),
            ("accounts.reels_ranking", "accounts.reels.ranking"),
            ("accounts.group_management", "accounts.messaging.group_management"),
            ("accounts.group_messaging", "accounts.messaging.group_messaging"),
            ("accounts.messaging_mutation_view", "accounts.messaging.mutation"),
            ("accounts.messaging_paging", "accounts.messaging.paging"),
            ("accounts.messaging_realtime", "accounts.messaging.messaging_realtime"),
            ("accounts.messaging_serializers", "accounts.messaging.messaging_serializers"),
            ("accounts.messaging_v9_views", "accounts.messaging.tools"),
            ("accounts.messaging_views", "accounts.messaging.messaging_views"),
            ("accounts.presence_store", "accounts.messaging.presence_store"),
            ("accounts.realtime", "accounts.messaging.realtime"),
        )
        for legacy_name, owner_name in aliases:
            with self.subTest(legacy=legacy_name):
                legacy = importlib.import_module(legacy_name)
                owner = importlib.import_module(owner_name)
                self.assertIs(legacy, owner)

    def test_collision_modules_are_now_real_domain_packages(self):
        for module_name in (
            "accounts.privacy",
            "accounts.trust_safety",
            "accounts.stories",
            "accounts.reels",
        ):
            with self.subTest(module=module_name):
                module = importlib.import_module(module_name)
                self.assertTrue(hasattr(module, "__path__"))
