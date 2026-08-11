from datetime import timedelta

from django.db.models import Case, Exists, F, IntegerField, OuterRef, Value, When
from django.utils import timezone

from .models import Follow
from .reels_models import ReelComment, ReelLike


RANK_CURSOR_PREFIX = "r1:"

# Ranking weights intentionally stay server-side so Nova can tune discovery
# without requiring a new Android release.
FOLLOW_BOOST = 36
LIKED_CREATOR_BOOST = 14
COMMENTED_CREATOR_BOOST = 20
LIKED_REEL_PENALTY = -8
OWN_REEL_PENALTY = -16


def ranked_reels_for(user, queryset):
    """
    Rank already-visible reels for a specific viewer.

    Visibility is deliberately handled before this function by visible_reels_for;
    this layer only changes ordering. New/cold-start accounts naturally fall
    back to freshness + engagement because their affinity boosts are zero.
    """
    now = timezone.now()

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
    liked_current_reel = ReelLike.objects.filter(
        user=user,
        reel_id=OuterRef("pk"),
    )

    return (
        queryset.annotate(
            follow_score=Case(
                When(Exists(follows_creator), then=Value(FOLLOW_BOOST)),
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
            ),
            liked_reel_penalty=Case(
                When(Exists(liked_current_reel), then=Value(LIKED_REEL_PENALTY)),
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
                + F("liked_creator_score")
                + F("commented_creator_score")
                + F("freshness_score")
                + F("engagement_score")
                + F("liked_reel_penalty")
                + F("own_reel_penalty")
            )
        )
        .order_by("-rank_score", "-created_at", "-id")
    )


def parse_rank_cursor(raw_cursor):
    """
    Return (offset, legacy_pk_cursor).

    New ranked pages use an opaque offset cursor. Plain numeric cursors from the
    previous chronological feed are still accepted so an already-open Android
    session does not break when the backend deploys.
    """
    raw = str(raw_cursor or "").strip()
    if not raw:
        return 0, None

    if raw.startswith(RANK_CURSOR_PREFIX):
        value = raw[len(RANK_CURSOR_PREFIX) :]
        if not value.isdigit():
            raise ValueError("invalid ranked cursor")
        offset = int(value)
        if offset < 0:
            raise ValueError("invalid ranked cursor")
        return offset, None

    try:
        legacy_pk = int(raw)
    except (TypeError, ValueError) as exc:
        raise ValueError("invalid legacy cursor") from exc
    if legacy_pk <= 0:
        raise ValueError("invalid legacy cursor")
    return 0, legacy_pk


def encode_rank_cursor(offset):
    return f"{RANK_CURSOR_PREFIX}{int(offset)}"
