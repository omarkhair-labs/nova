"""Test the hosted preview's database and deployment guard without network access."""
import unittest

from hosted_living import QA_DB, QA_HOST, QA_REDIS, verified_preview


class HostedPreviewGuardTests(unittest.TestCase):
    def setUp(self):
        self.env = {
            "COOLIFY_URL": "https://" + QA_HOST,
            "COOLIFY_FQDN": QA_HOST,
            "DJANGO_ALLOWED_HOSTS": QA_HOST + ",localhost",
            "DJANGO_ENV": "production",
            "DJANGO_DEBUG": "0",
            "PLANTUPIA_PREVIEW_HOSTED_QA_ENABLED": "1",
            "PLANTUPIA_HANDOVER_QA_ENABLED": "1",
            "DATABASE_URL": "postgresql://test:test@" + QA_DB + ":5432/test",
            "REDIS_URL": "redis://test:test@" + QA_REDIS + ":6379/0",
        }

    def test_exact_preview(self):
        self.assertTrue(verified_preview(self.env))

    def test_wrong_or_missing_preview_configuration(self):
        for key, value in (
            ("COOLIFY_URL", "https://production.example.test"),
            ("COOLIFY_FQDN", "241." + QA_HOST.split(".", 1)[1]),
            ("DATABASE_URL", "postgresql://test:test@production/test"),
            ("REDIS_URL", "redis://production/0"),
            ("DJANGO_DEBUG", "1"),
            ("PLANTUPIA_HANDOVER_QA_ENABLED", "0"),
            ("PLANTUPIA_PREVIEW_HOSTED_QA_ENABLED", "0"),
            ("DJANGO_ALLOWED_HOSTS", "localhost"),
        ):
            with self.subTest(key=key):
                self.assertFalse(verified_preview({**self.env, key: value}))
        self.assertFalse(verified_preview({**self.env, "COOLIFY_URL": "", "COOLIFY_FQDN": ""}))
