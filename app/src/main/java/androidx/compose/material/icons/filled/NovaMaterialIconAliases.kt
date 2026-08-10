@file:Suppress("PackageDirectoryMismatch")

package androidx.compose.material.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import com.nova.app.ui.icons.NovaCommunicationIcons


/**
 * Tiny local fallbacks for communication icons that are not shipped in
 * material-icons-core. Keeping these as Filled aliases lets Nova use the normal
 * Compose icon API without pulling the much larger material-icons-extended
 * artifact into an unminified release build.
 */
val Icons.Filled.CallEnd: ImageVector
    get() = NovaCommunicationIcons.CallEnd

val Icons.Filled.Mic: ImageVector
    get() = NovaCommunicationIcons.Mic

val Icons.Filled.Videocam: ImageVector
    get() = NovaCommunicationIcons.Video

val Icons.Filled.VolumeUp: ImageVector
    get() = NovaCommunicationIcons.VolumeUp
