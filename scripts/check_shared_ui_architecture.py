#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java"
DRAWABLE = ROOT / "app/src/main/res/drawable"
SHADOW_ANDROIDX = MAIN / "androidx"
CALL_ACTIVITY = MAIN / "com/nova/app/CallActivity.kt"
SETTINGS_ACTIVITY = MAIN / "com/nova/app/SettingsActivity.kt"
PROFILE_SCREEN = MAIN / "com/nova/app/feature/profile/ProfileScreen.kt"
NOTIFICATIONS_SCREEN = MAIN / "com/nova/app/feature/notifications/NotificationsScreen.kt"
HOME_SCREEN = MAIN / "com/nova/app/feature/home/HomeScreen.kt"
HOME_IDENTITY_HEADER = MAIN / "com/nova/app/feature/home/HomeIdentityHeader.kt"
PEOPLE_SCREEN = MAIN / "com/nova/app/feature/people/PeopleScreen.kt"
ICON_ALIASES = MAIN / "com/nova/app/ui/icons/NovaMaterialIconAliases.kt"
SHARED_UI = MAIN / "com/nova/app/ui"
SHARED_COMPONENTS = SHARED_UI / "components"
SHARED_ICONS = SHARED_UI / "icons"
THEME = SHARED_UI / "theme"
ICON_CATALOG = SHARED_ICONS / "NovaIcons.kt"
ICON_BUTTON = SHARED_COMPONENTS / "NovaIconButton.kt"
FEEDBACK = SHARED_COMPONENTS / "NovaFeedback.kt"
CARD = SHARED_COMPONENTS / "NovaCard.kt"
STATUS = SHARED_COMPONENTS / "NovaStatus.kt"
LEGACY_COMPONENTS = SHARED_COMPONENTS / "NovaComponents.kt"
COMMUNICATION_ICON_ALIASES = ("CallEnd", "Mic", "Videocam", "VolumeUp")
NARROW_COMPONENT_SEAMS = {
    "NovaButtons.kt": ("fun NovaPrimaryButton(", "fun NovaSecondaryButton("),
    "NovaTextField.kt": ("fun NovaTextField(",),
    "NovaHeader.kt": ("fun NovaHeader(",),
    "NovaBottomBar.kt": ("fun NovaBottomBar(", "enum class NovaTab"),
    "NovaIconButton.kt": ("fun NovaIconButton(", "fun NovaBackButton("),
    "NovaFeedback.kt": (
        "fun NovaLoadingState(",
        "fun NovaInlineLoading(",
        "fun NovaEmptyState(",
        "actionLabel: String? = null",
        "fun NovaErrorState(",
        "fun NovaInlineRetry(",
    ),
    "NovaCard.kt": ("fun NovaCard(", "MaterialTheme.shapes.large", "BorderStroke(1.dp"),
    "NovaStatus.kt": ("fun NovaUnreadDot(", ".size(8.dp)"),
    "NovaOrbitRing.kt": ("fun NovaOrbitRing(", "Canvas(", "NovaBaseLive"),
}
DESIGN_FOUNDATION_SEAMS = {
    "Color.kt": ("NovaBaseBackground", "NovaBaseAccent", "NovaBaseLive", "NovaColorOverride"),
    "Type.kt": ("object NovaType", "val Typography = Typography(", "val pageTitle", "val button"),
    "Shape.kt": ("val NovaShapes = Shapes(", "extraSmall", "extraLarge"),
    "Space.kt": ("object NovaSpacing", "val sm = 8.dp", "val xxxl = 32.dp"),
    "Elevation.kt": ("object NovaElevation", "val flat = 0.dp", "val floating = 6.dp"),
    "Motion.kt": ("object NovaMotion", "const val fast = 120", "const val emphasized = 320"),
    "Theme.kt": ("fun NovaTheme(", "typography = Typography", "shapes = NovaShapes"),
}
MIGRATED_COMPONENT_SEAMS = {
    "NovaButtons.kt": ("MaterialTheme.shapes.medium", "NovaType.button", "NovaElevation.flat"),
    "NovaTextField.kt": ("MaterialTheme.shapes.medium",),
    "NovaHeader.kt": ("NovaType.pageTitle", "NovaType.subtitle", "NovaSpacing.sm", "NovaBackButton"),
    "NovaBottomBar.kt": ("NovaElevation.floating", "NovaType.navigationLabel", "NovaType.badge", "NovaIconAsset.Home"),
    "NovaFeedback.kt": ("MaterialTheme.shapes.extraLarge", "NovaType.bodyCompact", "NovaSpacing.xxxl"),
    "NovaCard.kt": ("MaterialTheme.shapes.large", "NovaSurface", "NovaBorder"),
    "NovaStatus.kt": ("NovaAccent", "CircleShape"),
}
MATERIAL_SYMBOL_DRAWABLES = (
    "ic_nova_home.xml",
    "ic_nova_search.xml",
    "ic_nova_play.xml",
    "ic_nova_mail.xml",
    "ic_nova_notifications.xml",
    "ic_nova_person.xml",
    "ic_nova_settings.xml",
    "ic_nova_back.xml",
    "ic_nova_privacy.xml",
    "ic_nova_security.xml",
    "ic_nova_blocked.xml",
    "ic_nova_policy.xml",
    "ic_nova_account_deletion.xml",
    "ic_nova_logout.xml",
)
MIGRATED_ICON_CONSUMERS = (
    SHARED_COMPONENTS / "NovaBottomBar.kt",
    SHARED_COMPONENTS / "NovaHeader.kt",
    HOME_IDENTITY_HEADER,
    PROFILE_SCREEN,
    SETTINGS_ACTIVITY,
)

errors: list[str] = []

shadow_sources = list(SHADOW_ANDROIDX.rglob("*.kt")) if SHADOW_ANDROIDX.exists() else []
for path in shadow_sources:
    errors.append(
        "application source must not shadow the androidx namespace: "
        f"{path.relative_to(ROOT)}"
    )

for source in MAIN.rglob("*.kt"):
    text = source.read_text(encoding="utf-8")
    for import_name in COMMUNICATION_ICON_ALIASES:
        forbidden = f"import androidx.compose.material.icons.filled.{import_name}"
        if forbidden in text:
            errors.append(
                "communication icon alias must be imported from app-owned ui.icons: "
                f"{source.relative_to(ROOT)} -> {forbidden}"
            )

if LEGACY_COMPONENTS.exists():
    errors.append(
        "shared UI must stay split by responsibility; legacy dumping-ground file exists: "
        f"{LEGACY_COMPONENTS.relative_to(ROOT)}"
    )

for file_name, seams in NARROW_COMPONENT_SEAMS.items():
    component_file = SHARED_COMPONENTS / file_name
    if not component_file.is_file():
        errors.append(f"missing narrow shared UI owner: {component_file.relative_to(ROOT)}")
        continue
    component_text = component_file.read_text(encoding="utf-8")
    for seam in seams:
        if seam not in component_text:
            errors.append(f"shared UI public seam moved unexpectedly: {file_name} -> {seam}")

for file_name, seams in DESIGN_FOUNDATION_SEAMS.items():
    foundation_file = THEME / file_name
    if not foundation_file.is_file():
        errors.append(f"missing Nova design-system foundation: {foundation_file.relative_to(ROOT)}")
        continue
    foundation_text = foundation_file.read_text(encoding="utf-8")
    for seam in seams:
        if seam not in foundation_text:
            errors.append(f"Nova design-system foundation seam changed: {file_name} -> {seam}")

for file_name, seams in MIGRATED_COMPONENT_SEAMS.items():
    component_file = SHARED_COMPONENTS / file_name
    if not component_file.is_file():
        continue
    component_text = component_file.read_text(encoding="utf-8")
    for seam in seams:
        if seam not in component_text:
            errors.append(f"shared component bypassed Nova design-system foundation: {file_name} -> {seam}")

if not ICON_CATALOG.is_file():
    errors.append("missing app-owned Nova icon catalog")
else:
    icon_catalog_text = ICON_CATALOG.read_text(encoding="utf-8")
    for seam in (
        "enum class NovaIconAsset",
        "fun NovaIcon(",
        "Home(R.drawable.ic_nova_home)",
        "Search(R.drawable.ic_nova_search)",
        "Notifications(R.drawable.ic_nova_notifications)",
        "Back(R.drawable.ic_nova_back)",
        "Settings(R.drawable.ic_nova_settings)",
    ):
        if seam not in icon_catalog_text:
            errors.append(f"Nova icon catalog seam changed: {seam}")

for drawable_name in MATERIAL_SYMBOL_DRAWABLES:
    drawable = DRAWABLE / drawable_name
    if not drawable.is_file():
        errors.append(f"missing Nova Material Symbol drawable: {drawable.relative_to(ROOT)}")

for consumer in MIGRATED_ICON_CONSUMERS:
    if not consumer.is_file():
        errors.append(f"missing migrated icon consumer: {consumer.relative_to(ROOT)}")
        continue
    text = consumer.read_text(encoding="utf-8")
    if "import androidx.compose.material.icons." in text:
        errors.append(
            "migrated Nova chrome must use app-owned icon catalog, not legacy Compose icons: "
            f"{consumer.relative_to(ROOT)}"
        )

for forbidden_glyph in ('text = "‹"', 'icon = "◉"', 'icon = "⌁"', 'icon = "⊘"', 'icon = "×"', 'text = "↪"'):
    if forbidden_glyph in SETTINGS_ACTIVITY.read_text(encoding="utf-8") or forbidden_glyph in (SHARED_COMPONENTS / "NovaHeader.kt").read_text(encoding="utf-8"):
        errors.append(f"action glyph survived DS-2 migration: {forbidden_glyph}")

if not FEEDBACK.is_file():
    errors.append("missing shared Nova feedback component owner")
if not CARD.is_file():
    errors.append("missing shared Nova card owner")
if not STATUS.is_file():
    errors.append("missing shared Nova status owner")

if not NOTIFICATIONS_SCREEN.is_file():
    errors.append("missing Notifications screen")
else:
    notifications_text = NOTIFICATIONS_SCREEN.read_text(encoding="utf-8")
    for seam in (
        "NovaBackButton(onClick = onBack)",
        "NovaLoadingState(",
        "NovaEmptyState(",
        "NovaErrorState(",
        "NovaInlineLoading(",
        "NovaInlineRetry(",
        "NovaUnreadDot()",
        "NovaType.screenTitle",
    ):
        if seam not in notifications_text:
            errors.append(f"Notifications bypassed migrated Nova feedback/chrome/status seam: {seam}")
    if 'text = "Back"' in notifications_text:
        errors.append("Notifications restored a local text Back control after design-system migration")

settings_text = SETTINGS_ACTIVITY.read_text(encoding="utf-8")
for seam in ("NovaCard(", "NovaType.screenTitle", "NovaSpacing.xxl", "SettingsSectionLabel"):
    if seam not in settings_text:
        errors.append(f"Settings bypassed Nova container/type/spacing seam: {seam}")

profile_text = PROFILE_SCREEN.read_text(encoding="utf-8")
for seam in ("NovaCard(", "NovaType.pageTitle", "NovaType.sectionTitle", "NovaSpacing.xxl"):
    if seam not in profile_text:
        errors.append(f"Profile bypassed Nova container/type/spacing seam: {seam}")

home_text = HOME_SCREEN.read_text(encoding="utf-8")
for seam in (
    "HomeIdentityHeader(",
    "NovaCard(",
    "NovaLoadingState(",
    "NovaErrorState(",
    "NovaEmptyState(",
    "NovaInlineLoading(",
    "NovaInlineRetry(",
    "NovaSpacing.xl",
):
    if seam not in home_text:
        errors.append(f"Home bypassed Nova DS-3/identity seam: {seam}")

if not HOME_IDENTITY_HEADER.is_file():
    errors.append("missing Home identity header owner")
else:
    home_identity_text = HOME_IDENTITY_HEADER.read_text(encoding="utf-8")
    for seam in (
        "NovaType.display",
        "NovaType.screenTitle",
        "NovaType.subtitle",
        "NovaIconAsset.Search",
        "NovaIconAsset.Notifications",
        "NovaOrbitRing(",
        "NovaUnreadDot(",
        "NovaSpacing.",
    ):
        if seam not in home_identity_text:
            errors.append(f"Home identity header bypassed approved visual-identity seam: {seam}")

people_text = PEOPLE_SCREEN.read_text(encoding="utf-8")
for seam in (
    "NovaCard(",
    "NovaLoadingState(",
    "NovaEmptyState(",
    "NovaType.pageTitle",
    "NovaSpacing.xl",
):
    if seam not in people_text:
        errors.append(f"People bypassed Nova container/type/spacing seam: {seam}")
if "EmptyPeopleCard(" in people_text:
    errors.append("People restored its feature-local empty card after DS-3 migration")

if not ICON_ALIASES.is_file():
    errors.append("missing app-owned communication icon aliases")
else:
    aliases = ICON_ALIASES.read_text(encoding="utf-8")
    if "package com.nova.app.ui.icons" not in aliases:
        errors.append("communication icon aliases must stay in the app-owned ui.icons namespace")
    for seam in (
        "val Icons.Filled.CallEnd: ImageVector",
        "val Icons.Filled.Mic: ImageVector",
        "val Icons.Filled.Videocam: ImageVector",
        "val Icons.Filled.VolumeUp: ImageVector",
        "NovaCommunicationIcons.CallEnd",
        "NovaCommunicationIcons.Mic",
        "NovaCommunicationIcons.Video",
        "NovaCommunicationIcons.VolumeUp",
    ):
        if seam not in aliases:
            errors.append(f"app-owned communication icon alias seam changed: {seam}")

activity = CALL_ACTIVITY.read_text(encoding="utf-8")
for import_name in COMMUNICATION_ICON_ALIASES:
    required = f"import com.nova.app.ui.icons.{import_name}"
    if required not in activity:
        errors.append(f"CallActivity must import app-owned icon alias: {required}")

for usage in (
    "Icons.Filled.CallEnd",
    "Icons.Filled.Mic",
    "Icons.Filled.Videocam",
    "Icons.Filled.VolumeUp",
):
    if usage not in activity:
        errors.append(f"CallActivity icon usage changed unexpectedly: {usage}")

if errors:
    print("Shared UI architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Shared UI architecture check passed.")
