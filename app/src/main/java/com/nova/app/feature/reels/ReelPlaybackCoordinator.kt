package com.nova.app.feature.reels

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nova.app.core.reels.NovaReel
import java.util.UUID


data class ReelWatchSnapshot(
    val sessionId: String,
    val watchedMs: Long,
    val durationMs: Long,
    val maxPositionMs: Long,
)


class ReelPlayerPool(context: Context) {
    private val appContext = context.applicationContext
    private val entries = LinkedHashMap<Long, PlayerEntry>()

    fun playerFor(reel: NovaReel): ExoPlayer {
        val existing = entries[reel.id]
        if (existing != null && existing.videoUrl == reel.videoUrl) {
            return existing.player
        }
        existing?.player?.release()
        return ExoPlayer.Builder(appContext).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            setMediaItem(MediaItem.fromUri(reel.videoUrl))
            prepare()
        }.also { player ->
            entries[reel.id] = PlayerEntry(reel.videoUrl, player)
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

    fun releaseAll() {
        entries.values.forEach { it.player.release() }
        entries.clear()
    }

    private data class PlayerEntry(
        val videoUrl: String,
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
        if (duration > 0L && duration != Player.TIME_UNSET) {
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
