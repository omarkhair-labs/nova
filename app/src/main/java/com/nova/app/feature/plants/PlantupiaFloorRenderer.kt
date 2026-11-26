package com.nova.app.feature.plants

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.google.ar.core.Anchor
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal enum class FloorCommand { MARK, UNDO, RESET, FINISH }
internal data class FloorFrame(
    val tracking:Boolean=false,val canMark:Boolean=false,val count:Int=0,
    val instruction:String="Move slowly to find a tracked horizontal floor.",
    val estimate:FloorSurvey?=null,
    val projected:List<Pair<Float,Float>?> = emptyList(),
)

internal class PlantupiaFloorRenderer(
    private val rotation:()->Int,
    private val publish:(FloorFrame)->Unit,
    private val completed:(FloorSurvey, FloatArray)->Unit,
    private val failure:()->Unit,
): GLSurfaceView.Renderer {
    @Volatile var session:Session?=null
    private val pending=AtomicReference<FloorCommand?>(null)
    private val anchors=mutableListOf<Anchor>()
    private var floor:Plane?=null
    private var w=0;private var h=0
    private var texture=0;private var program=0
    private var lastUpdate=0L
    private var failed=false
    private val vertices=buffer(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f))
    private val uv=buffer(FloatArray(8))

    fun command(value:FloorCommand) { pending.compareAndSet(null,value) }
    fun clearCommand() { pending.set(null) }
    /** Caller pauses the GL thread before release. */
    fun releaseAnchors() {
        anchors.forEach { runCatching { it.detach() } }
        anchors.clear();floor=null;clearCommand();failed=false
    }
    private fun root(plane:Plane):Plane {
        var p=plane
        repeat(16) { p=p.subsumedBy ?: return p }
        return p
    }
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?) {
        try {
            GLES20.glClearColor(.07f,.12f,.09f,1f)
            val ids=IntArray(1)
            GLES20.glGenTextures(1,ids,0);texture=ids[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
            val vs=shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 aPosition; attribute vec2 aUv; varying vec2 vUv; void main(){gl_Position=vec4(aPosition,0.0,1.0);vUv=aUv;}")
            val fs=shader(GLES20.GL_FRAGMENT_SHADER,"#extension GL_OES_EGL_image_external : require\nprecision mediump float; uniform samplerExternalOES uTexture; varying vec2 vUv; void main(){gl_FragColor=texture2D(uTexture,vUv);}")
            program=GLES20.glCreateProgram()
            GLES20.glAttachShader(program,vs);GLES20.glAttachShader(program,fs);GLES20.glLinkProgram(program)
            val ok=IntArray(1);GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0)
            GLES20.glDeleteShader(vs);GLES20.glDeleteShader(fs)
            check(ok[0]!=0)
        } catch(_:Exception) { program=0;failed=true;failure() }
    }
    override fun onSurfaceChanged(gl:GL10?,width:Int,height:Int) {
        w=width;h=height;GLES20.glViewport(0,0,width,height)
    }
    override fun onDrawFrame(gl:GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val s=session ?: return
        if(failed || w<=0 || h<=0 || program==0) return
        try {
            s.setDisplayGeometry(rotation(),w,h)
            s.setCameraTextureName(texture)
            val frame=s.update()
            if(frame.timestamp!=0L) {
                vertices.position(0);uv.position(0)
                frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                    vertices,Coordinates2d.TEXTURE_NORMALIZED,uv)
                GLES20.glDisable(GLES20.GL_DEPTH_TEST)
                GLES20.glUseProgram(program)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture)
                GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uTexture"),0)
                val a=GLES20.glGetAttribLocation(program,"aPosition");val b=GLES20.glGetAttribLocation(program,"aUv")
                vertices.position(0);uv.position(0)
                GLES20.glEnableVertexAttribArray(a);GLES20.glEnableVertexAttribArray(b)
                GLES20.glVertexAttribPointer(a,2,GLES20.GL_FLOAT,false,0,vertices)
                GLES20.glVertexAttribPointer(b,2,GLES20.GL_FLOAT,false,0,uv)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
                GLES20.glDisableVertexAttribArray(a);GLES20.glDisableVertexAttribArray(b)
            }
            val now=SystemClock.elapsedRealtime()
            val cmd=pending.getAndSet(null)
            if(cmd==null && now-lastUpdate<100) return
            lastUpdate=now
            if(cmd==FloorCommand.RESET) releaseAnchors()
            if(cmd==FloorCommand.UNDO && anchors.isNotEmpty()) {
                anchors.removeAt(anchors.lastIndex).detach()
                if(anchors.isEmpty()) floor=null
            }
            val tracking=frame.camera.trackingState==TrackingState.TRACKING
            val eligible=tracking && anchors.all { it.trackingState==TrackingState.TRACKING }
            val hit=if(eligible && anchors.size<4) frame.hitTest(w/2f,h/2f).firstOrNull { h ->
                val plane=h.trackable as? Plane
                plane!=null && plane.type==Plane.Type.HORIZONTAL_UPWARD_FACING &&
                    plane.trackingState==TrackingState.TRACKING && plane.isPoseInPolygon(h.hitPose) &&
                    (floor==null || root(plane)==root(floor!!))
            } else null
            if(cmd==FloorCommand.MARK && hit!=null && anchors.size<4) {
                val candidate=hit.hitPose.translation
                val distant=anchors.all { a ->
                    val p=a.pose.translation
                    kotlin.math.hypot((p[0]-candidate[0]).toDouble(),(p[2]-candidate[2]).toDouble())>=.3
                }
                if(distant) {
                    if(floor==null) floor=hit.trackable as Plane
                    anchors.add(hit.createAnchor())
                }
            }
            val allTracking=tracking && anchors.all { it.trackingState==TrackingState.TRACKING }
            val estimate=if(allTracking && floor?.let{root(it).trackingState==TrackingState.TRACKING}==true)
                estimateRectangularFloor(anchors.map { a -> a.pose.translation.let {
                    FloorMark(it[0].toDouble(),it[1].toDouble(),it[2].toDouble())
                }}) else null
            if (cmd == FloorCommand.FINISH && estimate != null) {
                // Snapshot one tracked coordinate system, in the first corner anchor's frame.
                // A device can return a valid floor measurement with no usable feature cloud.
                val features = try {
                    val origin = anchors.first().pose.inverse()
                    frame.acquirePointCloud().use { cloud ->
                        val source = cloud.points
                        val result = FloatArray(minOf(source.remaining() / 4, 1500) * 3)
                        var count = 0
                        while (source.remaining() >= 4 && count < result.size) {
                            val x = source.get()
                            val y = source.get()
                            val z = source.get()
                            val confidence = source.get()
                            if (confidence < .5f || !confidence.isFinite() ||
                                !x.isFinite() || !y.isFinite() || !z.isFinite()) continue
                            val local = origin.transformPoint(floatArrayOf(x, y, z))
                            local.copyInto(result, count)
                            count += 3
                        }
                        result.copyOf(count)
                    }
                } catch (_: com.google.ar.core.exceptions.NotYetAvailableException) {
                    floatArrayOf()
                }
                completed(estimate, features)
                return
            }
            val projected=if(tracking) {
                val projection=FloatArray(16);val view=FloatArray(16);val combined=FloatArray(16)
                frame.camera.getProjectionMatrix(projection,0,.05f,100f)
                frame.camera.getViewMatrix(view,0);Matrix.multiplyMM(combined,0,projection,0,view,0)
                anchors.map { a ->
                    val v=a.pose.translation;val out=FloatArray(4)
                    Matrix.multiplyMV(out,0,combined,0,floatArrayOf(v[0],v[1],v[2],1f),0)
                    if(a.trackingState!=TrackingState.TRACKING || out[3]<=.01f) null else {
                        val x=(out[0]/out[3]+1f)/2f;val y=(1f-out[1]/out[3])/2f
                        if(x in 0f..1f && y in 0f..1f) x to y else null
                    }
                }
            } else emptyList()
            val instruction=when {
                !allTracking -> "Tracking paused. Move slowly toward textured surfaces; do not add or accept measurements yet."
                anchors.size==4 && estimate==null -> "The four marks do not agree with one rectangle. Undo or start again."
                estimate!=null -> "Four tracked corners agree within the consistency limits."
                hit==null -> "Aim at the same tracked floor. Move slowly until the reticle is ready."
                else -> "Aim at corner ${anchors.size+1} on the floor, then mark it."
            }
            publish(FloorFrame(allTracking,eligible && hit!=null && anchors.size<4,anchors.size,instruction,estimate,projected))
        } catch(_:Exception) {
            failed=true;failure()
        }
    }
    private fun buffer(values:FloatArray):FloatBuffer=ByteBuffer.allocateDirect(values.size*4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values);position(0) }
    private fun shader(type:Int,source:String):Int {
        val id=GLES20.glCreateShader(type);GLES20.glShaderSource(id,source);GLES20.glCompileShader(id)
        val ok=IntArray(1);GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,ok,0);check(ok[0]!=0)
        return id
    }
}
