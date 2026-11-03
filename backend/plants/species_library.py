"""Read-only published botanical library. No customer choice is a verified identity."""
from django.db.models import Q
from django.http import Http404
from django.shortcuts import get_object_or_404
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import BotanicalSpecies, CareGuideVersion


def visible_species():
    return (BotanicalSpecies.objects.filter(guides__published=True)
            .distinct().order_by("scientific_name", "key"))


def species_summary(species):
    return {
        "key": species.key,
        "scientific_name": species.scientific_name,
        "aliases": [
            {"name": alias.name, "language": alias.language}
            for alias in species.aliases.all()
        ],
        "has_published_guide": True,
        "identity_basis": "reference_only",
    }


class BotanicalSpeciesLibraryView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        query = request.query_params.get("q", "").strip()
        if len(query) > 80:
            return Response({"q": ["Maximum search length is 80 characters."]}, status=400)
        try:
            offset = int(request.query_params.get("offset", "0"))
        except (TypeError, ValueError):
            return Response({"offset": ["Use a non-negative integer offset."]}, status=400)
        if offset < 0 or offset > 10000:
            return Response({"offset": ["Offset out of range."]}, status=400)
        qs = visible_species()
        if query:
            qs = qs.filter(
                Q(scientific_name__icontains=query)
                | Q(key__icontains=query)
                | Q(aliases__name__icontains=query)
            ).distinct()
        page_size = 25
        rows = list(qs.prefetch_related("aliases")[offset:offset + page_size + 1])
        next_offset = offset + page_size if len(rows) > page_size else None
        return Response({
            "results": [species_summary(item) for item in rows[:page_size]],
            "next_offset": next_offset,
            "notice": "Curated species names are reference only, not physical-plant verification.",
        }, headers={"Cache-Control": "private, no-store"})


class BotanicalSpeciesLibraryDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, key):
        item = get_object_or_404(visible_species().prefetch_related("aliases"), key=key)
        guide = (CareGuideVersion.objects.filter(species=item, published=True)
                 .select_related("reviewer").order_by("-version").first())
        if guide is None:
            raise Http404
        return Response({
            **species_summary(item),
            "guide": {
                "version": guide.version,
                "instructions": guide.instructions,
                "context_notes": guide.context_notes,
                "source_reference": guide.source_reference,
                "source_url": guide.source_url,
                "reviewed_at": guide.reviewed_at.isoformat(),
            },
            "notice": "General, source-reviewed information based on the selected type; an individual plant is not verified.",
        }, headers={"Cache-Control": "private, no-store"})
