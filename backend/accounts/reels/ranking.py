from datetime import datetime, timedelta, timezone as datetime_timezone

from django.db.models import Case, Exists, F, IntegerField, OuterRef, Value, When
from django.utils import timezone

from .models import Follow
from .reels_models import ReelComment, ReelLike, ReelRepost, ReelWatch


RANK_CURSOR_PREFIX = "r2:"
LEGACY_RANK_CURSOR_PREFIX = "r1:"

# Ranking weights intentionally stay server-side so Nova can tune discovery
# without requiring a new Android release.
FOLLOW_BOOST = 36
FOLLOWED_REPOST_BOOST = 30
LIKED_CREATOR_BOOST = 14
COMMENTED_CREATOR_BOOST = 20
REPOSTED_CREATOR_BOOST = 18
WATCHED_CREATOR_BOOST = 22
REPLAYED_CREATOR_BOOST = 12
LIKED_REEL_PENALTY = -8
REPOSTED_REEL_PENALTY = -6
COMPLETED_REEL_PENALTY = -18
QUICK_SKIPPED_REEL_PENALTY = -26
OWN_REEL_PENALTY = -16


def ranked_reels_for(user, queryset, *, watch_cutoff=None):
    """Rank already-visible Reels for a specific viewer.

    ``watch_cutoff`` freezes watch-derived signals for one pagination session.
    The Android client reports watch behavior while the user swipes, so without
    this cutoff page-two ordering could shift underneath an offset cursor.
    """
    now = timezone.now()
    watch_cutoff = watch_cutoff or now

    follows_creator = Follow.objects.filter(
        follower=user,
        following_id=OuterRef("author_id"),
    )
    liked_creator_before = ReelLike.objects.filter(
        user=user,
        reel__author_id=OuterRef("author_id"),
    ).exclude(reel_id=OuterRef("pk"))
    commented_creator_before = ReelComment.objects.filter(
        author=user,
        reel__author_id=OuterRef("author_id"),
    ).exclude(reel_id=OuterRef("pk"))
    reposted_creator_before = ReelRepost.objects.filter(
        user=user,
        reel__author_id=OuterRef("author_id"),
    ).exclude(reel_id=OuterRef("pk"))
    watched_creator_well_before = ReelWatch.objects.filter(
        user=user,
        reel__author_id=OuterRef("author_id"),
        max_completion_permille__gte=700,
        last_watched_at__lte=watch_cutoff,
    ).exclude(reel_id=OuterRef("pk"))
    replayed_creator_before = ReelWatch.objects.filter(
        user=user,
        reel__author_id=OuterRef("author_id"),
        replay_count__gte=1,
        last_watched_at__lte=watch_cutoff,
    ).exclude(reel_id=OuterRef("pk"))
    liked_current_reel = ReelLike.objects.filter(
        user=user,
        reel_id=OuterRef("pk"),
    )
    reposted_current_reel = ReelRepost.objects.filter(
        user=user,
        reel_id=OuterRef("pk"),
    )
    completed_current_reel = ReelWatch.objects.filter(
        user=user,
        reel_id=OuterRef("pk"),
        completion_count__gte=1,
        last_watched_at__lte=watch_cutoff,
    )
    quick_skipped_current_reel = ReelWatch.objects.filter(
        user=user,
        reel_id=OuterRef("pk"),
        quick_skip_count__gte=1,
        completion_count=0,
        last_watched_at__lte=watch_cutoff,
    )

    return (
        queryset.annotate(
            follow_score=Case(
                When(Exists(follows_creator), then=Value(FOLLOW_BOOST)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            followed_repost_score=Case(
                When(has_followed_repost_value=True, then=Value(FOLLOWED_REPOST_BOOST)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            liked_creator_score=Case(
                When(Exists(liked_creator_before), then=Value(LIKED_CREATOR_BOOST)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            commented_creator_score=Case(
                When(Exists(commented_creator_before), then=Value(COMMENTED_CREATOR_BOOST)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            reposted_creator_score=Case(
                When(Exists(reposted_creator_before), then=Value(REPOSTED_CREATOR_BOOST)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            watched_creator_score=Case(
                When(Exists(watched_creator_well_before), then=Value(WATCHED_CREATOR_BOOST)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            replayed_creator_score=Case(
                When(Exists(replayed_creator_before), then=Value(REPLAYED_CREATOR_BOOST)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            freshness_score=Case(
                When(created_at__gte=now - timedelta(hours=6), then=Value(32)),
                When(created_at__gte=now - timedelta(hours=24), then=Value(26)),
                When(created_at__gte=now - timedelta(days=3), then=Value(18)),
                When(created_at__gte=now - timedelta(days=7), then=Value(10)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            engagement_score=(
                F("likes_count_value") * Value(2)
                + F("comments_count_value") * Value(4)
                + F("reposts_count_value") * Value(3)
            ),
            liked_reel_penalty=Case(
                When(Exists(liked_current_reel), then=Value(LIKED_REEL_PENALTY)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            reposted_reel_penalty=Case(
                When(Exists(reposted_current_reel), then=Value(REPOSTED_REEL_PENALTY)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            completed_reel_penalty=Case(
                When(Exists(completed_current_reel), then=Value(COMPLETED_REEL_PENALTY)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            quick_skipped_reel_penalty=Case(
                When(Exists(quick_skipped_current_reel), then=Value(QUICK_SKIPPED_REEL_PENALTY)),
                default=Value(0),
                output_field=IntegerField(),
            ),
            own_reel_penalty=Case(
                When(author=user, then=Value(OWN_REEL_PENALTY)),
                default=Value(0),
                output_field=IntegerField(),
            ),
        )
        .annotate(
            rank_score=(
                F("follow_score")
                + F("followed_repost_score")
                + F("liked_creator_score")
                + F("commented_creator_score")
                + F("reposted_creator_score")
                + F("watched_creator_score")
                + F("replayed_creator_score")
                + F("freshness_score")
                + F("engagement_score")
                + F("liked_reel_penalty")
                + F("reposted_reel_penalty")
                + F("completed_reel_penalty")
                + F("quick_skipped_reel_penalty")
                + F("own_reel_penalty")
            )
        )
        .order_by("-rank_score", "-created_at", "-id")
    )


def parse_rank_cursor(raw_cursor):
    """Return ``(offset, legacy_pk_cursor, watch_cutoff)``.

    V3 watch-aware pages carry the first-page watch cutoff in the opaque cursor
    so watch events created while scrolling cannot reorder the current session.
    Old ``r1:<offset>`` and numeric cursors remain accepted during rollout.
    """
    raw = str(raw_cursor or "").strip()
    now = timezone.now()
    if not raw:
        return 0, None, now

    if raw.startswith(RANK_CURSOR_PREFIX):
        payload = raw[len(RANK_CURSOR_PREFIX) :]
        parts = payload.split(":", 1)
        if len(parts) != 2 or not all(part.isdigit() for part in parts):
            raise ValueError("invalid ranked cursor")
        cutoff_ms = int(parts[0])
        offset = int(parts[1])
        if cutoff_ms <= 0 or offset < 0:
            raise ValueError("invalid ranked cursor")
        try:
            watch_cutoff = datetime.fromtimestamp(
                cutoff_ms / 1000.0,
                tz=datetime_timezone.utc,
            )
        except (OverflowError, OSError, ValueError) as exc:
            raise ValueError("invalid ranked cursor") from exc
        return offset, None, watch_cutoff

    if raw.startswith(LEGACY_RANK_CURSOR_PREFIX):
        value = raw[len(LEGACY_RANK_CURSOR_PREFIX) :]
        if not value.isdigit():
            raise ValueError("invalid legacy ranked cursor")
        offset = int(value)
        if offset < 0:
            raise ValueError("invalid legacy ranked cursor")
        return offset, None, now

    try:
        legacy_pk = int(raw)
    except (TypeError, ValueError) as exc:
        raise ValueError("invalid legacy cursor") from exc
    if legacy_pk <= 0:
        raise ValueError("invalid legacy cursor")
    return 0, legacy_pk, now


def encode_rank_cursor(offset, watch_cutoff):
    cutoff_ms = int(watch_cutoff.timestamp() * 1000)
    return f"{RANK_CURSOR_PREFIX}{cutoff_ms}:{int(offset)}"
