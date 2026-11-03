"""One honest payload for verified and self-selected published botanical guidance."""


def serialize_published_guide(guide):
    """Caller must authorize plant visibility and filter published guide versions."""
    sections = list(guide.sections.all())
    blocks = [
        {
            "topic": section.topic,
            "title": section.title,
            "instructions": section.instructions,
            "context_notes": section.context_notes,
            "source_url": section.source_url or guide.source_url,
            "source_reference": section.source_reference or guide.source_reference,
            "position": section.position,
        }
        for section in sections
    ]
    # Keep the legacy client fully informative even when the newly authored
    # guide has sections only. Never synthesize care facts, just concatenate
    # the exact reviewed author text.
    narrative = guide.instructions or "\n\n".join(
        f"{entry['title']}\n{entry['instructions']}"
        + (f"\n{entry['context_notes']}" if entry["context_notes"] else "")
        for entry in blocks
    )
    return {
        "version": guide.version,
        "species": guide.species.scientific_name,
        "instructions": narrative,
        "legacy_instructions": guide.instructions,
        "context_notes": guide.context_notes,
        "source_url": guide.source_url,
        "source_reference": guide.source_reference,
        "reviewer": guide.reviewer.username,
        "reviewed_at": guide.reviewed_at.isoformat(),
        "sections": blocks,
    }
