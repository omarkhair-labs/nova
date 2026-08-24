from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from ..memory_models import MemoryDraft


MAX_DRAFT_MEDIA_BYTES = 60 * 1024 * 1024


def serialize_draft(request, draft):
    media_url = ""
    if draft.media:
        try:
            media_url = request.build_absolute_uri(draft.media.url)
        except Exception:
            media_url = ""
    return {
        "id": draft.pk,
        "kind": draft.kind,
        "title": draft.title,
        "note": draft.note,
        "media_url": media_url,
        "media_type": draft.media_type,
        "created_at": draft.created_at,
        "updated_at": draft.updated_at,
    }


def clean_values(request, *, partial=False):
    values = {}
    if not partial or "title" in request.data:
        title = str(request.data.get("title") or "").strip()
        if not title:
            return None, "Give this Memory a title."
        if len(title) > 120:
            return None, "Memory title must be 120 characters or fewer."
        values["title"] = title
    if not partial or "kind" in request.data:
        kind = str(request.data.get("kind") or MemoryDraft.Kind.RECAP).strip().lower()
        if kind not in MemoryDraft.Kind.values:
            return None, "Choose a recap or film Memory."
        values["kind"] = kind
    if not partial or "note" in request.data:
        note = str(request.data.get("note") or "").strip()
        if len(note) > 500:
            return None, "Memory note must be 500 characters or fewer."
        values["note"] = note
    media = request.FILES.get("media")
    if media is not None:
        content_type = str(getattr(media, "content_type", "") or "").lower()
        if not (content_type.startswith("image/") or content_type.startswith("video/")):
            return None, "Memory drafts support photos and videos only."
        if media.size > MAX_DRAFT_MEDIA_BYTES:
            return None, "Memory media must be 60 MB or smaller."
        values["media"] = media
        values["media_type"] = "image" if content_type.startswith("image/") else "video"
    return values, None


class MemoryDraftListView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        drafts = request.user.memory_drafts.all()[:50]
        return Response({"drafts": [serialize_draft(request, row) for row in drafts]})

    def post(self, request):
        values, error = clean_values(request)
        if error:
            return Response({"detail": error}, status=status.HTTP_400_BAD_REQUEST)
        draft = MemoryDraft.objects.create(user=request.user, **values)
        return Response(serialize_draft(request, draft), status=status.HTTP_201_CREATED)


class MemoryDraftDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def _draft(self, request, draft_id):
        return get_object_or_404(MemoryDraft, pk=draft_id, user=request.user)

    def patch(self, request, draft_id):
        draft = self._draft(request, draft_id)
        values, error = clean_values(request, partial=True)
        if error:
            return Response({"detail": error}, status=status.HTTP_400_BAD_REQUEST)
        for key, value in values.items():
            setattr(draft, key, value)
        draft.save()
        return Response(serialize_draft(request, draft))

    def delete(self, request, draft_id):
        self._draft(request, draft_id).delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
