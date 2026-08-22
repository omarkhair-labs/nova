# Memories product boundary

Memories is a read-model over Nova life that has already happened. It does not copy or take ownership of source content.

## Weekly engine

- `GET /api/v1/memories/week/` returns the latest completed local Monday-to-Monday week by default.
- `utc_offset_minutes` defines the user's local calendar boundary.
- `weeks_ago` allows bounded access to older completed weeks without exposing an open-ended arbitrary-date query.
- source truth remains in Pulse, Posts and Rooms; Memories performs read-only aggregation.
- v1 includes the user's own Pulses and Posts plus visible shared Room items from Rooms the user is currently a member of.
- blocked Room creators are filtered before stats, people, rooms or highlights are built.
- stats use the complete week; the highlight payload is bounded for predictable response size.
- highlights are returned chronologically so Android and the later AI film layer can consume one stable timeline contract.
- `nights` groups activity between 18:00 and 06:00 local time, assigning after-midnight activity to the preceding night.

## Ownership

`accounts.memories` owns week-window policy and the weekly recap read model. It does not own Pulse/Post/Room models or write paths and introduces no database model or migration.

The future Android `Your Week` experience and AI Memory Film may consume this contract, but film rendering or generated artifacts are intentionally outside this engine PR.
