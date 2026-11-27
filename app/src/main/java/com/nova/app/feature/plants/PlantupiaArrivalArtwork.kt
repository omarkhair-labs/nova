package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * An original architectural botanical plate for arrival and first place.
 * Purely decorative, not a photograph, floor plan or representation of a user's
 * plant. No network or bitmap assets. Scales in its own coordinate system.
 */
@Composable
internal fun PlantupiaArrivalArtwork(modifier: Modifier = Modifier, light: Boolean = false) {
    val wall = if (light) Color(0xFFE7E7D9) else Color(0xFF234C3E)
    val floor = if (light) Color(0xFFD1C8AE) else Color(0xFF13352B)
    val recess = if (light) Color(0xFFD2DCC7) else Color(0xFF376454)
    val sun = Color(0xFFE4C88B)
    val darkLeaf = Color(0xFF527954)
    val paleLeaf = Color(0xFFB7CDA2)
    val vein = Color(0xFFE5E6BF)
    Canvas(modifier.fillMaxWidth().aspectRatio(1.83f).clip(RoundedCornerShape(12.dp))
        .clearAndSetSemantics {}) {
        fun p(x:Float,y:Float)=Offset(size.width*x,size.height*y)
        fun line(x1:Float,y1:Float,x2:Float,y2:Float,color:Color,width:Float=.0025f) =
            drawLine(color,p(x1,y1),p(x2,y2),size.height*width,StrokeCap.Round)
        fun path(block: Path.()->Unit)=Path().apply(block)
        fun Path.m(x:Float,y:Float)=moveTo(size.width*x,size.height*y)
        fun Path.l(x:Float,y:Float)=lineTo(size.width*x,size.height*y)
        fun Path.c(x1:Float,y1:Float,x2:Float,y2:Float,x3:Float,y3:Float)=
            cubicTo(size.width*x1,size.height*y1,size.width*x2,size.height*y2,size.width*x3,size.height*y3)
        drawRect(Brush.verticalGradient(listOf(wall,if(light) Color(0xFFF2EEE0) else Color(0xFF1B4034))))
        drawRect(floor,p(0f,.81f),Size(size.width,size.height*.19f))
        val arch=path {
            m(.095f,.81f);l(.095f,.32f);c(.095f,.02f,.49f,.02f,.49f,.32f);l(.49f,.81f);close()
        }
        drawPath(arch,recess)
        drawPath(arch,if(light) Color(0xFF8D9D87) else paleLeaf.copy(alpha=.38f),style=Stroke(size.height*.004f))
        drawCircle(sun,size.height*.125f,p(.30f,.30f))
        val beam=path {m(.19f,.41f);l(.39f,.41f);l(.79f,.81f);l(.98f,1f);l(.69f,1f);l(.43f,.81f);close()}
        drawPath(beam,sun.copy(alpha=if(light) .38f else .18f))
        // A restrained architectural grid establishes scale without pretending to measure.
        for (i in 0..3) {
            val x=.04f+i*.24f
            line(x,.81f,x+.10f,1f,vein.copy(alpha=.16f))
        }
        line(0f,.81f,1f,.81f,vein.copy(alpha=.30f))
        line(.292f,.18f,.292f,.81f,vein.copy(alpha=.3f))
        line(.095f,.47f,.49f,.47f,vein.copy(alpha=.3f))
        // Four leaves at different orientations, with individually shaped
        // midribs, asymmetric silhouettes and branching veins.
        val stem=path {m(.675f,.80f);c(.68f,.63f,.61f,.37f,.70f,.12f)}
        drawPath(stem,vein,style=Stroke(size.height*.009f,cap=StrokeCap.Round))
        data class Leaf(val base:Offset,val tip:Offset,val bow:Float,val width:Float,val color:Color)
        val leaves=listOf(
            Leaf(Offset(.664f,.53f),Offset(.445f,.32f),-.085f,.102f,darkLeaf),
            Leaf(Offset(.657f,.43f),Offset(.893f,.26f),.067f,.091f,paleLeaf),
            Leaf(Offset(.672f,.28f),Offset(.555f,.095f),-.074f,.075f,Color(0xFF86A978)),
            Leaf(Offset(.685f,.20f),Offset(.808f,.065f),.073f,.067f,paleLeaf),
            Leaf(Offset(.673f,.67f),Offset(.90f,.48f),.088f,.105f,Color(0xFF75986D))
        )
        leaves.forEach { leaf ->
            val b=leaf.base;val t=leaf.tip
            val dx=t.x-b.x; val dy=t.y-b.y
            val normal=if(dx>=0f) 1f else -1f
            val mid=Offset(b.x+dx*.52f,b.y+dy*.52f)
            val outline=path {
                m(b.x,b.y)
                c(b.x+dx*.18f,b.y+dy*.18f-leaf.width,
                    t.x-dx*.16f,t.y-leaf.width*.72f,t.x,t.y)
                c(t.x-dx*.20f,t.y+leaf.width*.72f,
                    b.x+dx*.16f,b.y+dy*.20f+leaf.width,b.x,b.y)
                close()
            }
            drawPath(outline,leaf.color)
            drawPath(outline,vein.copy(alpha=.22f),style=Stroke(size.height*.002f))
            val rib=path{m(b.x,b.y);c(mid.x,mid.y+leaf.bow*.24f,t.x-dx*.19f,t.y,t.x,t.y)}
            drawPath(rib,vein.copy(alpha=.82f),style=Stroke(size.height*.003f,cap=StrokeCap.Round))
            for(i in 1..4){
                val f=i/5f; val x=b.x+dx*f;val y=b.y+dy*f
                val spread=leaf.width*(1f-kotlin.math.abs(f-.5f)*1.7f)*.72f
                line(x,y,x+normal*.025f,y-spread,vein.copy(alpha=.52f),.0018f)
                line(x,y,x+normal*.032f,y+spread,vein.copy(alpha=.52f),.0018f)
            }
        }
        val pot=path {m(.586f,.766f);l(.763f,.766f);l(.735f,.94f);l(.614f,.94f);close()}
        drawPath(pot,Brush.horizontalGradient(listOf(Color(0xFFB17A57),Color(0xFFD9A37A),Color(0xFFAD6D4E)),size.width*.58f,size.width*.77f))
        drawLine(Color(0xFF784F3D),p(.586f,.766f),p(.763f,.766f),size.height*.012f,StrokeCap.Round)
        for(i in 1..5){
            val f=i/6f
            line(.586f+.177f*f,.79f,.614f+.121f*f,.93f,Color(0xFF80563D).copy(alpha=.28f))
        }
        line(.53f,.95f,.82f,.95f,vein.copy(alpha=.38f))
    }
}
