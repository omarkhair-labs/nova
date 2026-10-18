"""F05: a lost acknowledgement is a replay, never a second logical save."""
from concurrent.futures import ThreadPoolExecutor
from datetime import timedelta
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Barrier
from unittest.mock import patch
from django.db.models.signals import post_save
import uuid

from PIL import Image
from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import close_old_connections, connection, connections
from django.test import TestCase, TransactionTestCase, override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import Post, Notification
from .models import PlantDiscussion, PlantEvent, PlantUnit, Space, SpaceMember, PlantSubmission


class SubmissionFixtures:
    def setUp(self):
        self.media = TemporaryDirectory()
        self.storage = override_settings(MEDIA_ROOT=self.media.name)
        self.storage.enable()
        self.addCleanup(self.media.cleanup)
        self.addCleanup(self.storage.disable)
        User = get_user_model()
        self.owner = User.objects.create_user(email="save-a@example.test", username="save_a", password="test-only")
        self.other = User.objects.create_user(email="save-b@example.test", username="save_b", password="test-only")
        self.space = Space.objects.create(owner=self.owner, name="Private space")
        self.plant = PlantUnit.objects.create(space=self.space, label="Living record")
        self.second = PlantUnit.objects.create(space=self.space, label="Another plant")
        self.client = APIClient()
        self.client.force_authenticate(self.owner)
        self.key = str(uuid.uuid4())
        self.when = timezone.now().isoformat()

    def photo(self, color="green", name="photo.png"):
        out = BytesIO()
        Image.new("RGB", (8, 8), color).save(out, format="PNG")
        return SimpleUploadedFile(name, out.getvalue(), content_type="image/png")

    def event(self, **extra):
        return dict(client_submission_id=self.key, kind="care", evidence_type="observed",
                    note="Observed soil before caring", observed_at=self.when, **extra)

    def question(self, color="green"):
        return dict(client_submission_id=self.key, public_share_consent="true",
                    public_topic="Leaf observation", caption="What should I observe next?", image=self.photo(color))

    def files(self):
        return sorted(str(p.relative_to(self.media.name)) for p in Path(self.media.name).rglob("*") if p.is_file())


class PlantupiaSubmissionTests(SubmissionFixtures, TestCase):
    def test_json_lost_ack_replays_original_event_without_changing_history(self):
        path = reverse("plantupia-events", args=[self.plant.pk])
        first = self.client.post(path, self.event(), format="json")
        self.assertEqual(first.status_code, 201)
        retry = self.client.post(path, self.event(), format="json")
        self.assertEqual(retry.status_code, 200)
        self.assertEqual(first.json(), retry.json())
        self.assertEqual(retry.json()["client_submission_id"], self.key)
        self.assertEqual(PlantEvent.objects.count(), 1)

    def test_multipart_replay_does_not_duplicate_private_photo(self):
        path = reverse("plantupia-events", args=[self.plant.pk])
        first = self.client.post(path, self.event(photo=self.photo()), format="multipart")
        stored = self.files()
        retry = self.client.post(path, self.event(photo=self.photo(name="different-name.png")), format="multipart")
        self.assertEqual(retry.status_code, 200)
        self.assertEqual(first.json(), retry.json())
        self.assertEqual(len(stored), 1)
        self.assertEqual(stored, self.files())

    def test_conflicting_note_scope_kind_or_photo_is_rejected(self):
        path = reverse("plantupia-events", args=[self.plant.pk])
        self.assertEqual(self.client.post(path, self.event(photo=self.photo()), format="multipart").status_code, 201)
        for change in ({"note": "Different care"}, {"kind": "issue"}, {"photo": self.photo("red")}):
            body = self.event(photo=self.photo())
            body.update(change)
            self.assertEqual(self.client.post(path, body, format="multipart").status_code, 409)
        self.assertEqual(self.client.post(reverse("plantupia-events", args=[self.second.pk]), self.event(), format="json").status_code, 409)
        self.assertEqual(PlantEvent.objects.count(), 1)
        self.assertEqual(len(self.files()), 1)

    def test_same_identity_in_other_account_is_independent_not_an_information_leak(self):
        path = reverse("plantupia-events", args=[self.plant.pk])
        first = self.client.post(path, self.event(), format="json")
        self.client.force_authenticate(self.other)
        self.assertEqual(self.client.post(path, self.event(), format="json").status_code, 404)
        SpaceMember.objects.create(space=self.space, user=self.other, role=SpaceMember.Role.CARER)
        second = self.client.post(path, self.event(), format="json")
        self.assertEqual(second.status_code, 201)
        self.assertNotEqual(first.json()["id"], second.json()["id"])
        self.assertEqual(PlantEvent.objects.count(), 2)

    def test_permissions_are_rechecked_on_replay(self):
        SpaceMember.objects.create(space=self.space, user=self.other, role=SpaceMember.Role.CARER)
        self.client.force_authenticate(self.other)
        path = reverse("plantupia-events", args=[self.plant.pk])
        self.assertEqual(self.client.post(path, self.event(), format="json").status_code, 201)
        SpaceMember.objects.filter(user=self.other).update(role=SpaceMember.Role.VIEWER)
        self.assertEqual(self.client.post(path, self.event(), format="json").status_code, 403)
        SpaceMember.objects.filter(user=self.other).delete()
        self.assertEqual(self.client.post(path, self.event(), format="json").status_code, 404)

    def test_completed_followup_replays_original_outcome_before_mutable_state_validation(self):
        source = PlantEvent.objects.create(plant=self.plant, actor=self.owner, kind="care", observed_at=timezone.now(), follow_up_on=timezone.localdate()+timedelta(days=1))
        body = self.event(follow_up_for=str(source.pk))
        body["kind"] = "outcome"
        path = reverse("plantupia-events", args=[self.plant.pk])
        first = self.client.post(path, body, format="json")
        self.assertEqual(first.status_code, 201)
        retry = self.client.post(path, body, format="json")
        self.assertEqual(retry.status_code, 200)
        self.assertEqual(first.json(), retry.json())
        self.assertEqual(PlantEvent.objects.count(), 2)

    def test_public_question_lost_ack_replays_one_post_link_and_sanitized_photo(self):
        path = reverse("plantupia-questions", args=[self.plant.pk])
        first = self.client.post(path, self.question(), format="multipart")
        self.assertEqual(first.status_code, 201)
        stored = self.files()
        retry = self.client.post(path, self.question(), format="multipart")
        self.assertEqual(retry.status_code, 200)
        self.assertEqual(first.json(), retry.json())
        self.assertEqual(Post.objects.count(), 1)
        self.assertEqual(PlantDiscussion.objects.count(), 1)
        self.assertEqual(stored, self.files())
        with Image.open(Post.objects.get().image.path) as image:
            self.assertFalse(image.getexif())

    def test_question_conflict_missing_consent_and_cross_operation_are_rejected(self):
        path = reverse("plantupia-questions", args=[self.plant.pk])
        self.assertEqual(self.client.post(path, self.question(), format="multipart").status_code, 201)
        body = self.question(); body["caption"] = "Changed question"
        self.assertEqual(self.client.post(path, body, format="multipart").status_code, 409)
        self.assertEqual(self.client.post(path, self.question("red"), format="multipart").status_code, 409)
        body = self.question(); body["public_share_consent"] = "false"
        self.assertEqual(self.client.post(path, body, format="multipart").status_code, 400)
        self.assertEqual(self.client.post(reverse("plantupia-events", args=[self.plant.pk]), self.event(), format="json").status_code, 409)
        self.assertEqual(Post.objects.count(), 1)

    def test_new_protocol_requires_identity_and_old_protocol_remains_compatible(self):
        path = f"/api/v1/plantupia/plants/{self.plant.pk}/event-submissions/"
        body = self.event(); body.pop("client_submission_id")
        self.assertEqual(self.client.post(path, body, format="json").status_code, 400)
        body["client_submission_id"] = "invalid"
        self.assertEqual(self.client.post(path, body, format="json").status_code, 400)
        self.assertEqual(PlantEvent.objects.count(), 0)
        body.pop("client_submission_id")
        self.assertEqual(self.client.post(reverse("plantupia-events", args=[self.plant.pk]), body, format="json").status_code, 201)

    def test_durable_validation_rejection_cannot_race_with_changed_content(self):
        path = reverse("plantupia-event-submissions", args=[self.plant.pk])
        body = self.event(); body["kind"] = "invalid"
        first = self.client.post(path, body, format="json")
        self.assertEqual(first.status_code, 400)
        self.assertEqual(first.json()["code"], "submission_rejected")
        self.assertEqual(first.json()["request_id"], self.key)
        self.assertEqual(self.client.post(path, body, format="json").json(), first.json())
        self.assertEqual(self.client.post(path, self.event(), format="json").status_code, 409)
        self.assertEqual(PlantEvent.objects.count(), 0)
        self.assertEqual(PlantSubmission.objects.count(), 1)
        self.key = str(uuid.uuid4())
        self.assertEqual(self.client.post(path, self.event(), format="json").status_code, 201)

    def test_json_and_multipart_identical_fields_share_one_receipt(self):
        path = reverse("plantupia-event-submissions", args=[self.plant.pk])
        first = self.client.post(path, self.event(), format="json")
        replay = self.client.post(path, self.event(), format="multipart")
        self.assertEqual(replay.status_code, 200)
        self.assertEqual(first.json(), replay.json())
        self.assertEqual(PlantEvent.objects.count(), 1)

    def test_storage_write_survives_database_rollback_and_retry_reuses_the_blob(self):
        path = reverse("plantupia-question-submissions", args=[self.plant.pk])
        with patch("plants.views.PlantDiscussion.objects.create", side_effect=RuntimeError("Simulated DB failure")):
            with self.assertRaises(RuntimeError):
                self.client.post(path, self.question(), format="multipart")
        self.assertEqual(Post.objects.count(), 0)
        self.assertEqual(PlantSubmission.objects.count(), 0)
        original_files = self.files()
        self.assertEqual(len(original_files), 1)
        self.assertEqual(self.client.post(path, self.question(), format="multipart").status_code, 201)
        self.assertEqual(self.files(), original_files)
        self.assertEqual(Post.objects.count(), 1)

    def test_replay_never_resurrects_a_deleted_public_post(self):
        path = reverse("plantupia-question-submissions", args=[self.plant.pk])
        first = self.client.post(path, self.question(), format="multipart")
        Post.objects.get().delete()
        replay = self.client.post(path, self.question(), format="multipart")
        self.assertEqual(replay.status_code, 200)
        self.assertEqual(first.json(), replay.json())
        self.assertEqual(Post.objects.count(), 0)
        self.assertEqual(PlantDiscussion.objects.count(), 0)
        self.assertEqual(self.files(), [])

    def test_replay_does_not_repeat_model_creation_or_notification_side_effects(self):
        # Plantupia does not currently notify followers for a newly published question.
        # A test receiver proves creation-triggered DB effects run only for the winner.
        def notification(sender, instance, created, **kwargs):
            if created:
                Notification.objects.create(recipient=self.other, actor=self.owner, kind="comment", post=instance,
                                            dedupe_key=f"submission-test:{instance.pk}")
        post_save.connect(notification, sender=Post, weak=False)
        try:
            path = reverse("plantupia-question-submissions", args=[self.plant.pk])
            self.assertEqual(self.client.post(path, self.question(), format="multipart").status_code, 201)
            self.assertEqual(self.client.post(path, self.question(), format="multipart").status_code, 200)
            self.assertEqual(Notification.objects.count(), 1)
        finally:
            post_save.disconnect(notification, sender=Post)


class PlantupiaSubmissionConcurrencyTests(SubmissionFixtures, TransactionTestCase):
    def test_simultaneous_json_and_photo_submissions_have_one_commit(self):
        if connection.vendor != "postgresql":
            self.skipTest("Row/unique contention is verified on PostgreSQL CI, not SQLite.")
        for public in (False, True):
            self.key = str(uuid.uuid4())
            barrier = Barrier(2)
            path = reverse("plantupia-questions" if public else "plantupia-events", args=[self.plant.pk])
            def submit():
                close_old_connections()
                try:
                    client = APIClient(); client.force_authenticate(self.owner)
                    data = self.question() if public else self.event(photo=self.photo())
                    barrier.wait(timeout=10)
                    response = client.post(path, data, format="multipart")
                    return response.status_code, response.json()
                finally:
                    connections.close_all()
            with ThreadPoolExecutor(max_workers=2) as executor:
                results = list(executor.map(lambda _: submit(), range(2)))
            self.assertEqual(sorted(x[0] for x in results), [200, 201])
            self.assertEqual(results[0][1], results[1][1])
        self.assertEqual(PlantEvent.objects.count(), 1)
        self.assertEqual(Post.objects.count(), 1)
        self.assertEqual(PlantDiscussion.objects.count(), 1)
        self.assertEqual(len(self.files()), 2)

    def test_simultaneous_conflicting_submissions_have_one_winner(self):
        if connection.vendor != "postgresql":
            self.skipTest("Real row contention requires PostgreSQL.")
        barrier = Barrier(2)
        path = reverse("plantupia-event-submissions", args=[self.plant.pk])
        def submit(index):
            close_old_connections()
            try:
                client = APIClient(); client.force_authenticate(self.owner)
                data = self.event(); data["note"] = f"Different intent {index}"
                barrier.wait(timeout=10)
                return client.post(path, data, format="json").status_code
            finally:
                connections.close_all()
        with ThreadPoolExecutor(max_workers=2) as executor:
            results = list(executor.map(submit, range(2)))
        self.assertEqual(sorted(results), [201, 409])
        self.assertEqual(PlantEvent.objects.count(), 1)
        self.assertEqual(PlantSubmission.objects.count(), 1)
