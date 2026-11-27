"""Texture only image patches supported by triangulated tracks in multiple views."""
from base64 import b64encode
from io import BytesIO

import cv2
import numpy as np
from PIL import Image, ImageDraw


def photo_surface(model, folder):
    tile = 512
    atlas = Image.new("RGB", (tile * 2, tile * 2))
    faces = []
    seen = set()
    cameras = sorted(model.images.values(), key=lambda image: image.num_points3D, reverse=True)
    for index, image in enumerate(cameras[:4]):
        with Image.open(folder / image.name) as original:
            texture = original.convert("RGB").resize((tile, tile), Image.Resampling.LANCZOS)
        width, height = image.camera.width, image.camera.height
        subdiv = cv2.Subdiv2D((0, 0, width, height))
        observations = {}
        tracks = {}
        pose = image.cam_from_world()
        # Retain the best-supported observation in each small image cell.
        cells = set()
        for pixel in sorted((p for p in image.points2D if p.has_point3D()),
                            key=lambda p: model.points3D[p.point3D_id].error):
            x, y = map(float, pixel.xy)
            if not (1 <= x < width - 1 and 1 <= y < height - 1):
                continue
            point = model.points3D[pixel.point3D_id]
            cell = int(x // 12), int(y // 12)
            if cell in cells or point.error > 2 or point.track.length() < 3:
                continue
            cells.add(cell)
            subdiv.insert((x, y))
            observations[(round(x, 2), round(y, 2))] = pixel.point3D_id
            tracks[pixel.point3D_id] = {e.image_id for e in point.track.elements}
        if len(observations) < 3:
            continue
        mask = Image.new("L", (tile, tile))
        draw = ImageDraw.Draw(mask)
        ox, oy = index % 2 * tile, index // 2 * tile
        used = 0
        for row in subdiv.getTriangleList():
            xy = np.array(row, dtype=float).reshape(3, 2)
            ids = [observations.get((round(x, 2), round(y, 2))) for x, y in xy]
            identity = tuple(sorted(i for i in ids if i is not None))
            if len(identity) != 3 or identity in seen or len(set.intersection(*(tracks[i] for i in ids))) < 2:
                continue
            edges = np.linalg.norm(xy - np.roll(xy, 1, axis=0), axis=1)
            if min(edges) < 2 or max(edges) > min(width, height) * .07:
                continue
            xyz = np.array([model.points3D[i].xyz for i in ids])
            depth = (pose * xyz)[:, 2]
            if min(depth) <= 0 or max(depth) / min(depth) > 1.06:
                continue
            uv = xy * [tile / width, tile / height]
            draw.polygon([tuple(p) for p in uv], fill=255)
            faces.append(np.column_stack((xyz, uv + [ox, oy])))
            seen.add(identity)
            used += 1
            if used >= 500:
                break
        atlas.paste(texture, (ox, oy), mask)
    if len(faces) < 10:
        return {}
    vertices = np.array(faces)
    xyz = vertices[:, :, :3]
    # Start the viewer facing the best registered photograph.
    xyz[:] = (cameras[0].cam_from_world() * xyz.reshape(-1, 3)).reshape(xyz.shape)
    center = np.median(xyz.reshape(-1, 3), axis=0)
    xyz -= center
    radius = np.percentile(np.linalg.norm(xyz, axis=2), 98)
    if not np.isfinite(radius) or radius <= 0:
        return {}
    xyz /= radius
    keep = (np.linalg.norm(xyz, axis=2) <= 2.5).all(axis=1)
    if keep.sum() < 10:
        return {}
    encoded = BytesIO()
    atlas.save(encoded, format="JPEG", quality=84)
    return {"triangles": vertices[keep].round(5).reshape(-1, 15).tolist(),
            "texture": b64encode(encoded.getvalue()).decode("ascii")}
