from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import skipUnless
from unittest.mock import patch

from django.test import SimpleTestCase
from PIL import Image, ImageDraw

from .living_neural_outline import load_neural_engine, propose_neural_outline, neural_outline_available
from .living_segmentation import NoOutline, VisionUnavailable, vision_available


class NeuralModelIdentityTests(SimpleTestCase):
    def test_missing_and_changed_artifacts_are_not_inference_engines(self):
        self.assertFalse(neural_outline_available(""))
        with TemporaryDirectory() as root:
            model = Path(root)/"bad.tflite"
            model.write_bytes(b"not the reviewed model")
            self.assertFalse(neural_outline_available(str(model)))
            with self.assertRaises(VisionUnavailable):
                load_neural_engine(str(model))


@skipUnless(vision_available(), "Install optional vision requirements")
class NeuralOutlineGeometryTests(SimpleTestCase):
    def test_prompted_mask_passes_through_existing_review_geometry(self):
        import numpy as np
        from .living_regions import polygon
        image = Image.new("RGB", (192, 144), "#d6c6b5")
        mask = Image.new("L", image.size)
        ImageDraw.Draw(mask).ellipse((50,29,138,111), fill=1)
        with patch("plants.living_neural_outline.load_neural_engine", return_value=object()), \
                patch("plants.living_neural_outline.neural_mask", return_value=np.asarray(mask)) as infer:
            points = propose_neural_outline(image, [(96,72)], [(12,12)], "fake-model")
            polygon(points, *image.size)
            self.assertEqual(infer.call_args.args[1], (96,72))
            rendered = Image.new("L", image.size)
            ImageDraw.Draw(rendered).polygon(list(map(tuple,points)),fill=1)
            a,b = np.asarray(mask)>0,np.asarray(rendered)>0
            self.assertGreater((a&b).sum()/(a|b).sum(), .95)
            with self.assertRaises(NoOutline):
                propose_neural_outline(image, [(96,72)], [(80,72)], "fake-model")
            with self.assertRaises(NoOutline):
                propose_neural_outline(image, [(96,72),(8,8)], [(12,12)], "fake-model")
