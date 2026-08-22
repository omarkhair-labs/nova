# Rooms Live product boundary

Rooms Live is an additive product layer on the group-backed Rooms foundation.

- Messaging remains the source of truth for group membership, roles, chat and realtime messaging.
- Rooms owns Room items, Room Tonight activity, item creation UI/state and Room-specific media upload behavior.
- Tonight may compose the feature-owned `RoomTonightSection` as UI, but it does not own Rooms models, endpoints or repository state.
- Room Tonight is activity-based and does not expose raw online presence or last-seen data.
- Photo/video preparation happens off the main thread and respects the backend limits (12 MB photos, 60 MB videos).
- No Android version/signing or Google Play publishing behavior is changed by this slice.
