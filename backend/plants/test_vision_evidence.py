from io import BytesIO
from unittest import TestCase

from PIL import Image, ImageDraw

from .vision_evidence import inspect_photo, MAX_BYTES


def photo(color, size=(640, 480), fmt="PNG"):
    output = BytesIO()
    Image.new("RGB", size, color).save(output, format=fmt)
    return output.getvalue()


class VisionEvidenceTests(TestCase):
    def test_dark_photo(self):
        report = inspect_photo(photo((0, 0, 0)))
        self.assertIn("predominantly_dark", report["capture_flags"])
        self.assertEqual(report["status"], "review_capture")
        self.assertEqual(report["metrics"]["dark_pixel_fraction"], 1.0)

    def test_bright_photo(self):
        self.assertIn("predominantly_bright", inspect_photo(photo((255, 255, 255)))["capture_flags"])

    def test_small_photo(self):
        self.assertIn("low_resolution", inspect_photo(photo("green", (100, 100)))["capture_flags"])

    def test_invalid_or_oversized(self):
        for data in (b"", b"not-an-image", b"x" * (MAX_BYTES + 1)):
            with self.subTest(size=len(data)), self.assertRaises(ValueError):
                inspect_photo(data)

    def test_unsupported_format(self):
        with self.assertRaises(ValueError):
            inspect_photo(photo("green", fmt="GIF"))

    def test_deterministic_and_no_diagnosis_even_for_green_image(self):
        data = photo("green")
        report = inspect_photo(data)
        self.assertEqual(report, inspect_photo(data))
        self.assertEqual(report["plant_presence"], "not_assessed")
        self.assertEqual(report["health"], "not_assessed")
        self.assertEqual(report["soil_moisture"], "not_measured")
        self.assertEqual(report["environmental_illuminance"], "not_measured")

    def test_textured_synthetic_photo_can_pass_quality_without_plant_claim(self):
        image = Image.new("RGB", (640, 480), (60, 100, 50))
        draw = ImageDraw.Draw(image)
        for x in range(0, 640, 12):
            draw.rectangle((x, 0, x + 5, 479), fill=(180, 170, 90))
        output = BytesIO()
        image.save(output, format="PNG")
        report = inspect_photo(output.getvalue())
        self.assertEqual(report["status"], "no_quality_flags")
        self.assertEqual(report["plant_presence"], "not_assessed")
