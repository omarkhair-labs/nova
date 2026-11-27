"""Rectified binocular stereo anchored to the private room's registered cameras."""
from base64 import b64encode

import cv2
import numpy as np


def calibration(image, size):
    c = image.camera
    if c.model_name not in {"SIMPLE_PINHOLE", "PINHOLE", "SIMPLE_RADIAL", "RADIAL", "OPENCV"}:
        return None
    k = c.calibration_matrix().copy()
    k[0] *= size[0] / c.width
    k[1] *= size[1] / c.height
    distortion = np.zeros(5)
    extra = c.params[c.extra_params_idxs()]
    distortion[:len(extra)] = extra
    return k, distortion


def disparity(a, b, negative=False):
    n = 96
    estimator = cv2.StereoSGBM_create(minDisparity=-n if negative else 0, numDisparities=n,
        blockSize=5, P1=200, P2=800, uniquenessRatio=12, speckleWindowSize=70, speckleRange=2,
        disp12MaxDiff=1, mode=cv2.STEREO_SGBM_MODE_SGBM_3WAY)
    return estimator.compute(a, b).astype(np.float32) / 16


def pair_surface(model, folder, left, right, common):
    camera = left.camera
    if camera.width != right.camera.width or camera.height != right.camera.height:
        return None
    factor = min(1., 640 / max(camera.width, camera.height))
    size = round(camera.width * factor), round(camera.height * factor)
    lhs, rhs = calibration(left, size), calibration(right, size)
    if lhs is None or rhs is None or min(size) < 160:
        return None
    pose = left.cam_from_world()
    rel = right.cam_from_world() * pose.inverse()
    r1, r2, p1, p2, q, _, _ = cv2.stereoRectify(*lhs, *rhs, size, rel.rotation.matrix(),
        rel.translation, flags=cv2.CALIB_ZERO_DISPARITY, alpha=0)
    if p2[0, 3] >= 0 or abs(p2[1, 3]) > abs(p2[0, 3]):
        return None
    rectified = []
    for image, (k, dist), r, projection in ((left, lhs, r1, p1), (right, rhs, r2, p2)):
        original = cv2.imread(str(folder / image.name))
        if original is None:
            return None
        maps = cv2.initUndistortRectifyMap(k, dist, r, projection, size, cv2.CV_32FC1)
        rectified.append(cv2.remap(cv2.resize(original, size, interpolation=cv2.INTER_AREA), *maps, cv2.INTER_LINEAR))
    a, b = [cv2.cvtColor(im, cv2.COLOR_BGR2GRAY) for im in rectified]
    dl, dr = disparity(a, b), disparity(b, a, True)
    yy, xx = np.indices(dl.shape)
    matched = np.round(xx - dl).astype(int)
    inside = (matched >= 0) & (matched < size[0])
    back = dr[yy, matched.clip(0, size[0]-1)]
    valid = (dl > 2) & (dl < 94) & inside & (back < 0) & (abs(dl+back) < 1.25)
    cloud = cv2.reprojectImageTo3D(dl, q)
    valid &= np.isfinite(cloud).all(axis=2) & (cloud[:,:,2] > 0)
    # A pair also has to agree with independent three-view landmark depths.
    landmarks = np.array([model.points3D[i].xyz for i in common if model.points3D[i].track.length() >= 3])
    if len(landmarks) < 25:
        return None
    ref = (pose * landmarks) @ r1.T
    uvz = ref @ p1[:3,:3].T
    uv = np.round(uvz[:,:2] / uvz[:,2:3]).astype(int)
    errors = [abs(cloud[y,x,2]/z-1) for (x,y), z in zip(uv, ref[:,2])
              if z > 0 and 0 <= x < size[0] and 0 <= y < size[1] and valid[y,x]]
    if len(errors) < 20 or np.median(errors) > .06 or np.percentile(errors, 80) > .15:
        return None
    step = max(6, int(np.sqrt(valid.sum() / 875)))
    faces, stencil = [], np.zeros_like(a)
    for y in range(0, size[1]-step, step):
        for x in range(0, size[0]-step, step):
            block = valid[y:y+step+1, x:x+step+1]
            if block.mean() < .88:
                continue
            depths = cloud[y:y+step+1, x:x+step+1, 2][block]
            if np.percentile(depths, 95) > np.percentile(depths, 5)*1.06:
                continue
            for corners in ([(x,y),(x+step,y),(x+step,y+step)], [(x,y),(x+step,y+step),(x,y+step)]):
                if not all(valid[v,u] for u,v in corners):
                    continue
                coords = np.array([cloud[v,u] for u,v in corners])
                if max(coords[:,2])/min(coords[:,2]) > 1.06:
                    continue
                uv = np.array(corners) * np.array([1024/size[0], 1024/size[1]])
                faces.append(np.column_stack((coords, uv)))
                cv2.fillConvexPoly(stencil, np.int32(corners), 255)
    if len(faces) < 80:
        return None
    vertices = np.array(faces)
    # Uniform thinning across the view, preserving acquisition order and coverage.
    if len(vertices) > 2000:
        vertices = vertices[np.linspace(0, len(vertices)-1, 2000, dtype=int)]
    xyz = vertices[:,:,:3]
    xyz -= np.median(xyz.reshape(-1, 3), axis=0)
    radii = np.linalg.norm(xyz, axis=2)
    radius = np.percentile(radii, 98)
    if not np.isfinite(radius) or radius < 1e-8:
        return None
    xyz /= radius
    kept = vertices[(radii <= 2.5 * radius).all(axis=1)]
    if len(kept) < 80:
        return None
    masked = cv2.bitwise_and(rectified[0], rectified[0], mask=stencil)
    ok, texture = cv2.imencode(".jpg", cv2.resize(masked, (1024, 1024)), [cv2.IMWRITE_JPEG_QUALITY, 84])
    if not ok:
        return None
    return {"method": "validated_stereo", "triangles": kept.round(5).reshape(-1, 15).tolist(),
            "landmark_checks": len(errors), "median_landmark_depth_disagreement": float(np.median(errors)),
            "texture": b64encode(texture).decode("ascii")}


def stereo_surface(model, folder):
    images = list(model.images.values())
    tracks = {i.image_id: {p.point3D_id for p in i.points2D if p.has_point3D()} for i in images}
    pairs = []
    for left in images:
        for right in images:
            if left.image_id == right.image_id:
                continue
            common = tracks[left.image_id] & tracks[right.image_id]
            rel = right.cam_from_world() * left.cam_from_world().inverse()
            x, y, z = rel.translation
            if len(common) >= 50 and -x > max(abs(y), abs(z)) and rel.rotation.matrix()[2,2] > .92:
                pairs.append((len(common), left, right, common))
    for _, left, right, common in sorted(pairs, key=lambda pair: -pair[0])[:4]:
        try:
            surface = pair_surface(model, folder, left, right, common)
        except cv2.error:
            continue
        if surface:
            return surface
    return None
