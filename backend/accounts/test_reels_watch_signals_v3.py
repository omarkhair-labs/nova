import shutil
import tempfile
import uuid
from datetime import timedelta

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import User
from .reels_models import Reel, ReelWatch


class ReelWatchSignalTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-reel-watch-media-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.me = self.make_user("viewer")
        self.creator = self.make_user("creator")
        self.client.force_authenticate(user=self.me)

    def make_user(self, suffix):
        return User.objects.create_user(
            email=f"{suffix}-watch@example.com",
            username=f"{suffix}_watch",
            password="StrongNovaPass2026!",
            name=f"{suffix.title()} Watch",
        )

    def make_reel(self, author, caption, age=None):
        reel = Reel.objects.create(
            author=author,
            video=SimpleUploadedFile(
                f"{author.username}-{caption}.mp4",
                b"nova-watch-reel",
                content_type="video/mp4",
            ),
            caption=caption,
        )
        if age is not None:
            created_at = timezone.now() - age
            Reel.objects.filter(pk=reel.pk).update(created_at=created_at)
            reel.created_at = created_at
        return reel

    def watch(self, reel, *, session_id=None, watched_ms, duration_ms, max_position_ms):
        return self.client.post(
            reverse("reel-watch", kwargs={"reel_id": reel.id}),
            {
                "session_id": str(session_id or uuid.uuid4()),
                "watched_ms": watched_ms,
                "duration_ms": duration_ms,
                "max_position_ms": max_position_ms,
            },
            format="json",
        )

    def feed(self):
        return self.client.get(reverse("reels"))

    def test_watch_session_aggregates_completion_replay_and_dedupes_retry(self):
        reel = self.make_reel(self.creator, "completion")
        first_session = uuid.uuid4()

        first = self.watch(
            reel,
            session_id=first_session,
            watched_ms=9500,
            duration_ms=10000,
            max_position_ms=9500,
        )
        duplicate = self.watch(
            reel,
            session_id=first_session,
            watched_ms=9500,
            duration_ms=10000,
            max_position_ms=9500,
        )
        replay = self.watch(
            reel,
            watched_ms=12000,
            duration_ms=10000,
            max_position_ms=9900,
        )

        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertTrue(first.data["recorded"])
        self.assertEqual(first.data["completion_count"], 1)
        self.assertEqual(first.data["max_completion_permille"], 950)

        self.assertEqual(duplicate.status_code, status.HTTP_200_OK)
        self.assertFalse(duplicate.data["recorded"])
        self.assertTrue(duplicate.data["duplicate"])

        self.assertEqual(replay.status_code, status.HTTP_200_OK)
        summary = ReelWatch.objects.get(reel=reel, user=self.me)
        self.assertEqual(summary.sessions, 2)
        self.assertEqual(summary.total_watch_ms, 21500)
        self.assertEqual(summary.completion_count, 2)
        self.assertEqual(summary.replay_count, 1)
        self.assertEqual(summary.quick_skip_count, 0)
        self.assertEqual(summary.max_completion_permille, 990)

    def test_fast_swipe_records_quick_skip(self):
        reel = self.make_reel(self.creator, "skip")

        response = self.watch(
            reel,
            watched_ms=800,
            duration_ms=10000,
            max_position_ms=800,
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        summary = ReelWatch.objects.get(reel=reel, user=self.me)
        self.assertEqual(summary.sessions, 1)
        self.assertEqual(summary.quick_skip_count, 1)
        self.assertEqual(summary.completion_count, 0)
        self.assertEqual(summary.max_completion_permille, 80)

    def test_tiny_accidental_exposure_is_not_recorded(self):
        reel = self.make_reel(self.creator, "tiny")

        response = self.watch(
            reel,
            watched_ms=120,
            duration_ms=10000,
            max_position_ms=120,
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["recorded"])
        self.assertFalse(ReelWatch.objects.filter(reel=reel, user=self.me).exists())

    def test_own_reel_watch_never_teaches_ranking(self):
        own_reel = self.make_reel(self.me, "mine")

        response = self.watch(
            own_reel,
            watched_ms=9500,
            duration_ms=10000,
            max_position_ms=9500,
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["recorded"])
        self.assertFalse(ReelWatch.objects.filter(reel=own_reel, user=self.me).exists())

    def test_invalid_watch_payload_is_rejected(self):
        reel = self.make_reel(self.creator, "invalid")

        bad_session = self.client.post(
            reverse("reel-watch", kwargs={"reel_id": reel.id}),
            {
                "session_id": "not-a-uuid",
                "watched_ms": 1000,
                "duration_ms": 10000,
                "max_position_ms": 1000,
            },
            format="json",
        )
        bad_timing = self.client.post(
            reverse("reel-watch", kwargs={"reel_id": reel.id}),
            {
                "session_id": str(uuid.uuid4()),
                "watched_ms": -1,
                "duration_ms": 10000,
                "max_position_ms": 1000,
            },
            format="json",
        )

        self.assertEqual(bad_session.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(bad_timing.status_code, status.HTTP_400_BAD_REQUEST)

    def test_completed_creator_history_boosts_next_reel(self):
        other_creator = self.make_user("other")
        history = self.make_reel(self.creator, "history", timedelta(days=20))
        candidate = self.make_reel(self.creator, "candidate", timedelta(hours=4))
        self.make_reel(other_creator, "other", timedelta(hours=1))
        ReelWatch.objects.create(
            reel=history,
            user=self.me,
            sessions=1,
            total_watch_ms=9000,
            max_completion_permille=900,
            completion_count=1,
        )

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["id"], candidate.id)

    def test_quick_skipped_reel_is_demoted_below_neutral_reel(self):
        other_creator = self.make_user("neutral")
        skipped = self.make_reel(self.creator, "skipped", timedelta(hours=1))
        neutral = self.make_reel(other_creator, "neutral", timedelta(hours=1))
        ReelWatch.objects.create(
            reel=skipped,
            user=self.me,
            sessions=1,
            total_watch_ms=700,
            max_completion_permille=70,
            quick_skip_count=1,
        )

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [row["id"] for row in response.data["results"]]
        self.assertLess(ids.index(neutral.id), ids.index(skipped.id))

    def test_completed_reel_is_demoted_to_improve_feed_diversity(self):
        other_creator = self.make_user("fresh")
        completed = self.make_reel(self.creator, "already-finished", timedelta(hours=1))
        neutral = self.make_reel(other_creator, "not-seen", timedelta(hours=1))
        ReelWatch.objects.create(
            reel=completed,
            user=self.me,
            sessions=1,
            total_watch_ms=9500,
            max_completion_permille=950,
            completion_count=1,
        )

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [row["id"] for row in response.data["results"]]
        self.assertLess(ids.index(neutral.id), ids.index(completed.id))
