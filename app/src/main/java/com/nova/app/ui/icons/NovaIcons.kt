package com.nova.app.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.nova.app.R

/**
 * Semantic, app-owned icon catalog for ordinary Nova chrome and navigation.
 *
 * These assets are bundled Material Symbols Rounded vector drawables so feature
 * code does not depend directly on the legacy Compose Material Icons library.
 */
enum class NovaIconAsset(@DrawableRes val drawableRes: Int) {
    Home(R.drawable.ic_nova_home),
    People(R.drawable.ic_nova_search),
    Reels(R.drawable.ic_nova_play),
    Messages(R.drawable.ic_nova_mail),
    Profile(R.drawable.ic_nova_person),
    Settings(R.drawable.ic_nova_settings),
    Back(R.drawable.ic_nova_back),
    Privacy(R.drawable.ic_nova_privacy),
    Security(R.drawable.ic_nova_security),
    Blocked(R.drawable.ic_nova_blocked),
    Policy(R.drawable.ic_nova_policy),
    AccountDeletion(R.drawable.ic_nova_account_deletion),
    Logout(R.drawable.ic_nova_logout),
}

@Composable
fun NovaIcon(
    asset: NovaIconAsset,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    Icon(
        painter = painterResource(asset.drawableRes),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}
