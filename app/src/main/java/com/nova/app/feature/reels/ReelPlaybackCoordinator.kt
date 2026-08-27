package com.nova.app.feature.reels

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nova.app.feature.reels.domain.model.NovaReel
import java.lang.ref.WeakReference
import java.util.UUID


data class ReelWatchSnapshot(
    val sessionId: String,
    val watchedMs: Long,
    val durationMs: Long,
    val maxPositionMs: Long,
)


/**
 * Activity-level safety net for Reel playback.
 *
 * Reels live in their own Activity, but finishing/switching root tabs can happen
 * before Compose disposes every page. Keeping weak references to active pools lets
 * the Activity pause audio synchronously from onPause without retaining screens.
 */
object ReelPlaybackSafety {
    private val pools = mutableListOf<WeakReference<ReelPlayerPool>>()

    @Synchronized
    internal fun register(pool: ReelPlayerPool) {
        pruneLocked()
        if (pools.none { it.get() === pool }) {
            pools += WeakReference(pool)
        }
    }

    @Synchronized
    fun pauseAll() {
        val activePools = pools.mapNotNull { it.get() }
        pruneLocked()
        activePools.forEach { it.pauseAll() }
    }

    @Synchronized
    private fun pruneLocked() {
        pools.removeAll { it.get() == null }
    }
}


class ReelPlayerPool(context: Context) {
    private val appContext = context.applicationContext
    private val entries = LinkedHashMap<Long, PlayerEntry>()

    init {
        ReelPlaybackSafety.register(this)
    }

    fun playerFor(reel: NovaReel): ExoPlayer {
        // A Reel's media is immutable after creation. Interaction responses can
        // rebuild an equivalent absolute URL, so the Reel id is the stable media
        // identity. Reusing by id preserves the decoder, buffer and playhead across
        // Like/Repost/comment count updates instead of flashing/restarting playback.
        entries[reel.id]?.let { return it.player }

        return ExoPlayer.Builder(appContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            setMediaItem(MediaItem.fromUri(reel.videoUrl))
            prepare()
        }.also { player ->
            entries[reel.id] = PlayerEntry(player)
        }
    }

    fun retainAround(reels: List<NovaReel>, centerIndex: Int) {
        if (reels.isEmpty()) {
            releaseAll()
            return
        }
        val safeCenter = centerIndex.coerceIn(0, reels.lastIndex)
        val keep = buildSet {
            for (index in (safeCenter - 1)..(safeCenter + 1)) {
                if (index in reels.indices) {
                    add(reels[index].id)
                    playerFor(reels[index])
                }
            }
        }
        val staleIds = entries.keys.filterNot { it in keep }
        staleIds.forEach { id ->
            entries.remove(id)?.player?.release()
        }
    }

    fun pauseAllExcept(reelId: Long?) {
        entries.forEach { (id, entry) ->
            if (id != reelId && entry.player.isPlaying) {
                entry.player.pause()
            }
        }
    }

    fun pauseAll() {
        entries.values.forEach { entry ->
            if (entry.player.isPlaying) entry.player.pause()
        }
    }

    fun releaseAll() {
        entries.values.forEach { it.player.release() }
        entries.clear()
    }

    private data class PlayerEntry(
        val player: ExoPlayer,
    )
}


class ReelWatchSession {
    private val sessionId = UUID.randomUUID().toString()
    private var lastSampleAt = SystemClock.elapsedRealtime()
    private var watchedMs = 0L
    private var maxPositionMs = 0L
    private var durationMs = 0L

    fun sample(player: Player) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = (now - lastSampleAt).coerceAtLeast(0L)
        if (player.isPlaying) {
            watchedMs += elapsed
        }
        lastSampleAt = now

        val position = player.currentPosition.coerceAtLeast(0L)
        maxPositionMs = maxOf(maxPositionMs, position)
        val duration = player.duration
        if (duration > 0L && duration != C.TIME_UNSET) {
            durationMs = maxOf(durationMs, duration)
        }
    }

    fun finish(player: Player): ReelWatchSnapshot {
        sample(player)
        return ReelWatchSnapshot(
            sessionId = sessionId,
            watchedMs = watchedMs,
            durationMs = durationMs,
            maxPositionMs = maxPositionMs,
        )
    }
}
