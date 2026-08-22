from pathlib import Path

from django.test import SimpleTestCase


class Phase6ExitArchitectureTests(SimpleTestCase):
    def test_tests_module_collision_is_gone(self):
        accounts_dir = Path(__file__).resolve().parents[1]
        self.assertFalse((accounts_dir / "tests.py").exists())
        self.assertTrue((accounts_dir / "tests" / "__init__.py").is_file())
        self.assertTrue((accounts_dir / "test_core_api.py").is_file())

    def test_live_composition_uses_domain_packages_directly(self):
        accounts_dir = Path(__file__).resolve().parents[1]
        api_source = (accounts_dir / "api" / "urls.py").read_text(encoding="utf-8")
        routing_source = (accounts_dir / "routing.py").read_text(encoding="utf-8")

        for module in (
            "accounts.auth.urls",
            "accounts.trust_safety.urls",
            "accounts.privacy.urls",
            "accounts.social.urls",
            "accounts.stories.urls",
            "accounts.posts.urls",
            "accounts.sharing.urls",
            "accounts.notifications.urls",
            "accounts.reels.urls",
            "accounts.calls.urls",
            "accounts.messaging.urls",
        ):
            with self.subTest(module=module):
                self.assertIn(module, api_source)

        self.assertIn(".messaging.routing", routing_source)
        self.assertIn(".calls.routing", routing_source)

        for legacy_adapter in (
            "stories_urls.py",
            "reels_urls.py",
            "calls_urls.py",
            "calls_routing.py",
        ):
            with self.subTest(adapter=legacy_adapter):
                self.assertFalse((accounts_dir / legacy_adapter).exists())

    def test_call_history_startup_hook_uses_domain_owner(self):
        accounts_dir = Path(__file__).resolve().parents[1]
        apps_source = (accounts_dir / "apps.py").read_text(encoding="utf-8")
        self.assertIn("from .calls import call_history", apps_source)
        self.assertNotIn("from . import call_history", apps_source)
