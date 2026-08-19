from __future__ import annotations

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
MESSAGES = ROOT / "app/src/main/java/com/nova/app/feature/messages"
CORE_GROUP_COMPAT = ROOT / "app/src/main/java/com/nova/app/core/messaging/GroupModelCompatibility.kt"

FORBIDDEN_FILENAMES = {
    "ConversationScreenV8.kt",
    "ConversationScreenV9.kt",
}

FORBIDDEN_TOKENS = {
    "ConversationScreenV8",
    "ConversationScreenV9",
    "NovaGroupManagementRepository",
    "NovaGroupMessagingRepository",
    "NovaGroupMember",
    "NovaGroupDetail",
    "NovaManagedGroupDetail",
}

REQUIRED_PATHS = {
    MESSAGES / "ConversationScreen.kt",
    MESSAGES / "conversation/ConversationContent.kt",
    MESSAGES / "conversation/ConversationViewModel.kt",
    MESSAGES / "conversation/ConversationComposer.kt",
    MESSAGES / "details/ConversationDetailsViewModel.kt",
    MESSAGES / "appearance/ConversationAppearanceViewModel.kt",
    MESSAGES / "group/GroupInfoViewModel.kt",
    MESSAGES / "group/AddGroupMembersViewModel.kt",
    MESSAGES / "group/NewGroupViewModel.kt",
    MESSAGES / "NewMessageViewModel.kt",
}


def main() -> int:
    violations: list[str] = []

    if not MESSAGES.is_dir():
        violations.append(f"missing Messages source root: {MESSAGES.relative_to(ROOT)}")
    else:
        for path in sorted(MESSAGES.rglob("*.kt")):
            if path.name in FORBIDDEN_FILENAMES:
                violations.append(f"historical live filename: {path.relative_to(ROOT)}")
            text = path.read_text(encoding="utf-8")
            for token in sorted(FORBIDDEN_TOKENS):
                if token in text:
                    violations.append(f"historical live token {token!r}: {path.relative_to(ROOT)}")

    if CORE_GROUP_COMPAT.exists():
        violations.append(
            "obsolete group model compatibility file still exists: "
            f"{CORE_GROUP_COMPAT.relative_to(ROOT)}"
        )

    for path in sorted(REQUIRED_PATHS):
        if not path.is_file():
            violations.append(f"missing stable Messages owner: {path.relative_to(ROOT)}")

    if violations:
        print("Messages architecture check failed:")
        for violation in violations:
            print(f"- {violation}")
        return 1

    print("Messages architecture check passed: stable owners present; V8/V9 and group compatibility names absent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
