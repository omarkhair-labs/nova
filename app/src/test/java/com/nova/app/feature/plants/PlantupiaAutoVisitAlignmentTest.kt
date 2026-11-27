package com.nova.app.feature.plants

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaAutoVisitAlignmentTest {
    @Test fun exclusionIsInImageCoordinatesAndRequiresBackground() {
        assertEquals(VisitExclusion(.2,.3,.8,.7),visitExclusion(listOf(VisitPoint(.8,.7),VisitPoint(.2,.3))))
        assertNull(visitExclusion(listOf(VisitPoint(0.0,0.0),VisitPoint(1.0,1.0))))
        assertNull(visitExclusion(listOf(VisitPoint(.2,.3),VisitPoint(.21,.31))))
        assertNull(visitExclusion(listOf(VisitPoint(Double.NaN,.3),VisitPoint(.8,.7))))
    }

    @Test fun onlyValidBoundedRegistrationCanBeDisplayed() {
        val raw = JSONObject("""{"status":"aligned","model":"static-orb-similarity-v1","basis":"operator_marked_moving_regions",
            "frame_width":640,"frame_height":480,"test_matches":12,"test_p75_error_px":0.8,
            "transform":{"a":1,"b":0,"x":-0.0125,"y":0.0078125}}""")
        assertEquals(VisitTransform(1.0,0.0,-.0125,.0078125),parseVisitRegistration(raw)?.transform)
        assertNull(parseVisitRegistration(JSONObject(raw.toString()).put("status","not_comparable")))
        assertNull(parseVisitRegistration(JSONObject(raw.toString()).put("test_matches",3)))
        assertNull(parseVisitRegistration(JSONObject(raw.toString()).put("test_p75_error_px",9)))
        raw.getJSONObject("transform").put("x",10)
        assertNull(parseVisitRegistration(raw))
    }
}
