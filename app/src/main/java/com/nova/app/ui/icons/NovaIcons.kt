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
enum class NovaIconAsset(@param:DrawableRes val drawableRes: Int) {
    Home(R.drawable.ic_nova_home),
    Orbit(R.drawable.ic_nova_orbit),
    Create(R.drawable.ic_nova_add),
    Search(R.drawable.ic_nova_search),
    People(R.drawable.ic_nova_search),
    Reels(R.drawable.ic_nova_play),
    Story(R.drawable.ic_nova_story),
    Play(R.drawable.ic_nova_play),
    Pause(R.drawable.ic_nova_pause),
    VolumeOn(R.drawable.ic_nova_volume_on),
    VolumeOff(R.drawable.ic_nova_volume_off),
    Moon(R.drawable.ic_nova_moon),
    Messages(R.drawable.ic_nova_mail),
    Inbox(R.drawable.ic_nova_mail),
    Group(R.drawable.ic_nova_group),
    Room(R.drawable.ic_nova_room),
    CallAudio(R.drawable.ic_nova_call_audio),
    CallEnd(R.drawable.ic_nova_call_end),
    CallVideo(R.drawable.ic_nova_call_video),
    Info(R.drawable.ic_nova_info),
    Refresh(R.drawable.ic_nova_refresh),
    Reply(R.drawable.ic_nova_reply),
    Photo(R.drawable.ic_nova_photo),
    Microphone(R.drawable.ic_nova_microphone),
    Stop(R.drawable.ic_nova_stop),
    Check(R.drawable.ic_nova_check),
    Edit(R.drawable.ic_nova_edit),
    Notifications(R.drawable.ic_nova_notifications),
    Profile(R.drawable.ic_nova_person),
    Location(R.drawable.ic_nova_location),
    Link(R.drawable.ic_nova_link),
    Lock(R.drawable.ic_nova_lock),
    Verified(R.drawable.ic_nova_verified),
    Settings(R.drawable.ic_nova_settings),
    Back(R.drawable.ic_nova_back),
    Privacy(R.drawable.ic_nova_privacy),
    Security(R.drawable.ic_nova_security),
    Blocked(R.drawable.ic_nova_blocked),
    Policy(R.drawable.ic_nova_policy),
    AccountDeletion(R.drawable.ic_nova_account_deletion),
    Logout(R.drawable.ic_nova_logout),
    Like(R.drawable.ic_nova_like),
    LikeFilled(R.drawable.ic_nova_like_filled),
    Comment(R.drawable.ic_nova_comment),
    Repost(R.drawable.ic_nova_repost),
    Share(R.drawable.ic_nova_share),
    More(R.drawable.ic_nova_more),
    Send(R.drawable.ic_nova_send),
    Close(R.drawable.ic_nova_close),
    Delete(R.drawable.ic_nova_delete),
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
