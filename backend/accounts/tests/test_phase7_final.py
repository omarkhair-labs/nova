from pathlib import Path

from django.test import SimpleTestCase


LEGACY_ROOT_MODULES = (
    "account_security",
    "jwt_auth",
    "social_paging",
    "privacy_views",
    "sharing_views",
    "profile_reels",
    "reels_ranking",
    "group_management",
    "group_messaging",
    "messaging_mutation_view",
    "messaging_paging",
    "messaging_realtime",
    "messaging_serializers",
    "messaging_v9_views",
    "messaging_views",
    "presence_store",
    "realtime",
    "call_history",
    "call_realtime",
    "call_reliability_realtime",
    "call_reliability_view",
)


class Phase7FinalArchitectureTests(SimpleTestCase):
    def test_no_legacy_backend_shims_or_references(self):
        accounts_dir = Path(__file__).resolve().parents[1]
        backend_dir = accounts_dir.parent
        this_file = Path(__file__).resolve()

        remaining_files = [
            name
            for name in LEGACY_ROOT_MODULES
            if (accounts_dir / f"{name}.py").exists()
        ]

        references = []
        legacy_paths = {
            (accounts_dir / f"{name}.py").resolve()
            for name in LEGACY_ROOT_MODULES
        }
        for path in sorted(backend_dir.rglob("*.py")):
            resolved = path.resolve()
            if resolved == this_file or resolved in legacy_paths or "migrations" in path.parts:
                continue
            source = path.read_text(encoding="utf-8")
            is_accounts_root_module = path.parent.resolve() == accounts_dir.resolve()
            for line_number, line in enumerate(source.splitlines(), start=1):
                for name in LEGACY_ROOT_MODULES:
                    full = f"accounts.{name}"
                    patterns = [
                        f"from {full} import",
                        f"import {full}",
                        f"{full}.",
                        f'"{full}"',
                        f"'{full}'",
                    ]
                    if is_accounts_root_module:
                        patterns.extend(
                            (
                                f"from .{name} import",
                                f"from .{name} ",
                                f"from . import {name}",
                            )
                        )
                    if any(pattern in line for pattern in patterns):
                        references.append(
                            f"{path.relative_to(backend_dir)}:{line_number}: {line.strip()}"
                        )
                        break

        details = []
        if remaining_files:
            details.append("legacy files still present: " + ", ".join(remaining_files))
        if references:
            details.append("legacy references:\n" + "\n".join(references))

        self.assertEqual(details, [], "\n\n".join(details))
