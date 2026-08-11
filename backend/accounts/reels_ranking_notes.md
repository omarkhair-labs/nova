# Reels ranking rollout notes

Nova V3 Reels ranking is intentionally server-owned. The Android client keeps using the existing `/api/v1/reels/` contract while the backend orders visible reels per viewer.

Signals in V1 ranking:

- following the creator
- prior likes on that creator's other reels
- prior comments on that creator's other reels
- current reel likes and comments
- freshness buckets
- small penalties for already-liked reels and the viewer's own reels

Cold-start viewers have no affinity boosts, so their feed naturally falls back to freshness plus engagement.

The ranked feed emits opaque `r1:<offset>` cursors but accepts the previous numeric primary-key cursor during rollout for compatibility with already-open sessions.

Visibility is not part of ranking. `visible_reels_for()` still owns blocking and private-account access before ranking is applied.
