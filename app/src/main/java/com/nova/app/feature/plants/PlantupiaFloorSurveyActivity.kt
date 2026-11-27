package com.nova.app.feature.plants

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.nova.app.ui.theme.NovaTheme
import java.util.Locale

/** Optional, private, local measurement session. It never writes a room or saves a camera frame. */
class PlantupiaFloorSurveyActivity : ComponentActivity() {
    companion object {
        internal const val WIDTH = "plantupia.survey.width"
        internal const val SOURCE = "plantupia.survey.source"
        internal const val FEATURES = "plantupia.survey.features"
        internal const val LENGTH = "plantupia.survey.length"
    }
    private lateinit var gl: GLSurfaceView
    private lateinit var renderer: PlantupiaFloorRenderer
    private var session: Session? = null
    private var requested = false
    private var installRequested = false
    private var resumed = false
    private var glRunning = false
    private var checking by mutableStateOf(false)
    private var started by mutableStateOf(false)
    private var error by mutableStateOf("")
    private var state by mutableStateOf(FloorFrame())
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else {
            requested = false
            checking = false
            error = "Camera permission is required only for this optional survey. Manual recording is available."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        renderer = PlantupiaFloorRenderer(
            rotation = { gl.display?.rotation ?: 0 },
            publish = { frame -> runOnUiThread { if (resumed && !isFinishing) state = frame } },
            completed = { result, features, source -> runOnUiThread {
                if (resumed && !isFinishing && glRunning) {
                    setResult(Activity.RESULT_OK, Intent().putExtra(WIDTH,result.width).putExtra(LENGTH,result.length)
                        .putExtra(FEATURES, features).putExtra(SOURCE, source))
                    finish()
                }
            }},
            failure = { runOnUiThread {
                if (!isFinishing) {
                    stopSession()
                    error = "AR tracking stopped. You can retry or record the room manually."
                }
            }},
        )
        gl = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
        }
        setContent {
            NovaTheme {
                Surface(color=PlantupiaColors.paper, modifier=Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                        .verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=10.dp),
                        verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick={finish()}) { Text("Back to room") }
                        Text("Floor survey",style=PlantupiaText.pageHeading)
                        if (!started) {
                            Text("Trace the room in light.",style=PlantupiaText.display)
                            Text("Measure a rectangular floor by marking four corners in order. Start from the corner you will use as your room origin. Mark the width direction next.",style=PlantupiaText.body)
                            Text("This application runs on Google Play Services for AR (ARCore), which is provided by Google and governed by the Google Privacy Policy.",
                                style=PlantupiaText.supporting)
                            TextButton(onClick={
                                runCatching { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://policies.google.com/privacy"))) }
                            }) { Text("Google Privacy Policy") }
                            Text("The survey returns estimated dimensions and a 3D preview to your room draft. You can include the tracked points when saving a private layout. Camera frames stay on this device.",
                                style=PlantupiaText.supporting)
                            Button(enabled=!checking,onClick={requestStart()},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {
                                Text(if(checking) "Checking AR support…" else "Start floor survey")
                            }
                        } else {
                            Box(Modifier.fillMaxWidth().height(320.dp).clip(PlantupiaShapes.card)
                                .background(Color(0xFF14251F))) {
                                AndroidView(factory={gl},modifier=Modifier.fillMaxSize()
                                    .semantics { contentDescription = "Live AR floor camera" })
                                Canvas(Modifier.fillMaxSize()) {
                                    val c=center
                                    val r=13.dp.toPx()
                                    val ink=if(state.canMark) Color(0xFFCBF4C3) else Color.White
                                    drawCircle(Color.Black.copy(alpha=.5f),r+3.dp.toPx(),c,style=Stroke(4.dp.toPx()))
                                    drawCircle(ink,r,c,style=Stroke(2.dp.toPx()))
                                    drawLine(ink,c-Offset(r+6.dp.toPx(),0f),c-Offset(r-2.dp.toPx(),0f),2.dp.toPx())
                                    drawLine(ink,c+Offset(r-2.dp.toPx(),0f),c+Offset(r+6.dp.toPx(),0f),2.dp.toPx())
                                    drawLine(ink,c-Offset(0f,r+6.dp.toPx()),c-Offset(0f,r-2.dp.toPx()),2.dp.toPx())
                                    drawLine(ink,c+Offset(0f,r-2.dp.toPx()),c+Offset(0f,r+6.dp.toPx()),2.dp.toPx())
                                    val projected=state.projected.map { p -> p?.let { Offset(it.first*size.width,it.second*size.height) } }
                                    projected.zipWithNext().forEach { (a,b) ->
                                        if(a!=null && b!=null) drawLine(Color(0xFFCBF4C3),a,b,2.dp.toPx())
                                    }
                                    if(projected.size==4) {
                                        val a=projected.first();val b=projected.last()
                                        if(a!=null && b!=null) drawLine(Color(0xFFCBF4C3),a,b,2.dp.toPx())
                                    }
                                    projected.filterNotNull().forEach { p ->
                                        drawCircle(Color(0xFF173A2A),9.dp.toPx(),p)
                                        drawCircle(Color(0xFFCBF4C3),7.dp.toPx(),p,style=Stroke(2.dp.toPx()))
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                repeat(4) { i ->
                                    Surface(color=if(i<state.count) PlantupiaColors.forest else PlantupiaColors.softSage,
                                        shape=PlantupiaShapes.badge,modifier=Modifier.weight(1f).height(5.dp)) {}
                                }
                            }
                            Text("${state.count} / 4 corners marked",style=PlantupiaText.itemTitle)
                            Text(state.instruction,style=PlantupiaText.body)
                            Button(enabled=state.canMark && state.count<4,onClick={renderer.command(FloorCommand.MARK)},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Mark corner ${(state.count+1).coerceAtMost(4)}") }
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                TextButton(enabled=state.count>0,onClick={renderer.command(FloorCommand.UNDO)}) { Text("Undo corner") }
                                TextButton(enabled=state.count>0,onClick={renderer.command(FloorCommand.RESET)}) { Text("Start again") }
                            }
                            val estimate=state.estimate
                            if(estimate!=null) {
                                Surface(color=PlantupiaColors.softSage,shape=PlantupiaShapes.card,modifier=Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                        Text(String.format(Locale.ROOT,"%.2f × %.2f m",estimate.width,estimate.length),style=PlantupiaText.display)
                                        Text(String.format(Locale.ROOT,"Corner closure discrepancy: %.1f cm",estimate.closure*100),style=PlantupiaText.supporting)
                                        Text("Estimated tracked geometry. This is not an independent accuracy check. Confirm against a physical measurement.",style=PlantupiaText.body)
                                        Button(onClick={renderer.command(FloorCommand.FINISH)},enabled=state.tracking,
                                            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Return estimated dimensions") }
                                    }
                                }
                            }
                            Text("Point at the floor, move slowly, and keep textured surfaces in view. Do not walk while looking only at the screen.",style=PlantupiaText.supporting)
                        }
                        if(error.isNotBlank()) Text(error,style=PlantupiaText.supporting.copy(color=PlantupiaColors.errorText))
                    }
                }
            }
        }
    }

    private fun requestStart() {
        if(checking || started) return
        error=""
        requested=true
        checking=true
        ArCoreApk.getInstance().checkAvailabilityAsync(this) { availability ->
            runOnUiThread {
                if(isFinishing || isDestroyed) return@runOnUiThread
                if(!availability.isSupported) {
                    checking=false; requested=false
                    error="AR support could not be confirmed. Manual room recording remains available."
                } else if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
                    permission.launch(Manifest.permission.CAMERA)
                } else begin()
            }
        }
    }

    private fun begin() {
        if(!requested || !resumed || glRunning || isFinishing) return
        try {
            val current=session ?: run {
                if(ArCoreApk.getInstance().requestInstall(this,!installRequested)==ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    installRequested=true
                    return
                }
                Session(this).also { s ->
                    session=s
                    s.configure(Config(s).apply {
                        val supported = s.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)
                        depthMode = if (supported) Config.DepthMode.RAW_DEPTH_ONLY else Config.DepthMode.DISABLED
                        renderer.depthEnabled = supported
                        planeFindingMode=Config.PlaneFindingMode.HORIZONTAL
                        focusMode=Config.FocusMode.AUTO
                        updateMode=Config.UpdateMode.LATEST_CAMERA_IMAGE
                    })
                }
            }
            current.resume()
            renderer.session=current
            state=FloorFrame()
            started=true;checking=false;glRunning=true
            gl.onResume()
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Exception) {
            stopSession()
            error="AR services, permission or camera could not start. Retry or measure manually."
        }
    }
    override fun onResume() {
        super.onResume()
        resumed=true
        if(requested && ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) begin()
    }
    override fun onPause() {
        resumed=false
        if(glRunning) {
            gl.onPause()
            renderer.clearCommand()
            renderer.session=null
            runCatching { session?.pause() }
            glRunning=false
            state=FloorFrame()
        }
        super.onPause()
    }
    private fun stopSession() {
        if(glRunning) gl.onPause()
        renderer.session=null
        renderer.releaseAnchors()
        runCatching { session?.pause() }
        runCatching { session?.close() }
        session=null
        requested=false;installRequested=false;checking=false;started=false;glRunning=false
        state=FloorFrame()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onDestroy() {
        stopSession()
        super.onDestroy()
    }
}
