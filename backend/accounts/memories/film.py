from datetime import datetime, timedelta


FILM_MAX_SCENES = 12
FILM_TARGET_DURATION_MS = 45_000
IMAGE_DURATION_MS = 15_000
VIDEO_DURATION_MS = 5_000


def _parse_time(raw):
    try:
        return datetime.fromisoformat(str(raw).replace("Z", "+00:00"))
    except (TypeError, ValueError):
        return None


def _local_day(row, utc_offset_minutes):
    occurred_at = _parse_time(row.get("occurred_at"))
    if occurred_at is None:
        return "unknown"
    return (occurred_at + timedelta(minutes=utc_offset_minutes)).date().isoformat()


def _scene_duration(row):
    return VIDEO_DURATION_MS if row.get("media_type") == "video" else IMAGE_DURATION_MS


def _candidate_score(row):
    score = 6 if row.get("media_type") == "video" else 5
    if row.get("room"):
        score += 2
    if row.get("person"):
        score += 1
    if str(row.get("text") or "").strip():
        score += 1
    if row.get("source") == "pulse":
        score += 1
    return score


def _caption(row):
    text = str(row.get("text") or "").strip()
    title = str(row.get("title") or "").strip()
    room = row.get("room") or {}
    person = row.get("person") or {}
    value = text or title or str(room.get("title") or "").strip()
    if not value:
        value = str(person.get("name") or person.get("username") or "").strip()
    return value[:96]


def _mood(stats):
    if stats.get("nights", 0) >= 3:
        return "after_dark"
    if stats.get("people", 0) >= 3 or stats.get("rooms", 0) >= 2:
        return "together"
    if stats.get("highlights", 0) <= 3:
        return "quiet"
    return "week_in_motion"


def build_memory_film_plan(weekly_memory):
    offset = int(weekly_memory.get("utc_offset_minutes", 0))
    highlights = weekly_memory.get("highlights") or []
    candidates = [
        row
        for row in highlights
        if row.get("media_type") in {"image", "video"}
        and str(row.get("media_url") or "").strip()
    ]

    scored = sorted(
        candidates,
        key=lambda row: (
            -_candidate_score(row),
            str(row.get("occurred_at") or ""),
            int(row.get("id") or 0),
        ),
    )

    selected = []
    selected_keys = set()
    seen_days = set()
    total_duration = 0

    # First pass: give each active local day a chance to appear in the film.
    for row in scored:
        day = _local_day(row, offset)
        if day in seen_days:
            continue
        duration = _scene_duration(row)
        if total_duration + duration > FILM_TARGET_DURATION_MS:
            continue
        key = (row.get("source"), row.get("id"))
        selected.append(row)
        selected_keys.add(key)
        seen_days.add(day)
        total_duration += duration
        if len(selected) >= FILM_MAX_SCENES:
            break

    # Second pass: fill the remaining runtime with the strongest unused moments.
    if len(selected) < FILM_MAX_SCENES and total_duration < FILM_TARGET_DURATION_MS:
        for row in scored:
            key = (row.get("source"), row.get("id"))
            if key in selected_keys:
                continue
            duration = _scene_duration(row)
            if total_duration + duration > FILM_TARGET_DURATION_MS:
                continue
            selected.append(row)
            selected_keys.add(key)
            total_duration += duration
            if len(selected) >= FILM_MAX_SCENES:
                break

    selected.sort(
        key=lambda row: (
            str(row.get("occurred_at") or ""),
            int(row.get("id") or 0),
        )
    )

    scenes = []
    for index, row in enumerate(selected):
        scenes.append(
            {
                "index": index,
                "source": row.get("source"),
                "source_id": row.get("id"),
                "media_type": row.get("media_type"),
                "media_url": row.get("media_url"),
                "occurred_at": row.get("occurred_at"),
                "duration_ms": _scene_duration(row),
                "trim_start_ms": 0,
                "caption": _caption(row),
                "person": row.get("person"),
                "room": row.get("room"),
            }
        )

    stats = weekly_memory.get("stats") or {}
    return {
        "render_version": 1,
        "selection_version": "smart-v1",
        "starts_at": weekly_memory.get("starts_at"),
        "ends_at": weekly_memory.get("ends_at"),
        "utc_offset_minutes": offset,
        "weeks_ago": weekly_memory.get("weeks_ago", 0),
        "film_ready": bool(scenes),
        "mood": _mood(stats),
        "target_duration_ms": FILM_TARGET_DURATION_MS,
        "total_duration_ms": sum(scene["duration_ms"] for scene in scenes),
        "cover_media_url": scenes[0]["media_url"] if scenes else "",
        "scenes": scenes,
    }
