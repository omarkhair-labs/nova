"""Approximate unrefracted solar position and one-window placement scenarios."""
from calendar import isleap
from datetime import timezone
from math import asin, atan2, cos, degrees, isfinite, pi, radians, sin

from .living_measurements import aperture_footprint, direct_sun


def solar_position(stamp, latitude, longitude):
    """NOAA fractional-year approximation, east-positive longitude, true-north azimuth."""
    if stamp.utcoffset() is None or not -90 <= latitude <= 90 or not -180 <= longitude <= 180:
        raise ValueError("Supply a time with UTC offset and geographic coordinates.")
    utc = stamp.astimezone(timezone.utc)
    hour = utc.hour + utc.minute/60 + utc.second/3600
    year = 366 if isleap(utc.year) else 365
    angle = 2*pi/year * (utc.timetuple().tm_yday-1+(hour-12)/24)
    eq = 229.18*(.000075+.001868*cos(angle)-.032077*sin(angle)-.014615*cos(2*angle)-.040849*sin(2*angle))
    dec = (.006918-.399912*cos(angle)+.070257*sin(angle)-.006758*cos(2*angle)
           +.000907*sin(2*angle)-.002697*cos(3*angle)+.00148*sin(3*angle))
    h = radians(((hour*60+eq+4*longitude) % 1440)/4-180)
    lat = radians(latitude)
    up = sin(lat)*sin(dec)+cos(lat)*cos(dec)*cos(h)
    east = -cos(dec)*sin(h)
    north = cos(lat)*sin(dec)-sin(lat)*cos(dec)*cos(h)
    return degrees(asin(max(-1, min(1, up)))), degrees(atan2(east, north)) % 360


def window_scenario(room, window_id, point, *, top_normal_deg, width_m, sill_m, head_m,
                    latitude, longitude, timestamps):
    w, l = room["width_m"], room["length_m"]
    opening = next((row for row in room["windows"] if row["id"] == window_id), None)
    if not opening:
        raise ValueError("Choose a window from this room revision.")
    wx, wy = opening["x_m"], opening["y_m"]
    x, y, z = point
    if (wx in (0,w) and wy in (0,l)) or not (0 < x < w and 0 < y < l and z >= 0):
        raise ValueError("Choose an unambiguous wall opening and an interior sample point.")
    if wy == 0:
        along, inward, side, middle, length = x, y, 0, wx, w
    elif wx == w:
        along, inward, side, middle, length = y, w-x, 90, wy, l
    elif wy == l:
        along, inward, side, middle, length = w-x, l-y, 180, w-wx, w
    else:
        along, inward, side, middle, length = l-y, x, 270, l-wy, l
    left, right = middle-width_m/2, middle+width_m/2
    if width_m <= 0 or not 0 <= left < right <= length:
        raise ValueError("The opening must fit on the selected wall.")
    if not isfinite(top_normal_deg):
        raise ValueError("Supply a finite orientation.")
    depth = l if side in (0, 180) else w

    def to_room(along_wall, inward):
        if side == 0:
            return [along_wall, inward]
        if side == 90:
            return [w-inward, along_wall]
        if side == 180:
            return [w-along_wall, l-inward]
        return [inward, l-along_wall]

    rows = []
    for stamp in timestamps:
        elevation, azimuth = solar_position(stamp, latitude, longitude)
        aperture = dict(height_m=z, window_left_m=left, window_right_m=right,
                        sill_m=sill_m, head_m=head_m, sun_elevation_deg=elevation, sun_azimuth_deg=azimuth,
                        outward_normal_deg=top_normal_deg+side)
        ray = direct_sun(inward_m=inward, along_m=along, **aperture)
        footprint = aperture_footprint(wall_span=length, room_depth=depth, **aperture)
        rows.append({"at": stamp.isoformat(), "sun_elevation_deg": elevation, "sun_azimuth_deg": azimuth,
                     "direct_ray": ray["unobstructed_direct_ray"],
                     "footprint_xy_m": [to_room(a, i) for a, i in footprint]})
    return {"source": "simulation", "geometry_source": room["geometry_source"], "samples": rows, "plane_height_m": z,
            "assumptions": ["one vertical opening", "no glazing attenuation", "unobstructed exterior and interior"],
            "illuminance_lux": None, "artificial_lighting": "unmodelled", "placement_suitability": "unassessed"}
