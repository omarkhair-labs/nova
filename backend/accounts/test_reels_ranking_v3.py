import shutil
import tempfile
from datetime import timedelta

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, User
from .reels_models import Reel, ReelComment, ReelLike, ReelRepost


class ReelRankingTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-reel-ranking-media-")
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
        self.client.force_authenticate(user=self.me)
        self._user_counter = 0

    def make_user(self, suffix):
        return User.objects.create_user(
            email=f"{suffix}-ranking@example.com",
            username=f"{suffix}_ranking",
            password="StrongNovaPass2026!",
            name=f"{suffix.title()} Ranking",
        )

    def extra_user(self):
        self._user_counter += 1
        return self.make_user(f"extra{self._user_counter}")

    def make_reel(self, author, caption, age=None):
        reel = Reel.objects.create(
            author=author,
            video=SimpleUploadedFile(
                f"{author.username}-{caption}.mp4",
                b"nova-ranked-reel",
                content_type="video/mp4",
            ),
            caption=caption,
        )
        if age is not None:
            created_at = timezone.now() - age
            Reel.objects.filter(pk=reel.pk).update(created_at=created_at)
            reel.created_at = created_at
        return reel

    def feed(self, cursor=None):
        url = reverse("reels")
        if cursor is not None:
            url = f"{url}?cursor={cursor}"
        return self.client.get(url)

    def test_followed_creator_can_beat_fresher_unknown_creator(self):
        followed = self.make_user("followed")
        unknown = self.make_user("unknown")
        followed_reel = self.make_reel(followed, "followed", timedelta(days=2))
        self.make_reel(unknown, "unknown", timedelta(hours=1))
        Follow.objects.create(follower=self.me, following=followed)

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["id"], followed_reel.id)

    def test_followed_person_repost_distributes_reel_with_attribution(self):
        reposter = self.make_user("reposter")
        creator = self.make_user("socialcreator")
        fresh_creator = self.make_user("freshunknown")
        social_reel = self.make_reel(creator, "social-repost", timedelta(days=3))
        self.make_reel(fresh_creator, "fresh-unknown", timedelta(hours=1))
        Follow.objects.create(follower=self.me, following=reposter)
        ReelRepost.objects.create(reel=social_reel, user=reposter)

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        first = response.data["results"][0]
        self.assertEqual(first["id"], social_reel.id)
        self.assertEqual(first["reposted_by"]["username"], reposter.username)
        self.assertEqual(first["reposts_count"], 1)

    def test_previous_like_creates_creator_affinity(self):
        affinity_creator = self.make_user("affinity")
        other_creator = self.make_user("other")
        history = self.make_reel(affinity_creator, "history", timedelta(days=30))
        affinity_candidate = self.make_reel(affinity_creator, "candidate", timedelta(hours=4))
        self.make_reel(other_creator, "other-candidate", timedelta(hours=1))
        ReelLike.objects.create(reel=history, user=self.me)

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["id"], affinity_candidate.id)

    def test_previous_comment_creates_stronger_creator_affinity(self):
        affinity_creator = self.make_user("commented")
        other_creator = self.make_user("neutral")
        history = self.make_reel(affinity_creator, "history", timedelta(days=30))
        affinity_candidate = self.make_reel(affinity_creator, "candidate", timedelta(hours=4))
        self.make_reel(other_creator, "neutral-candidate", timedelta(hours=1))
        ReelComment.objects.create(reel=history, author=self.me, body="More like this")

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["id"], affinity_candidate.id)

    def test_engagement_lifts_a_reel_inside_same_freshness_window(self):
        engaged_creator = self.make_user("engaged")
        plain_creator = self.make_user("plain")
        engaged = self.make_reel(engaged_creator, "engaged", timedelta(hours=4))
        self.make_reel(plain_creator, "plain", timedelta(hours=1))

        for _ in range(4):
            ReelLike.objects.create(reel=engaged, user=self.extra_user())
        ReelComment.objects.create(reel=engaged, author=self.extra_user(), body="Strong reel")

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["id"], engaged.id)

    def test_cold_start_falls_back_to_fresh_content(self):
        recent_creator = self.make_user("recent")
        stale_creator = self.make_user("stale")
        recent = self.make_reel(recent_creator, "recent", timedelta(hours=2))
        self.make_reel(stale_creator, "stale", timedelta(days=10))

        response = self.feed()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"][0]["id"], recent.id)

    def test_ranked_cursor_pages_without_duplicates(self):
        creator = self.make_user("paged")
        reels = [
            self.make_reel(creator, f"page-{index}", timedelta(minutes=index))
            for index in range(30)
        ]

        first = self.feed()
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(len(first.data["results"]), 24)
        self.assertEqual(first.data["next_cursor"], "r1:24")

        second = self.feed(first.data["next_cursor"])
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertEqual(len(second.data["results"]), 6)
        self.assertIsNone(second.data["next_cursor"])

        first_ids = {row["id"] for row in first.data["results"]}
        second_ids = {row["id"] for row in second.data["results"]}
        self.assertFalse(first_ids & second_ids)
        self.assertEqual(first_ids | second_ids, {reel.id for reel in reels})

    def test_invalid_rank_cursor_is_rejected(self):
        response = self.feed("r1:not-a-number")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data["detail"], "Invalid Reels cursor.")

    def test_legacy_numeric_cursor_remains_accepted_during_rollout(self):
        creator = self.make_user("legacy")
        older = self.make_reel(creator, "older", timedelta(days=2))
        newer = self.make_reel(creator, "newer", timedelta(hours=1))

        response = self.feed(str(newer.id))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [row["id"] for row in response.data["results"]]
        self.assertIn(older.id, ids)
        self.assertNotIn(newer.id, ids)
