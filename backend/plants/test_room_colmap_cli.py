"""Test the CLI adapter without installing or invoking COLMAP."""
from importlib.util import find_spec
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, skipUnless


@skipUnless(find_spec("numpy") is not None, "Optional geometry runtime")
class ColmapTextModelTests(TestCase):
    def test_poses_and_camera_calibration(self):
        import numpy as np
        from .room_colmap_cli import camera_pose, Camera
        pose = camera_pose([.7071067811865476, 0, 0, .7071067811865476], [2, 3, 4])
        points = np.array([[1, 0, 0], [0, 1, 0]], dtype=float)
        np.testing.assert_allclose(pose * points, [[2, 4, 4], [1, 3, 4]], atol=1e-7)
        np.testing.assert_allclose(pose.inverse() * (pose * points), points, atol=1e-7)
        np.testing.assert_allclose((pose.inverse() * pose) * points, points, atol=1e-7)
        camera = Camera("SIMPLE_RADIAL", 640, 480, np.array([300, 320, 240, .1]))
        np.testing.assert_allclose(camera.calibration_matrix(), [[300,0,320],[0,300,240],[0,0,1]])
        self.assertEqual(camera.extra_params_idxs(), [3])

    def test_real_text_contract_and_cross_checked_tracks(self):
        from .room_colmap_cli import read_model
        with TemporaryDirectory() as tmp:
            folder = Path(tmp)
            (folder / "cameras.txt").write_text("1 SIMPLE_PINHOLE 640 480 300 320 240\n")
            (folder / "images.txt").write_text(
                "1 1 0 0 0 0 0 0 1 a.jpg\n320 240 5 400 350 -1\n"
                "2 1 0 0 0 1 0 0 1 b.jpg\n420 240 5\n"
                "3 1 0 0 0 2 0 0 1 c.jpg\n520 240 5\n"
                "4 1 0 0 0 3 0 0 1 d.jpg\n\n")
            point_file = folder / "points3D.txt"
            point_file.write_text("5 0 0 3 20 40 60 .5 1 0 2 0 3 0\n")
            model = read_model(folder, {"a.jpg", "b.jpg", "c.jpg", "d.jpg"})
            self.assertEqual(model.num_reg_images(), 4)
            self.assertEqual(model.num_points3D(), 1)
            self.assertEqual(model.images[1].num_points3D, 1)
            self.assertEqual(model.images[4].num_points3D, 0)
            self.assertEqual(model.points3D[5].track.length(), 3)
            self.assertEqual(model.compute_mean_reprojection_error(), .5)
            point_file.write_text("5 0 0 3 20 40 60 .5 1 1 2 0 3 0\n")
            with self.assertRaises(ValueError):
                read_model(folder, {"a.jpg", "b.jpg", "c.jpg", "d.jpg"})
