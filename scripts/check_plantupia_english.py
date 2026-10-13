#!/usr/bin/env python3
"""Prevent regression to Arabic hard-coded copy in English-first Plantupia UI.

Only checks newly converted native product-owned entrypoints and plant module.
Inherited Nova social/inbox screens must be separately localized and audited.
User-authored Unicode data and test fixtures are deliberately out of scope.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/nova/app"
PLANTS = JAVA / "feature/plants"
ENTRYPOINTS = [
    JAVA / "ui/components/NovaBottomBar.kt",
    JAVA / "feature/welcome/WelcomeScreen.kt",
    JAVA / "feature/home/HomeScreen.kt",
    JAVA / "feature/create/CreateHubScreen.kt",
    JAVA / "feature/orbit/OrbitScreen.kt",
    JAVA / "feature/profile/ProfileScreen.kt",
    JAVA / "SettingsActivity.kt",
    JAVA / "MainActivity.kt",
]
ARABIC_SCRIPT = re.compile(r"[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF]")

checked = sorted(PLANTS.rglob("*.kt")) + ENTRYPOINTS
failures = []
for path in checked:
    if not path.is_file():
        failures.append(f"missing audited UI source: {path.relative_to(ROOT)}")
        continue
    for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if ARABIC_SCRIPT.search(line):
            failures.append(f"{path.relative_to(ROOT)}:{line_no}: unexpected Arabic-script literal")

if failures:
    print("Plantupia English-first UI guard failed:")
    for issue in failures:
        print(" -", issue)
    sys.exit(1)

print(f"Plantupia English-first UI guard passed ({len(checked)} native source files).")
