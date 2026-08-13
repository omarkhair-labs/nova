# Nova architecture consolidation

Status: **temporary feature freeze** for architecture work, beginning from
`master` at `dcbd534f6ba6656832d08b55d94097a302c8e99b` (Android 2.1.3 / 20103).

During the freeze, merge only:

- the sequential consolidation PRs described in the governing plan;
- urgent production or security fixes that cannot safely wait; or
- explicitly approved release work.

Architecture PRs must preserve visible product behavior and the protected
contracts in [NOVA_ARCHITECTURE_AUDIT_AND_MASTER_PROMPT.md](NOVA_ARCHITECTURE_AUDIT_AND_MASTER_PROMPT.md).
They must not bump Android versions or publish to Google Play.

## Baseline records

- [Governing plan](NOVA_ARCHITECTURE_AUDIT_AND_MASTER_PROMPT.md)
- [Current ownership and entry paths](OWNERSHIP.md)
- [REST and WebSocket inventory](ROUTE_INVENTORY.md)
- [Samsung smoke checklist](SAMSUNG_SMOKE_CHECKLIST.md)
- [Progress log](PROGRESS.md)

The inventories are contract snapshots, not proposals. Update them in the same
PR whenever an approved ownership change moves an implementation while keeping
its external contract stable.
