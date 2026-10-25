"""Fixed-identity Oracle Preview #241 handover gate.

Even when public Django is hardened as production, a copied flag must not
turn on client handover for Nova or an unrecognized database/preview.
"""
import os
from unittest.mock import patch

from django.conf import settings
from django.test import SimpleTestCase, override_settings

from .handovers import pilot_enabled

QA_HOST = "241.zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io"
QA_DATABASE = "imfiwc6cb0pkuyuzu1y1rxsu"
QA_SETTINGS = {
    "ENGINE": "django.db.backends.postgresql", "HOST": QA_DATABASE,
    "NAME": "postgres",
}


class HostedPreviewHandoverConfigurationTests(SimpleTestCase):
    def assert_gate(self, expected, *, env="production", host=QA_DATABASE,
                    allowed_hosts=None, debug=False, preview_flag="1", qa_flag="1"):
        with (
            override_settings(
                IS_PRODUCTION=(env == "production"),
                DEBUG=debug,
                ALLOWED_HOSTS=allowed_hosts if allowed_hosts is not None else [QA_HOST],
                DATABASES={"default": {**QA_SETTINGS, "HOST": host}},
            ),
            patch.dict(os.environ, {
                "PLANTUPIA_HANDOVER_QA_ENABLED": qa_flag,
                "PLANTUPIA_PREVIEW_HOSTED_QA_ENABLED": preview_flag,
            }),
        ):
            self.assertEqual(pilot_enabled(), expected)

    def test_explicit_hardened_oracle_241_preview_can_issue(self):
        self.assert_gate(True)

    def test_both_flags_must_be_enabled(self):
        self.assert_gate(False, qa_flag="0")
        self.assert_gate(False, preview_flag="0")

    def test_public_nova_and_other_database_are_never_enabled(self):
        self.assert_gate(False, host="ptrfqssosjhwn99g1ogfzlhf")
        self.assert_gate(False, host="unrelated-postgres")

    def test_copied_flags_on_other_preview_or_host_cannot_enable(self):
        self.assert_gate(False, allowed_hosts=["240.zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io"])

    def test_debug_server_cannot_enable_hosted_gate(self):
        self.assert_gate(False, debug=True)

    @override_settings(IS_PRODUCTION=True, DEBUG=False)
    def test_production_default_remains_off_despite_hosted_flag(self):
        with patch.dict(os.environ, {
            "PLANTUPIA_PREVIEW_HOSTED_QA_ENABLED": "1",
            "PLANTUPIA_HANDOVER_QA_ENABLED": "1",
        }):
            self.assertFalse(pilot_enabled())
