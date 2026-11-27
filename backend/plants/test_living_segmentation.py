from io import BytesIO
from tempfile import TemporaryDirectory
from unittest import skipUnless
from uuid import uuid4

from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase, SimpleTestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from PIL import Image, ImageDraw
from rest_framework.test import APIClient

from .living_regions import polygon
from .living_segmentation import METHOD, NoOutline, propose_outline, vision_available
from .models import PlantEvent, PlantUnit, Space, SpaceMember, TracedRegion


def sample_image():
    image = Image.new("RGB", (192, 144), (210, 190, 165))
    ImageDraw.Draw(image).ellipse((50, 29, 138, 111), fill=(45, 130, 65))
    return image


@skipUnless(vision_available(), "Install the optional isolated vision requirements")
class SegmentationGeometryTests(SimpleTestCase):
    def test_seeded_outline_is_bounded_and_follows_actual_pixels(self):
        image = sample_image()
        points = propose_outline(image, [(96, 72)], [(12, 12)])
        polygon(points, *image.size)
        actual = Image.new("1", image.size)
        expected = Image.new("1", image.size)
        ImageDraw.Draw(actual).polygon(list(map(tuple, points)), fill=1)
        ImageDraw.Draw(expected).ellipse((50, 29, 138, 111), fill=1)
        pairs = list(zip(actual.getdata(), expected.getdata()))
        iou = sum(a and b for a, b in pairs) / sum(a or b for a, b in pairs)
        self.assertGreater(iou, .95)
        self.assertEqual(actual.getpixel((96, 72)), 1)
        self.assertEqual(actual.getpixel((12, 12)), 0)

    def test_ambiguous_or_invalid_points_do_not_manufacture_a_boundary(self):
        with self.assertRaises(NoOutline):
            propose_outline(Image.new("RGB", (192, 144), "green"), [(96, 72)], [(12, 12)])
        with self.assertRaises(NoOutline):
            propose_outline(sample_image(), [(96, 72)], [(96, 73)])
        with self.assertRaises(ValueError):
            propose_outline(sample_image(), [(193, 72)], [(12, 12)])


@override_settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=True)
@skipUnless(vision_available(), "Install the optional isolated vision requirements")
class RegionProposalTests(TestCase):
    def setUp(self):
        self.media = TemporaryDirectory()
        self.addCleanup(self.media.cleanup)
        self.settings_override = self.settings(MEDIA_ROOT=self.media.name)
        self.settings_override.enable()
        self.addCleanup(self.settings_override.disable)
        user = get_user_model()
        self.owner = user.objects.create_user(email="outline-owner@example.test", username="outline_owner")
        self.other = user.objects.create_user(email="outline-other@example.test", username="outline_other")
        self.space = Space.objects.create(owner=self.owner, name="Synthetic outline room")
        self.plant = PlantUnit.objects.create(space=self.space, label="Synthetic outline plant")
        output = BytesIO()
        sample_image().save(output, format="PNG")
        self.event = PlantEvent.objects.create(plant=self.plant, actor=self.owner,
            kind="observation", observed_at=timezone.now(), photo=SimpleUploadedFile(
                "outline.png", output.getvalue(), content_type="image/png"))
        args = [self.plant.pk, self.event.pk]
        self.suggest = reverse("plantupia-suggest-region", args=args)
        self.save = reverse("plantupia-trace-region", args=args)
        self.evidence = reverse("plantupia-field-evidence", args=args)
        self.api = APIClient()
        self.api.force_authenticate(self.owner)
        self.prompts = {"foreground": [[96,72]], "background": [[12,12]]}

    def test_private_proposal_only_writes_on_explicit_acceptance_and_replays(self):
        self.assertTrue(self.api.get(self.evidence).data["can_suggest_region"])
        response = self.api.post(self.suggest, self.prompts, format="json")
        self.assertEqual(response.status_code, 200, response.data)
        data = response.data
        self.assertEqual(data["status"], "candidate", data)
        self.assertEqual(data["health"], "not_assessed")
        self.assertEqual(TracedRegion.objects.count(), 0)
        payload = {"submission_id": str(uuid4()), "part": "leaf", "vertices": data["vertices"],
                   "suggestion_token": data["suggestion_token"]}
        tampered = {**payload, "vertices": [[1,1],[10,1],[10,10]]}
        self.assertEqual(self.api.post(self.save, tampered, format="json").status_code, 409)
        self.assertEqual(TracedRegion.objects.count(), 0)
        result = self.api.post(self.save, payload, format="json")
        self.assertEqual(result.status_code, 201, result.data)
        self.assertEqual(result.data["measurements"]["method"], METHOD)
        self.assertEqual(result.data["measurements"]["assistance"]["prompts"], self.prompts)
        trace = TracedRegion.objects.get()
        self.assertNotIn("suggestion_token", trace.annotation)
        self.assertEqual(self.api.post(self.save, payload, format="json").status_code, 200)
        self.assertEqual(TracedRegion.objects.count(), 1)

    def test_automatic_proposal_keeps_its_method_only_after_acceptance(self):
        data = self.api.post(self.suggest, {"mode": "automatic"}, format="json").data
        self.assertEqual(data["status"], "candidate", data)
        self.assertFalse(TracedRegion.objects.exists())
        payload = {"submission_id": str(uuid4()), "part": "leaf", "vertices": data["vertices"],
                   "outline_method": data["method"], "suggestion_token": data["suggestion_token"]}
        result = self.api.post(self.save, payload, format="json")
        self.assertEqual(result.status_code, 201, result.data)
        self.assertEqual(result.data["measurements"]["method"], "reviewed-grabcut-frame-v1")
        self.assertEqual(result.data["measurements"]["assistance"]["prompts"], {})

    def test_scope_flag_and_actor_bound_token(self):
        result = self.api.post(self.suggest, self.prompts, format="json").data
        self.api.force_authenticate(self.other)
        self.assertEqual(self.api.post(self.suggest, self.prompts, format="json").status_code, 404)
        member = SpaceMember.objects.create(space=self.space, user=self.other, role="viewer")
        self.assertFalse(self.api.get(self.evidence).data["can_suggest_region"])
        self.assertEqual(self.api.post(self.suggest, self.prompts, format="json").status_code, 403)
        member.role = "carer"
        member.save(update_fields=["role"])
        self.assertEqual(self.api.post(self.save, {"submission_id": str(uuid4()), "part": "leaf",
            "vertices": result["vertices"], "suggestion_token": result["suggestion_token"]}, format="json").status_code, 409)
        self.assertEqual(TracedRegion.objects.count(), 0)
        with self.settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=False):
            self.assertEqual(self.api.post(self.suggest, self.prompts, format="json").status_code, 404)

    def test_neural_outline_is_opt_in_and_preserves_signed_evidence(self):
        from unittest.mock import patch
        from .living_neural_outline import METHOD as NEURAL_METHOD
        payload = {**self.prompts, "mode": "neural"}
        with self.settings(PLANTUPIA_NEURAL_OUTLINE_MODEL=""):
            self.assertFalse(self.api.get(self.evidence).data["can_neural_suggest_region"])
            self.assertEqual(self.api.post(self.suggest, payload, format="json").status_code, 503)
        with self.settings(PLANTUPIA_NEURAL_OUTLINE_MODEL="isolated-model"), \
                patch("plants.living_api.neural_outline_available", return_value=True), \
                patch("plants.living_api.propose_neural_outline", return_value=[[50,72],[96,29],[138,72],[96,111]]) as engine:
            self.assertTrue(self.api.get(self.evidence).data["can_neural_suggest_region"])
            data = self.api.post(self.suggest, payload, format="json").data
            self.assertEqual(data["status"], "candidate", data)
            self.assertEqual(data["method"], NEURAL_METHOD)
            self.assertEqual(data["health"], "not_assessed")
            self.assertEqual(engine.call_args.kwargs["model_path"], "isolated-model")
            self.assertEqual(TracedRegion.objects.count(), 0)
            saved = {"submission_id": str(uuid4()), "part": "leaf", "vertices": data["vertices"],
                     "suggestion_token": data["suggestion_token"], "outline_method": NEURAL_METHOD}
            self.assertEqual(self.api.post(self.save, {**saved, "outline_method": METHOD}, format="json").status_code, 409)
            result = self.api.post(self.save, saved, format="json")
            self.assertEqual(result.status_code, 201, result.data)
            self.assertEqual(result.data["measurements"]["method"], NEURAL_METHOD)
            self.assertEqual(result.data["measurements"]["assistance"]["prompts"], self.prompts)
            self.api.force_authenticate(self.other)
            self.assertEqual(self.api.post(self.suggest, payload, format="json").status_code, 404)
        self.assertEqual(TracedRegion.objects.count(), 1)
