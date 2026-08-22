from datetime import timedelta


MIN_UTC_OFFSET_MINUTES = -12 * 60
MAX_UTC_OFFSET_MINUTES = 14 * 60
TONIGHT_START_HOUR = 18
TONIGHT_END_HOUR = 6


def parse_utc_offset(raw):
    try:
        value = int(str(raw if raw is not None else "0").strip())
    except (TypeError, ValueError):
        return None
    if value < MIN_UTC_OFFSET_MINUTES or value > MAX_UTC_OFFSET_MINUTES:
        return None
    return value


def tonight_window(now, utc_offset_minutes):
    offset = timedelta(minutes=utc_offset_minutes)
    local_now = now + offset

    if local_now.hour >= TONIGHT_START_HOUR:
        start_local = local_now.replace(
            hour=TONIGHT_START_HOUR,
            minute=0,
            second=0,
            microsecond=0,
        )
        is_tonight = True
    elif local_now.hour < TONIGHT_END_HOUR:
        start_local = (local_now - timedelta(days=1)).replace(
            hour=TONIGHT_START_HOUR,
            minute=0,
            second=0,
            microsecond=0,
        )
        is_tonight = True
    else:
        start_local = local_now.replace(
            hour=TONIGHT_START_HOUR,
            minute=0,
            second=0,
            microsecond=0,
        )
        is_tonight = False

    end_local = (start_local + timedelta(days=1)).replace(
        hour=TONIGHT_END_HOUR,
        minute=0,
        second=0,
        microsecond=0,
    )
    return {
        "is_tonight": is_tonight,
        "local_hour": local_now.hour,
        "starts_at": start_local - offset,
        "ends_at": end_local - offset,
    }
