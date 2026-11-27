from datetime import timedelta
from io import BytesIO
from tempfile import TemporaryDirectory
from unittest import skipUnless

from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import SimpleTestCase, TestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from PIL import Image
from rest_framework.test import APIClient

from .living_registration import align_retained_photos, checked_box
from .living_segmentation import vision_available
from .models import PlantEvent, PlantUnit, Space, SpaceMember, TracedRegion


@skipUnless(vision_available(), "Install optional vision dependencies")
class VisitRegistrationGeometryTests(SimpleTestCase):
    def test_known_shift_and_changed_moving_region(self):
        import cv2
        import numpy as np
        rng = np.random.default_rng(1729)
        image = rng.integers(0,255,(480,640,3),dtype=np.uint8)
        image[160:320,240:400] = [41,115,52]
        later = cv2.warpAffine(image, np.float32([[1,0,8],[0,1,-5]]), (640,480))
        later[180:280,260:380] = [155,170,72]
        result = align_retained_photos(Image.fromarray(image), Image.fromarray(later),
            [.36,.30,.64,.70], [.37,.29,.66,.69])
        self.assertEqual(result["status"], "aligned", result)
        t = result["transform"]
        self.assertAlmostEqual(t["x"], -8/640, delta=.002)
        self.assertAlmostEqual(t["y"], 5/640, delta=.002)
        self.assertAlmostEqual(t["a"], 1., delta=.003)
        self.assertAlmostEqual(t["b"], 0., delta=.003)
        self.assertEqual(result["growth"], "not_inferred")

    def test_mixed_original_resolutions_keep_common_detail(self):
        import cv2
        import numpy as np
        image = np.random.default_rng(1729).integers(0,255,(480,640,3),dtype=np.uint8)
        later = cv2.warpAffine(image,np.float32([[1,0,8],[0,1,-5]]),(640,480))
        # A second visit may use a different camera resolution while framing is similar.
        large = Image.fromarray(later).resize((2560,1920), Image.Resampling.NEAREST)
        box = [.36,.30,.64,.70]
        result = align_retained_photos(Image.fromarray(image), large, box, box)
        self.assertEqual(result["status"], "aligned", result)
        self.assertEqual((result["frame_width"],result["frame_height"]),(640,480))
        self.assertAlmostEqual(result["transform"]["x"], -8/640, delta=.002)

    def test_changed_background_lighting_is_not_accepted(self):
        import cv2
        import numpy as np
        image = np.random.default_rng(1729).integers(0,180,(480,640,3),dtype=np.uint8)
        later = cv2.warpAffine(image,np.float32([[1,0,8],[0,1,-5]]),(640,480))
        later = (later.astype(np.int16)+60).clip(0,255).astype(np.uint8)
        box = [.36,.30,.64,.70]
        result = align_retained_photos(Image.fromarray(image), Image.fromarray(later),box,box)
        self.assertEqual(result["status"], "not_comparable", result)
        self.assertEqual(result["growth"], "not_inferred")

    def test_textureless_and_incompatible_photos_abstain(self):
        box = [.2,.2,.8,.8]
        before = Image.new("RGB",(640,480),"green")
        self.assertEqual(align_retained_photos(before, before, box, box)["status"], "not_comparable")
        self.assertEqual(align_retained_photos(before, Image.new("RGB",(480,640)), box, box)["reason"],
                         "different_frame_geometry")
        for bad in ([0.,0.,1.,1.],[.2,.2,.21,.21],[0.,0.,float("nan"),.5]):
            with self.assertRaises(ValueError): checked_box(bad)


@override_settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=True)
@skipUnless(vision_available(), "Install optional vision dependencies")
class VisitRegistrationAPITests(TestCase):
    def setUp(self):
        self.media = TemporaryDirectory()
        self.addCleanup(self.media.cleanup)
        override = self.settings(MEDIA_ROOT=self.media.name)
        override.enable()
        self.addCleanup(override.disable)
        user = get_user_model()
        self.owner = user.objects.create_user(email="registration-owner@example.test",username="registration_owner")
        self.other = user.objects.create_user(email="registration-other@example.test",username="registration_other")
        self.space = Space.objects.create(owner=self.owner,name="Synthetic registration room")
        self.plant = PlantUnit.objects.create(space=self.space,label="Synthetic plant")
        self.foreign_plant = PlantUnit.objects.create(space=self.space,label="Other synthetic plant")
        import numpy as np
        import cv2
        image = np.random.default_rng(1729).integers(0,255,(480,640,3),dtype=np.uint8)
        later = cv2.warpAffine(image,np.float32([[1,0,8],[0,1,-5]]),(640,480))
        self.a = self.event(self.plant, image, -2)
        self.b = self.event(self.plant, later, 0)
        self.same = self.event(self.plant, image, -1)
        self.foreign = self.event(self.foreign_plant, later, 0)
        self.url = reverse("plantupia-align-visits",args=[self.plant.pk])
        self.api = APIClient()
        self.api.force_authenticate(self.owner)
        self.payload = {"earlier_event_id":str(self.a.pk),"later_event_id":str(self.b.pk),
                        "earlier_exclusion":[.36,.30,.64,.70],"later_exclusion":[.37,.29,.66,.69]}

    def event(self, plant, array, days):
        output = BytesIO()
        Image.fromarray(array).save(output,format="PNG")
        return PlantEvent.objects.create(plant=plant,actor=self.owner,kind="observation",
            observed_at=timezone.now()+timedelta(days=days),
            photo=SimpleUploadedFile("registration.png",output.getvalue(),content_type="image/png"))

    def test_read_only_registration_and_membership(self):
        count = PlantEvent.objects.count()
        response = self.api.post(self.url,self.payload,format="json")
        self.assertEqual(response.status_code,200,response.data)
        self.assertEqual(response.data["status"],"aligned",response.data)
        self.assertEqual(response["Cache-Control"],"private, no-store")
        self.assertNotIn("image",response.data)
        self.api.force_authenticate(self.other)
        self.assertEqual(self.api.post(self.url,self.payload,format="json").status_code,404)
        SpaceMember.objects.create(space=self.space,user=self.other,role="viewer")
        self.assertEqual(self.api.post(self.url,self.payload,format="json").data["status"],"aligned")
        self.assertEqual(PlantEvent.objects.count(),count)
        self.assertFalse(TracedRegion.objects.exists())

    def test_invalid_comparisons_never_propose_transform(self):
        def post(**extra):
            return self.api.post(self.url,{**self.payload,**extra},format="json")
        self.assertEqual(post(later_event_id=str(self.foreign.pk)).status_code,404)
        self.assertEqual(post(later_event_id=str(self.a.pk)).data["reason"],"distinct_ordered_visits_required")
        self.assertEqual(post(later_event_id=str(self.same.pk)).data["reason"],"different_source_photographs_required")
        self.assertEqual(post(earlier_exclusion=[0,0,1,1]).status_code,400)
        with self.settings(PLANTUPIA_LIVING_SYSTEMS_ENABLED=False):
            self.assertEqual(post().status_code,404)
