"""Regression tests for the local-only Plantupia demo credential output.

The generated file contains a one-time handover invitation and passwords.
No live backend, production database, or account credentials are used here.
"""
import json
import os
import stat
import tempfile
from pathlib import Path
from unittest.mock import patch

from django.test import SimpleTestCase

from .management.commands.seed_local_plantupia_qa import write_private_qa_credentials


class LocalQaCredentialFileTests(SimpleTestCase):
    def test_credentials_are_created_exclusively_and_owner_readable_only(self):
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory) / "private-demo.json"
            payload = {"synthetic": True, "invitation": "test-secret"}
            write_private_qa_credentials(destination, payload)
            self.assertEqual(json.loads(destination.read_text(encoding="utf-8")), payload)
            if os.name == "posix":
                self.assertEqual(stat.S_IMODE(destination.stat().st_mode), 0o600)

    def test_existing_file_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory) / "private-demo.json"
            destination.write_text("original", encoding="utf-8")
            with self.assertRaises(FileExistsError):
                write_private_qa_credentials(destination, {"invitation": "new"})
            self.assertEqual(destination.read_text(encoding="utf-8"), "original")

    def test_partial_output_is_removed_on_serialization_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory) / "private-demo.json"
            with patch(
                "plants.management.commands.seed_local_plantupia_qa.json.dump",
                side_effect=ValueError("synthetic serialization error"),
            ):
                with self.assertRaisesRegex(ValueError, "synthetic serialization error"):
                    write_private_qa_credentials(destination, {"synthetic": True})
            self.assertFalse(destination.exists())
