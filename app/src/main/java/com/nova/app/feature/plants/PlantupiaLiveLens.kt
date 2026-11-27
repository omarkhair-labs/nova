package com.nova.app.feature.plants

import kotlin.math.abs

/** A temporary screen-space foreground proposal, never a botanical observation. */
internal class LiveLensRegion(val width: Int, val height: Int, private val cells: BooleanArray) {
    init { require(width in 24..96 && height in 24..96 && cells.size == width * height) }

    fun contains(x: Int, y: Int): Boolean =
        x in 0 until width && y in 0 until height && cells[y * width + x]

    fun overlap(other: LiveLensRegion): Float {
        if (width != other.width || height != other.height) return 0f
        var intersection = 0
        var union = 0
        for (i in cells.indices) {
            if (cells[i] && other.cells[i]) intersection++
            if (cells[i] || other.cells[i]) union++
        }
        return if (union == 0) 0f else intersection.toFloat() / union
    }
}

/** Pack the three unsigned YUV samples without a full-frame bitmap conversion. */
internal fun liveLensPixel(y: Int, u: Int, v: Int): Int = (y shl 16) or (u shl 8) or v

/**
 * Bounded, prompt-centred region growing in YUV. This is an inexpensive visual
 * framing aid, not a trained leaf detector or a substitute for the saved-photo
 * GrabCut/review pipeline. Use a contrasting background and one complete part.
 * Weak contrast, ambiguous/fragmented foreground and cropped parts abstain.
 */
internal fun proposeLiveLensRegion(width: Int, height: Int, samples: IntArray): LiveLensRegion? {
    if (width !in 24..96 || height !in 24..96 || samples.size != width * height) return null
    val cx = width / 2
    val cy = height / 2
    val center = ArrayList<Int>(25)
    for (y in cy - 2..cy + 2) for (x in cx - 2..cx + 2) center.add(samples[y * width + x])

    fun channel(pixel: Int, shift: Int) = (pixel shr shift) and 255
    fun medianChannel(shift: Int) = center.map { channel(it, shift) }.sorted()[center.size / 2]
    val yy = medianChannel(16)
    val uu = medianChannel(8)
    val vv = medianChannel(0)
    if (yy < 34 || yy > 237) return null
    fun distance(pixel: Int): Int = abs(channel(pixel, 16) - yy) / 2 +
        abs(channel(pixel, 8) - uu) + abs(channel(pixel, 0) - vv)

    val seedSpread = center.map(::distance).sorted()[20]
    if (seedSpread > 25) return null
    val distances = IntArray(samples.size) { distance(samples[it]) }
    val perimeter = ArrayList<Int>(2 * (width + height))
    for (x in 0 until width) {
        perimeter.add(distances[x])
        perimeter.add(distances[(height - 1) * width + x])
    }
    for (y in 1 until height - 1) {
        perimeter.add(distances[y * width])
        perimeter.add(distances[y * width + width - 1])
    }
    perimeter.sort()
    val background = perimeter[perimeter.size / 2]
    if (background < maxOf(25, seedSpread * 2 + 12)) return null
    val threshold = minOf((seedSpread + 14).coerceIn(16, 38), background * 2 / 3)
    val origin = cy * width + cx
    if (distances[origin] > threshold) return null

    val accepted = BooleanArray(samples.size)
    val queue = IntArray(samples.size)
    var start = 0
    var end = 0
    fun add(index: Int) {
        if (!accepted[index] && distances[index] <= threshold) {
            accepted[index] = true
            queue[end++] = index
        }
    }
    add(origin)
    while (start < end) {
        val index = queue[start++]
        val x = index % width
        val y = index / width
        // A region cut by the analysis frame has no complete visible outline.
        if (x <= 1 || x >= width - 2 || y <= 1 || y >= height - 2) return null
        add(index - 1)
        add(index + 1)
        add(index - width)
        add(index + width)
        if (end > samples.size * .64) return null
    }
    if (end < maxOf(25, samples.size / 40)) return null
    // Reject a colour that occurs widely in separate foreground islands.
    val similar = distances.count { it <= threshold }
    if (end < .65 * similar) return null

    var boundary = 0
    var distinct = 0
    for (i in 0 until end) {
        val index = queue[i]
        for (neighbor in intArrayOf(index - 1, index + 1, index - width, index + width)) {
            if (!accepted[neighbor]) {
                boundary++
                if (distances[neighbor] >= threshold + 7) distinct++
            }
        }
    }
    if (boundary == 0 || distinct < boundary * .60) return null
    return LiveLensRegion(width, height, accepted)
}

/** Require consecutive agreeing proposals, never keep a lost outline alive. */
internal fun stableLiveLensRegion(previous: LiveLensRegion?, next: LiveLensRegion?): LiveLensRegion? =
    next?.takeIf { previous != null && previous.overlap(it) >= .65f }
