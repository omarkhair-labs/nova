from hashlib import sha256
from io import BytesIO
from tempfile import TemporaryDirectory
from uuid import uuid4

from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from PIL import Image, ImageDraw
from rest_framework.test import APIClient

from .models import PlantEvent, PlantUnit, Space, SpaceMember, TracedRegion


@override_settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=True)
class OutlineReviewTests(TestCase):
    def setUp(self):
        media = TemporaryDirectory()
        self.addCleanup(media.cleanup)
        override = self.settings(MEDIA_ROOT=media.name)
        override.enable()
        self.addCleanup(override.disable)
        user = get_user_model()
        self.owner = user.objects.create_user(email="outline-check@example.test",username="outline_check")
        self.other = user.objects.create_user(email="outline-reader@example.test",username="outline_reader")
        space = Space.objects.create(owner=self.owner,name="Synthetic QA room")
        self.plant = PlantUnit.objects.create(space=space,label="Synthetic QA plant")
        self.api = APIClient()
        self.api.force_authenticate(self.owner)

    def create(self, size=(640,480), part="leaf"):
        image = Image.new("RGB",size,(200,200,200))
        draw = ImageDraw.Draw(image)
        # Structured background and green region with an actual visible yellow
        # patch. This is a numerical control, not botanical ground truth.
        for x in range(0,size[0],16):
            draw.line((x,0,x,size[1]-1),fill=(60,60,60),width=4)
        w,h = size
        draw.rectangle((w//8,h//8,7*w//8,7*h//8),fill=(55,145,65))
        draw.rectangle((w//3,h//3,w//2,h//2),fill=(215,194,65))
        out = BytesIO()
        image.save(out,"PNG")
        event = PlantEvent.objects.create(plant=self.plant,actor=self.owner,kind="observation",
            observed_at=timezone.now(),photo=SimpleUploadedFile("control.png",out.getvalue(),content_type="image/png"))
        args=[self.plant.pk,event.pk]
        trace_url=reverse("plantupia-trace-region",args=args)
        result=self.api.post(trace_url,dict(submission_id=str(uuid4()),part=part,
            vertices=[[w//8,h//8],[7*w//8,h//8],[7*w//8,7*h//8],[w//8,7*h//8]]),format="json")
        self.assertEqual(result.status_code,201,result.data)
        return event,TracedRegion.objects.get(pk=result.data["id"]),reverse("plantupia-field-evidence",args=args)

    def test_saved_pixels_drive_inspection_without_recording_a_diagnosis(self):
        event,trace,url=self.create()
        payload={"symptom":"uncertain","trace_id":trace.pk}
        result=self.api.post(url,payload,format="json")
        self.assertEqual(result.status_code,200,result.data)
        self.assertEqual(result.data["input_source"],"saved_outline")
        evidence=result.data["photo_evidence"]
        self.assertEqual(evidence["status"],"visible_patch_candidate",evidence)
        self.assertEqual(evidence["health"],"not_assessed")
        self.assertIn("yellow_like",[p["appearance"] for p in evidence["patches"]])
        self.assertIsNone(result.data["diagnosis"])
        self.assertTrue(all(x["hypothesis"] is None for x in result.data["inspection_options"]))
        self.assertEqual(TracedRegion.objects.count(),1)
        self.assertEqual(PlantEvent.objects.count(),1)
        self.api.force_authenticate(self.other)
        self.assertEqual(self.api.post(url,payload,format="json").status_code,404)
        SpaceMember.objects.create(space=self.plant.space,user=self.other,role="viewer")
        self.assertEqual(self.api.post(url,payload,format="json").status_code,200)

    def test_low_quality_and_mismatched_source_are_not_evidence_of_health(self):
        event,trace,url=self.create(size=(160,120))
        payload={"symptom":"uncertain","trace_id":trace.pk}
        result=self.api.post(url,payload,format="json")
        self.assertEqual(result.data["photo_evidence"]["status"],"review_capture")
        self.assertIn("low_resolution",result.data["photo_evidence"]["capture_flags"])
        self.assertEqual(self.api.post(url,{**payload,"symptom":"wilting"},format="json").status_code,400)
        _,other,other_url=self.create()
        self.assertEqual(self.api.post(other_url,payload,format="json").status_code,404)
        TracedRegion.objects.filter(pk=trace.pk).update(source_sha256="0"*64)
        self.assertEqual(self.api.post(url,payload,format="json").status_code,409)
        _,pot,pot_url=self.create(part="pot")
        self.assertEqual(self.api.post(pot_url,{"symptom":"uncertain","trace_id":pot.pk},format="json").status_code,409)
