from datetime import timedelta


MIN_UTC_OFFSET_MINUTES = -12 * 60
MAX_UTC_OFFSET_MINUTES = 14 * 60
MAX_WEEKS_AGO = 51


def parse_utc_offset(raw):
    try:
        value = int(str(raw).strip())
    except (TypeError, ValueError):
        return None
    if value < MIN_UTC_OFFSET_MINUTES or value > MAX_UTC_OFFSET_MINUTES:
        return None
    return value


def parse_weeks_ago(raw):
    try:
        value = int(str(raw).strip())
    except (TypeError, ValueError):
        return None
    if value < 0 or value > MAX_WEEKS_AGO:
        return None
    return value


def completed_week_window(now, utc_offset_minutes, weeks_ago=0):
    offset = timedelta(minutes=utc_offset_minutes)
    local_now = now + offset
    this_week_start_local = (local_now - timedelta(days=local_now.weekday())).replace(
        hour=0,
        minute=0,
        second=0,
        microsecond=0,
    )
    end_local = this_week_start_local - timedelta(weeks=weeks_ago)
    start_local = end_local - timedelta(days=7)
    return {
        "starts_at": start_local - offset,
        "ends_at": end_local - offset,
        "local_start": start_local,
        "local_end": end_local,
    }


def local_night_key(timestamp, utc_offset_minutes):
    offset = timedelta(minutes=utc_offset_minutes)
    local = timestamp + offset
    if local.hour < 6:
        local -= timedelta(days=1)
    if local.hour >= 18:
        return local.date()
    return None
