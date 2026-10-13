"""Offline contract tests: zero live provider usage or stored credentials."""
import json
import os
from io import BytesIO
from unittest.mock import patch

from django.test import SimpleTestCase

from .assistant import call_provider


class _ProviderResponse:
    def __init__(self, data):
        self._bytes = BytesIO(json.dumps(data).encode("utf-8"))

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def read(self, limit):
        return self._bytes.read(limit)


class ProviderAdapterTests(SimpleTestCase):
    def test_free_groq_payload_has_system_instructions_and_no_private_context_by_default(self):
        captured = []

        def intercept(request, timeout):
            captured.append((request.full_url, json.loads(request.data.decode("utf-8")), timeout))
            return _ProviderResponse({"choices": [{"message": {"content": "Check the soil first."}}]})

        with patch.dict(os.environ, {
            "PLANTUPIA_AI_PROVIDER": "groq",
            "PLANTUPIA_AI_MODEL": "openai/gpt-oss-20b",
            "PLANTUPIA_AI_API_KEY": "test-only-key",
        }), patch("plants.assistant.urlopen", side_effect=intercept):
            answer = call_provider("When should I water?", None, [
                {"role": "assistant", "content": "What does the soil feel like?"},
            ])
        self.assertEqual(answer, "Check the soil first.")
        endpoint, payload, timeout = captured[0]
        self.assertEqual(endpoint, "https://api.groq.com/openai/v1/chat/completions")
        self.assertEqual(payload["model"], "openai/gpt-oss-20b")
        self.assertEqual(payload["messages"][0]["role"], "system")
        self.assertEqual(payload["messages"][-1], {"role": "user", "content": "When should I water?"})
        self.assertNotIn("personal_name", json.dumps(payload))
        self.assertNotIn("test-only-key", json.dumps(payload))
        self.assertEqual(timeout, 18)

    def test_only_explicit_consented_context_reaches_groq_messages(self):
        captured = []

        def intercept(request, timeout):
            captured.append(json.loads(request.data.decode("utf-8")))
            return _ProviderResponse({"choices": [{"message": {"content": "Inspect drainage."}}]})

        with patch.dict(os.environ, {
            "PLANTUPIA_AI_PROVIDER": "groq",
            "PLANTUPIA_AI_API_KEY": "test-only-key",
        }), patch("plants.assistant.urlopen", side_effect=intercept):
            call_provider("Please help.", {"personal_name": "Private alias"}, [])
        self.assertIn("Private alias", json.dumps(captured[0]["messages"]))
        self.assertEqual(captured[0]["model"], "openai/gpt-oss-20b")

    def test_groq_missing_or_empty_answer_is_rejected_not_fabricated(self):
        with patch.dict(os.environ, {
            "PLANTUPIA_AI_PROVIDER": "groq",
            "PLANTUPIA_AI_API_KEY": "test-only-key",
        }), patch("plants.assistant.urlopen", return_value=_ProviderResponse({"choices": []})):
            with self.assertRaisesRegex(RuntimeError, "invalid_provider_response"):
                call_provider("Question", None)

    def test_invalid_provider_fails_closed_without_network(self):
        with patch.dict(os.environ, {
            "PLANTUPIA_AI_PROVIDER": "unsupported",
            "PLANTUPIA_AI_API_KEY": "test-only-key",
        }), patch("plants.assistant.urlopen") as network:
            with self.assertRaisesRegex(RuntimeError, "provider_not_configured"):
                call_provider("Question", None)
        network.assert_not_called()

    def test_openai_responses_behavior_stays_compatible(self):
        captured = []

        def intercept(request, timeout):
            captured.append(json.loads(request.data.decode("utf-8")))
            return _ProviderResponse({
                "output": [{"type": "message", "content": [
                    {"type": "output_text", "text": "Review the roots."},
                ]}],
            })

        with patch.dict(os.environ, {
            "PLANTUPIA_AI_PROVIDER": "openai",
            "PLANTUPIA_AI_MODEL": "gpt-5.4-mini",
            "PLANTUPIA_AI_API_KEY": "test-only-key",
        }), patch("plants.assistant.urlopen", side_effect=intercept):
            self.assertEqual(call_provider("Question", None), "Review the roots.")
        self.assertFalse(captured[0]["store"])
