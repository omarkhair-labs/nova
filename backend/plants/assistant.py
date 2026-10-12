"""Plantupia's permission-scoped, text-only AI care assistant.

No company or sensor data is guessed. Provider is opt-in by server configuration;
API credentials never enter the mobile client, Git repository or public posts.
"""
import json
import os
from datetime import date
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from django.core.cache import cache
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import PlantEvent
from .views import accessible_plants

BASE_INSTRUCTIONS = (
    "You are Plantupia's careful educational plant-care assistant. "
    "Reply in the language of the user's question (including natural Arabic when appropriate). "
    "Help the person inspect soil, drainage, light, seasons and previous care before making assumptions. "
    "Differentiate possible causes from facts and clearly state uncertainty. "
    "Do not invent species, sensor readings, precise moisture numbers, diagnoses, product warranties, "
    "company visits, service entitlements, or expert consultations. Plantupia has no plant sensors. "
    "Do not promise cures, specific watering calendars, pesticides or dangerous treatments. "
    "If the plant context is empty, state that you do not have private plant information. "
    "Never claim to have looked up a source if no verified reference was supplied. "
    "A note is what a person reported; it is not automatically measured data. "
    "You cannot write to Plantupia records or message other users. "
    "Ask one or two useful clarifying questions if facts are missing. "
    "Keep initial replies useful and relatively concise."
)


def call_provider(message, plant_context):
    """OpenAI Responses API; no retained response, bounded output and hard timeout."""
    key = os.getenv("PLANTUPIA_AI_API_KEY", "").strip()
    model = os.getenv("PLANTUPIA_AI_MODEL", "gpt-5.4-mini").strip()
    if not key or not model:
        raise RuntimeError("provider_not_configured")
    prompt = (
        message if not plant_context else
        "User-opted-in plant data (reported observations, NOT verified measurements):\n"
        + json.dumps(plant_context, ensure_ascii=False)
        + "\n\nUser question:\n" + message
    )
    payload = {
        "model": model,
        "instructions": BASE_INSTRUCTIONS,
        "input": prompt,
        "max_output_tokens": 650,
        "store": False,
    }
    request = Request(
        "https://api.openai.com/v1/responses",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": "Bearer " + key,
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urlopen(request, timeout=18) as response:
            result = json.loads(response.read(250_000).decode("utf-8"))
    except (HTTPError, URLError, TimeoutError, ValueError) as exc:
        # Never return exception bodies: some providers echo request or secrets.
        raise RuntimeError("provider_unavailable") from exc
    pieces = []
    for item in result.get("output", []):
        if item.get("type") != "message":
            continue
        for entry in item.get("content", []):
            if entry.get("type") == "output_text" and isinstance(entry.get("text"), str):
                pieces.append(entry["text"])
    answer = "\n".join(pieces).strip()
    if not answer or len(answer) > 10000:
        raise RuntimeError("invalid_provider_response")
    return answer


class PlantAssistantView(APIView):
    """Read-only context; user consent is per request, checked on the server."""

    permission_classes = [IsAuthenticated]

    def post(self, request):
        raw = request.data
        message = raw.get("message")
        plant_id = raw.get("plant_id")
        include_context = raw.get("include_my_plant_context", False)

        if not isinstance(message, str) or not message.strip() or len(message) > 1000:
            return Response({"detail": "Provide a question of 1–1000 characters."}, status=400)
        if not isinstance(include_context, bool):
            return Response({"detail": "Context consent must be a boolean."}, status=400)
        if plant_id is not None and not isinstance(plant_id, str):
            return Response({"detail": "Invalid plant ID."}, status=400)

        plant = None
        if plant_id:
            # A guessed or cross-account plant UUID must never disclose its existence.
            plant = get_object_or_404(accessible_plants(request.user), pk=plant_id)
        elif include_context:
            return Response({"detail": "Choose a plant before including private context."}, status=400)

        # No user content is forwarded to any provider until enabled explicitly.
        if os.getenv("PLANTUPIA_AI_ENABLED", "0") != "1" or not os.getenv("PLANTUPIA_AI_API_KEY"):
            return Response(
                {"detail": "The Plantupia assistant is not enabled on this server yet.", "code": "not_configured"},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        # Local throttling works for QA. Configure a SHARED Django cache such as Redis
        # plus upstream provider budget caps before allowing paid production usage.
        daily_limit = 35
        key = "plantupia-ai-v1:%s:%s" % (request.user.pk, date.today().isoformat())
        first = cache.add(key, 1, timeout=24 * 3600)
        try:
            count = 1 if first else cache.incr(key)
        except ValueError:
            cache.set(key, 1, timeout=24 * 3600)
            count = 1
        if count > daily_limit:
            return Response(
                {"detail": "Daily assistant limit reached. Please try again tomorrow.", "code": "rate_limit"},
                status=status.HTTP_429_TOO_MANY_REQUESTS,
            )

        context = None
        if plant is not None and include_context:
            nickname = next(
                (name.nickname for name in plant.nicknames.all() if name.user_id == request.user.pk),
                plant.label,
            )
            notes = list(
                PlantEvent.objects.filter(plant=plant)
                .order_by("-observed_at", "-created_at")
                .values("kind", "note", "observed_at")[:6]
            )
            context = {
                "personal_name": nickname,
                "species_label": plant.species_label or "unknown",
                "placement_note": plant.placement_note,
                "recent_person_observations": [
                    {
                        "kind": event["kind"],
                        "note": (event["note"] or "")[:450],
                        "observed_on": event["observed_at"].date().isoformat(),
                    }
                    for event in notes
                ],
            }

        try:
            answer = call_provider(message.strip(), context)
        except RuntimeError:
            return Response(
                {"detail": "The assistant is temporarily unavailable. Your care history was not changed.", "code": "unavailable"},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        return Response({
            "answer": answer,
            "scope": "plant_context" if context is not None else "general",
            "source_links": [],  # No RAG corpus is connected; do not invent citations.
            "uncertainty": "AI-generated educational guidance; not a confirmed diagnosis.",
            "can_write_care_history": False,
        })
