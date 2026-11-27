package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaLiveTrackerTest {
    private val foreground = liveLensPixel(116,91,102)
    private val background = liveLensPixel(190,128,128)
    private fun frame(cx: Int = 40, cy: Int = 40, blank: Boolean = false, twin: Boolean = false): LiveLensFrame {
        val n = 80
        fun part(x: Int,y: Int,a: Int,b: Int) =
            (x-a)*(x-a)/144.0 + (y-b)*(y-b)/225.0 <= 1.0
        return LiveLensFrame(n,n,IntArray(n*n) { i ->
            val x = i%n; val y = i/n
            if (!blank && (part(x,y,cx,cy) || (twin && part(x,y,18,17)))) foreground else background
        })
    }

    @Test fun followsRegionBeyondTheOriginalCentre() {
        val tracker = LiveLensTracker()
        assertEquals(LiveLensPhase.ACQUIRING,tracker.update(frame()).phase)
        assertEquals(LiveLensPhase.TRACKING,tracker.update(frame()).phase)
        for (x in listOf(46,52,58,63)) {
            val tracked = tracker.update(frame(cx=x))
            assertEquals("Region at $x",LiveLensPhase.TRACKING,tracked.phase)
            assertTrue(tracked.region!!.contains(x,40))
            assertFalse(tracked.region.contains(x-13,40))
        }
        val offCentre = tracker.update(frame(cx=64))
        assertEquals(LiveLensPhase.TRACKING,offCentre.phase)
        assertFalse(offCentre.region!!.contains(40,40))
    }

    @Test fun lostTrackDoesNotSilentlyAcquireAnotherObject() {
        val tracker = LiveLensTracker()
        tracker.update(frame())
        tracker.update(frame())
        assertEquals(LiveLensPhase.LOST,tracker.update(frame(blank=true)).phase)
        repeat(3) { assertNull(tracker.update(frame()).region) }
        tracker.reset()
        assertEquals(LiveLensPhase.ACQUIRING,tracker.update(frame()).phase)
        assertEquals(LiveLensPhase.TRACKING,tracker.update(frame()).phase)
    }

    @Test fun duplicateRegionsOrInterruptionStopTheTrack() {
        val tracker = LiveLensTracker()
        tracker.update(frame()); tracker.update(frame())
        assertEquals(LiveLensPhase.LOST,tracker.update(frame(twin=true)).phase)
        tracker.reset()
        tracker.update(frame()); tracker.update(frame())
        tracker.interrupt()
        assertEquals(LiveLensPhase.LOST,tracker.update(frame()).phase)
    }

    @Test fun malformedFrameAndExcessiveJumpDoNotInventTracking() {
        val tracker = LiveLensTracker()
        assertEquals(LiveLensPhase.ACQUIRING,tracker.update(LiveLensFrame(80,80,IntArray(1))).phase)
        tracker.update(frame()); tracker.update(frame())
        assertEquals(LiveLensPhase.LOST,tracker.update(frame(cx=60)).phase)
    }
}
