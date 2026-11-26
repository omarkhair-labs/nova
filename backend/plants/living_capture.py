"""Optional, bounded inspection of a new private history photo in the current process."""
from django.conf import settings

from .vision_evidence import MAX_BYTES, POLICY_VERSION, inspect_photo


def capture_quality(photo):
    if not photo or not settings.PLANTUPIA_LIVING_SYSTEMS_ENABLED:
        return None
    # Never reject an otherwise valid event because this optional inspection abstains.
    if photo.size > MAX_BYTES:
        return {"policy_version": POLICY_VERSION, "status": "unsupported_capture"}
    try:
        photo.seek(0)
        return inspect_photo(photo.read(MAX_BYTES + 1))
    except (OSError, ValueError):
        return {"policy_version": POLICY_VERSION, "status": "unsupported_capture"}
    finally:
        photo.seek(0)
