"""Calendar dates belong to the viewing device's civil timezone."""
from datetime import timedelta, timezone
import re
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from rest_framework.exceptions import ValidationError


def calendar_zone(value):
    if value is None:
        return timezone.utc
    if not isinstance(value, str) or not 0 < len(value) <= 100:
        raise ValidationError("Invalid calendar timezone.")
    if value == "Z":
        return timezone.utc
    offset = re.fullmatch(r"(?:GMT|UTC)?([+-])(\d{2}):(\d{2})", value)
    if offset:
        hours, minutes = int(offset[2]), int(offset[3])
        if hours <= 18 and minutes < 60 and (hours < 18 or minutes == 0):
            return timezone((1 if offset[1] == "+" else -1) * timedelta(hours=hours, minutes=minutes))
        raise ValidationError("Invalid calendar offset.")
    try:
        return ZoneInfo(value)
    except (ValueError, ZoneInfoNotFoundError):
        raise ValidationError("Unknown calendar timezone.")
