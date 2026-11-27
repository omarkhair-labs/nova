package com.nova.app.feature.plants

import kotlin.math.abs
import kotlin.math.max

internal enum class LiveLensPhase { ACQUIRING, TRACKING, LOST }
internal data class LiveLensUpdate(val phase: LiveLensPhase, val region: LiveLensRegion? = null)
internal data class LiveLensFrame(val width: Int, val height: Int, val samples: IntArray)

/**
 * Small, bounded translation tracker on the sampled camera grid. Acquisition
 * requires two agreeing masks. Thereafter a fixed appearance/edge template
 * follows a moving region, and a fresh segmented mask must support the shift.
 * It never silently acquires another subject after loss. This is screen-space
 * correspondence, not a semantic leaf detector or persistent biological ID.
 */
internal class LiveLensTracker {
    private data class Sample(val x: Int, val y: Int, val pixel: Int)
    private data class Anchor(val region: LiveLensRegion, val samples: List<Sample>)
    private data class Match(val x: Int, val y: Int, val cost: Float)
    private var pending: LiveLensRegion? = null
    private var anchor: Anchor? = null
    private var previous: LiveLensRegion? = null
    private var dx = 0
    private var dy = 0
    private var lost = false

    fun reset() {
        pending = null
        anchor = null
        previous = null
        dx = 0
        dy = 0
        lost = false
    }

    fun interrupt() {
        if (anchor != null) lost = true
        pending = null
        anchor = null
        previous = null
    }

    private fun fail(): LiveLensUpdate {
        interrupt()
        lost = true
        return LiveLensUpdate(LiveLensPhase.LOST)
    }

    fun update(frame: LiveLensFrame?): LiveLensUpdate {
        if (lost) return LiveLensUpdate(LiveLensPhase.LOST)
        val original = anchor
        if (frame == null || frame.width !in 24..96 || frame.height !in 24..96 ||
            frame.samples.size != frame.width * frame.height) {
            if (original != null) return fail()
            pending = null
            return LiveLensUpdate(LiveLensPhase.ACQUIRING)
        }
        val (w, h, pixels) = frame
        if (original == null) {
            val next = proposeLiveLensRegion(w, h, pixels)
            val stable = stableLiveLensRegion(pending, next)
            pending = next
            if (stable == null) return LiveLensUpdate(LiveLensPhase.ACQUIRING)
            val template = boundaryTemplate(stable, pixels)
            if (template.size < 32) return LiveLensUpdate(LiveLensPhase.ACQUIRING)
            anchor = Anchor(stable, template)
            previous = stable
            pending = null
            return LiveLensUpdate(LiveLensPhase.TRACKING, stable)
        }
        if (original.region.width != w || original.region.height != h) return fail()
        val candidates = ArrayList<Match>(225)
        // At most 15 by 15 shifts, each scored on <= 256 samples.
        for (y in dy - 7..dy + 7) for (x in dx - 7..dx + 7) {
            val cost = matchCost(original.samples, frame, x, y) ?: continue
            candidates.add(Match(x, y, cost))
        }
        val best = candidates.minWithOrNull(compareBy<Match> { it.cost }
            .thenBy { abs(it.x-dx)+abs(it.y-dy) }) ?: return fail()
        if (best.cost > 18f) return fail()
        val alternative = candidates.filter { max(abs(it.x-best.x), abs(it.y-best.y)) > 2 }
            .minOfOrNull { it.cost } ?: return fail()
        if (alternative - best.cost < max(2f, best.cost * .25f)) return fail()
        val next = proposeLiveLensRegion(w, h, pixels, w/2+best.x, h/2+best.y) ?: return fail()
        val recent = previous ?: return fail()
        // Require independent support from both the immutable acquisition mask
        // and the last mask; no gradual drift to a different same-colour object.
        if (original.region.shiftedOverlap(next,best.x,best.y) < .65f ||
            recent.shiftedOverlap(next,best.x-dx,best.y-dy) < .65f) return fail()
        dx = best.x
        dy = best.y
        previous = next
        return LiveLensUpdate(LiveLensPhase.TRACKING, next)
    }

    private fun boundaryTemplate(region: LiveLensRegion, pixels: IntArray): List<Sample> {
        val inside = ArrayList<Sample>()
        val outside = ArrayList<Sample>()
        val w = region.width
        val h = region.height
        for (y in 1 until h-1) for (x in 1 until w-1) {
            val present = region.contains(x,y)
            val edge = region.contains(x-1,y) != present || region.contains(x+1,y) != present ||
                region.contains(x,y-1) != present || region.contains(x,y+1) != present
            if (edge) (if (present) inside else outside).add(Sample(x,y,pixels[y*w+x]))
        }
        if (inside.size < 16 || outside.size < 16) return emptyList()
        fun bounded(list: List<Sample>): List<Sample> {
            val n = minOf(128,list.size)
            return List(n) { list[it*list.size/n] }
        }
        return bounded(inside) + bounded(outside)
    }

    private fun matchCost(template: List<Sample>, frame: LiveLensFrame, dx: Int, dy: Int): Float? {
        var total = 0
        for (sample in template) {
            val x = sample.x+dx
            val y = sample.y+dy
            if (x !in 0 until frame.width || y !in 0 until frame.height) return null
            val other = frame.samples[y*frame.width+x]
            fun channel(value: Int, shift: Int) = (value shr shift) and 255
            total += abs(channel(other,16)-channel(sample.pixel,16))/2 +
                abs(channel(other,8)-channel(sample.pixel,8)) +
                abs(channel(other,0)-channel(sample.pixel,0))
        }
        return total.toFloat()/template.size
    }
}
