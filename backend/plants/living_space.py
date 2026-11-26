"""Research-only metric room model. Coordinates supplied by an operator, not reconstructed."""
from math import hypot, isfinite


def _number(value):
    if type(value) not in (int, float) or not isfinite(value):
        raise ValueError("Coordinates must be finite numbers.")
    return float(value)


def _identity(value):
    if not isinstance(value, str) or not value.strip() or len(value) > 128:
        raise ValueError("Invalid identity.")
    return value


def build_room(room_id, width_m, length_m, plants, windows):
    """A rectangular local coordinate system in meters, with explicitly reported positions."""
    room_id = _identity(room_id)
    width, length = _number(width_m), _number(length_m)
    if not 0 < width <= 1000 or not 0 < length <= 1000:
        raise ValueError("Invalid room dimensions.")
    if len(plants) > 1000 or len(windows) > 100:
        raise ValueError("Room entity limit exceeded.")

    def position(item):
        x, y = _number(item["x_m"]), _number(item["y_m"])
        if not 0 <= x <= width or not 0 <= y <= length:
            raise ValueError("Position outside room.")
        return x, y

    seen = set()
    openings = []
    for item in windows:
        key = _identity(item["id"])
        if key in seen:
            raise ValueError("Duplicate entity identity.")
        seen.add(key)
        x, y = position(item)
        if x not in (0, width) and y not in (0, length):
            raise ValueError("Window must lie on boundary.")
        openings.append({"id": key, "x_m": x, "y_m": y})
    units = []
    for item in plants:
        key = _identity(item["plant_id"])
        if key in seen:
            raise ValueError("Duplicate entity identity.")
        seen.add(key)
        x, y = position(item)
        distances = [hypot(x-w["x_m"], y-w["y_m"]) for w in openings]
        units.append({
            "plant_id": key, "x_m": x, "y_m": y,
            "nearest_window_distance_m": min(distances) if distances else None,
            "illuminance": "not_measured",
            "position_source": "operator_reported",
        })
    return {
        "version": "room-rectangle-01", "room_id": room_id,
        "width_m": width, "length_m": length, "area_m2": width * length,
        "geometry_source": "operator_reported",
        "plants": units, "windows": openings,
        "geometry_validation": "not_independently_verified",
    }


def living_snapshot(room, observations):
    """Join existing plant IDs to reported visual evidence; no invented health state."""
    known = {x["plant_id"] for x in room["plants"]}
    by_plant = {}
    for observation in observations:
        key = _identity(observation["plant_id"])
        if key not in known or key in by_plant:
            raise ValueError("Unknown or duplicate plant observation.")
        evidence_id = _identity(observation["evidence_id"])
        observed_at = observation["observed_at"]
        from datetime import datetime
        if not isinstance(observed_at, str):
            raise ValueError("Expected timestamp.")
        try:
            stamp = datetime.fromisoformat(observed_at.replace("Z", "+00:00"))
        except ValueError as exc:
            raise ValueError("Invalid timestamp.") from exc
        if stamp.tzinfo is None:
            raise ValueError("Timestamp must include offset.")
        by_plant[key] = {"evidence_id": evidence_id, "observed_at": stamp.isoformat(), "status": "reported_unreviewed"}
    return {
        "room_id": room["room_id"], "scope": "operator_reported_spatial_snapshot",
        "plants": [
            {**plant, "latest_supplied_observation": by_plant.get(plant["plant_id"]), "health": "not_assessed"}
            for plant in room["plants"]
        ],
        "live_sync": False,
    }
