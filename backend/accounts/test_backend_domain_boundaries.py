from pathlib import Path

from django.test import SimpleTestCase
from django.urls import resolve, reverse

from .routing import websocket_urlpatterns


API_ROUTE_CASES = (
    ("health", {}, "/api/v1/health/"),
    ("register", {}, "/api/v1/auth/register/"),
    ("login", {}, "/api/v1/auth/login/"),
    ("token-refresh", {}, "/api/v1/auth/refresh/"),
    ("password-reset-request", {}, "/api/v1/auth/password/reset/request/"),
    ("password-reset-confirm", {}, "/api/v1/auth/password/reset/confirm/"),
    ("password-change", {}, "/api/v1/auth/password/change/"),
    ("revoke-other-sessions", {}, "/api/v1/auth/sessions/revoke-others/"),
    ("account-delete", {}, "/api/v1/auth/account/delete/"),
    ("blocked-users", {}, "/api/v1/auth/blocks/"),
    ("me", {}, "/api/v1/me/"),
    ("account-privacy", {}, "/api/v1/privacy/"),
    ("follow-requests", {}, "/api/v1/follow-requests/"),
    ("follow-request-accept", {"request_id": 7}, "/api/v1/follow-requests/7/accept/"),
    ("follow-request-decline", {"request_id": 7}, "/api/v1/follow-requests/7/decline/"),
    ("close-friends", {}, "/api/v1/close-friends/"),
    ("close-friend-detail", {"username": "alice"}, "/api/v1/close-friends/alice/"),
    ("people", {}, "/api/v1/people/"),
    ("person-detail", {"username": "alice"}, "/api/v1/people/alice/"),
    ("person-posts", {"username": "alice"}, "/api/v1/people/alice/posts/"),
    ("person-reposts", {"username": "alice"}, "/api/v1/people/alice/reposts/"),
    ("person-followers", {"username": "alice"}, "/api/v1/people/alice/followers/"),
    ("person-following-list", {"username": "alice"}, "/api/v1/people/alice/following/"),
    ("person-follow", {"username": "alice"}, "/api/v1/people/alice/follow/"),
    ("person-block", {"username": "alice"}, "/api/v1/people/alice/block/"),
    ("person-report", {"username": "alice"}, "/api/v1/people/alice/report/"),
    ("stories", {}, "/api/v1/stories/"),
    ("story-detail", {"story_id": 7}, "/api/v1/stories/7/"),
    ("story-view", {"story_id": 7}, "/api/v1/stories/7/view/"),
    ("story-viewers", {"story_id": 7}, "/api/v1/stories/7/viewers/"),
    ("story-reaction", {"story_id": 7}, "/api/v1/stories/7/reaction/"),
    ("story-reply", {"story_id": 7}, "/api/v1/stories/7/reply/"),
    ("posts", {}, "/api/v1/posts/"),
    ("post-detail", {"post_id": 7}, "/api/v1/posts/7/"),
    ("post-like", {"post_id": 7}, "/api/v1/posts/7/like/"),
    ("post-repost", {"post_id": 7}, "/api/v1/posts/7/repost/"),
    ("post-comments", {"post_id": 7}, "/api/v1/posts/7/comments/"),
    ("comment-detail", {"comment_id": 7}, "/api/v1/comments/7/"),
    ("comment-reply-detail", {"reply_id": 7}, "/api/v1/comment-replies/7/"),
    ("feed", {}, "/api/v1/feed/"),
    ("message-share", {}, "/api/v1/shares/messages/"),
    ("notifications", {}, "/api/v1/notifications/"),
    ("notifications-read", {}, "/api/v1/notifications/read/"),
    ("push-devices", {}, "/api/v1/push/devices/"),
    ("reels", {}, "/api/v1/reels/"),
    ("profile-reels", {"username": "alice"}, "/api/v1/reels/profile/alice/"),
    ("reel-detail", {"reel_id": 7}, "/api/v1/reels/7/"),
    ("reel-watch", {"reel_id": 7}, "/api/v1/reels/7/watch/"),
    ("reel-like", {"reel_id": 7}, "/api/v1/reels/7/like/"),
    ("reel-repost", {"reel_id": 7}, "/api/v1/reels/7/repost/"),
    ("reel-comments", {"reel_id": 7}, "/api/v1/reels/7/comments/"),
    ("reel-comment-detail", {"comment_id": 7}, "/api/v1/reel-comments/7/"),
    ("reel-comment-reply-detail", {"reply_id": 7}, "/api/v1/reel-comment-replies/7/"),
    ("call-create", {}, "/api/v1/calls/"),
    ("call-ice-config", {}, "/api/v1/calls/ice/"),
    (
        "call-detail",
        {"call_id": "123e4567-e89b-12d3-a456-426614174000"},
        "/api/v1/calls/123e4567-e89b-12d3-a456-426614174000/",
    ),
    (
        "call-action",
        {"call_id": "123e4567-e89b-12d3-a456-426614174000"},
        "/api/v1/calls/123e4567-e89b-12d3-a456-426614174000/action/",
    ),
    ("conversations", {}, "/api/v1/conversations/"),
    ("group-conversation-create", {}, "/api/v1/conversations/groups/"),
    ("group-conversation-detail", {"conversation_id": 7}, "/api/v1/conversations/7/group/"),
    ("group-management-detail", {"conversation_id": 7}, "/api/v1/conversations/7/group/manage/"),
    ("group-members", {"conversation_id": 7}, "/api/v1/conversations/7/group/members/"),
    (
        "group-member-detail",
        {"conversation_id": 7, "username": "alice"},
        "/api/v1/conversations/7/group/members/alice/",
    ),
    (
        "group-member-role",
        {"conversation_id": 7, "username": "alice"},
        "/api/v1/conversations/7/group/members/alice/role/",
    ),
    ("conversation-messages", {"conversation_id": 7}, "/api/v1/conversations/7/messages/"),
    (
        "conversation-message-search",
        {"conversation_id": 7},
        "/api/v1/conversations/7/messages/search/",
    ),
    (
        "conversation-message-context",
        {"conversation_id": 7},
        "/api/v1/conversations/7/messages/context/",
    ),
    ("conversation-media", {"conversation_id": 7}, "/api/v1/conversations/7/media/"),
    (
        "conversation-preferences",
        {"conversation_id": 7},
        "/api/v1/conversations/7/preferences/",
    ),
    ("conversation-read", {"conversation_id": 7}, "/api/v1/conversations/7/read/"),
    ("message-detail", {"message_id": 7}, "/api/v1/messages/7/"),
    ("message-reaction", {"message_id": 7}, "/api/v1/messages/7/reaction/"),
)

PUBLIC_ROUTE_CASES = (
    ("privacy-policy", "/privacy/"),
    ("account-deletion", "/account-deletion/"),
    ("child-safety", "/child-safety/"),
)

EXPECTED_WEBSOCKET_PATTERNS = (
    r"^ws/presence/$",
    r"^ws/conversations/(?P<conversation_id>\d+)/$",
    r"^ws/calls/(?P<call_id>[0-9a-fA-F-]+)/$",
)


class BackendRouteContractTests(SimpleTestCase):
    def test_all_72_named_api_routes_reverse_and_resolve_exactly(self):
        self.assertEqual(len(API_ROUTE_CASES), 72)
        self.assertEqual(len({name for name, _, _ in API_ROUTE_CASES}), 72)

        for name, kwargs, expected_path in API_ROUTE_CASES:
            with self.subTest(name=name):
                self.assertEqual(reverse(name, kwargs=kwargs or None), expected_path)
                self.assertEqual(resolve(expected_path).url_name, name)

    def test_public_legal_routes_remain_exact(self):
        for name, expected_path in PUBLIC_ROUTE_CASES:
            with self.subTest(name=name):
                self.assertEqual(reverse(name), expected_path)
                self.assertEqual(resolve(expected_path).url_name, name)

    def test_all_three_websocket_regexes_remain_exact_and_ordered(self):
        actual = tuple(pattern.pattern.regex.pattern for pattern in websocket_urlpatterns)
        self.assertEqual(actual, EXPECTED_WEBSOCKET_PATTERNS)


class BackendDomainBoundaryTests(SimpleTestCase):
    def test_collision_safe_domain_boundaries_exist(self):
        accounts_dir = Path(__file__).resolve().parent
        required = (
            "api/urls.py",
            "auth/urls.py",
            "social/urls.py",
            "posts/urls.py",
            "notifications/urls.py",
            "sharing/urls.py",
            "messaging/urls.py",
            "messaging/routing.py",
            "privacy/urls.py",
            "trust_safety/urls.py",
            "stories/urls.py",
            "reels/urls.py",
            "calls_urls.py",
            "calls_routing.py",
        )
        missing = [relative for relative in required if not (accounts_dir / relative).is_file()]
        self.assertEqual(missing, [])

    def test_root_url_and_websocket_files_are_composition_only(self):
        accounts_dir = Path(__file__).resolve().parent
        urls_source = (accounts_dir / "urls.py").read_text(encoding="utf-8")
        routing_source = (accounts_dir / "routing.py").read_text(encoding="utf-8")

        self.assertEqual(urls_source.strip(), "from .api.urls import urlpatterns")
        self.assertNotIn("Consumer", routing_source)
        self.assertIn(".messaging.routing", routing_source)
        self.assertIn(".calls_routing", routing_source)

    def test_owned_collisions_advance_to_packages(self):
        accounts_dir = Path(__file__).resolve().parent
        for module in ("privacy", "trust_safety", "stories", "reels"):
            with self.subTest(module=module):
                self.assertFalse((accounts_dir / f"{module}.py").exists())
                self.assertTrue((accounts_dir / module / "__init__.py").is_file())
                self.assertTrue((accounts_dir / module / "urls.py").is_file())

    def test_remaining_flat_collisions_are_deferred_to_their_owning_prs(self):
        accounts_dir = Path(__file__).resolve().parent
        self.assertTrue((accounts_dir / "calls.py").is_file())
        self.assertFalse((accounts_dir / "calls").exists())

    def test_model_and_migration_identity_stay_in_existing_accounts_app(self):
        accounts_dir = Path(__file__).resolve().parent
        self.assertTrue((accounts_dir / "models.py").is_file())
        self.assertTrue((accounts_dir / "migrations").is_dir())
        self.assertFalse((accounts_dir / "posts" / "models.py").exists())

        for domain in ("auth", "stories", "reels", "messaging"):
            adapter = (accounts_dir / domain / "models.py").read_text(encoding="utf-8")
            self.assertIn("from ..models import *", adapter)
            self.assertNotIn("class ", adapter)

        messaging_model_adapter = (
            accounts_dir / "messaging" / "messaging_models.py"
        ).read_text(encoding="utf-8")
        self.assertIn("from ..messaging_models import *", messaging_model_adapter)
        self.assertNotIn("class ", messaging_model_adapter)
