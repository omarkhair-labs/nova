package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Authored visual studies. Decorative only: never evidence of a specimen, person or SKU. */
@Composable
internal fun PlantupiaEditionArt(kind: String, modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val u = size.minDimension / 120f
        val ox = (size.width - 120f*u)/2f
        val oy = (size.height - 120f*u)/2f
        fun p(x:Float,y:Float)=Offset(ox+x*u,oy+y*u)
        val dark=Color(0xFF123B2E)
        val leaf=Color(0xFF83A578)
        val pale=Color(0xFFDCE6BB)
        val clay=Color(0xFFBC8A69)
        val warm=Color(0xFFEDE3D0)
        fun line(x1:Float,y1:Float,x2:Float,y2:Float,c:Color,w:Float=1f) =
            drawLine(c,p(x1,y1),p(x2,y2),w*u)
        fun disc(x:Float,y:Float,r:Float,c:Color,outline:Boolean=false) {
            if(outline) drawCircle(c,r*u,p(x,y),style=Stroke(.9f*u))
            else drawCircle(c,r*u,p(x,y))
        }
        fun blade(x:Float,y:Float,tx:Float,ty:Float,bend:Float,c:Color) {
            val dx=tx-x; val dy=ty-y
            val a=p(x,y); val b=p(tx,ty)
            val k=p(x+dx*.4f-dy*bend,y+dy*.4f+dx*bend)
            val l=p(x+dx*.65f+dy*bend,y+dy*.65f-dx*bend)
            val path=Path().apply {
                moveTo(a.x,a.y); quadraticBezierTo(k.x,k.y,b.x,b.y)
                quadraticBezierTo(l.x,l.y,a.x,a.y); close()
            }
            drawPath(path,c);line(x,y,tx,ty,dark.copy(alpha=.5f),.7f)
        }
        when(kind) {
            "day" -> {
                for(r in listOf(53f,43f,31f)) disc(60f,60f,r,pale.copy(alpha=.45f),true)
                disc(83f,29f,12f,clay)
                line(15f,92f,105f,92f,pale)
                line(56f,99f,59f,22f,pale,1.5f)
                blade(57f,75f,24f,50f,.6f,leaf)
                blade(58f,59f,91f,44f,.6f,pale)
                blade(58f,45f,35f,19f,.6f,leaf)
                blade(59f,34f,73f,10f,.45f,pale)
            }
            "circle" -> {
                disc(44f,50f,35f,warm);disc(78f,71f,35f,leaf)
                disc(44f,50f,35f,dark,true);disc(78f,71f,35f,dark,true)
                disc(40f,47f,7f,dark);disc(80f,66f,7f,pale)
                line(22f,63f,53f,63f,dark);line(66f,84f,95f,84f,dark)
            }
            "material" -> {
                drawRect(warm,topLeft=p(0f,0f),size=Size(120*u,120*u))
                val pot=Path().apply{
                    val a=p(25f,39f);moveTo(a.x,a.y)
                    val b=p(95f,39f);lineTo(b.x,b.y)
                    val c=p(83f,96f);lineTo(c.x,c.y)
                    val d=p(37f,96f);lineTo(d.x,d.y);close()
                }
                drawPath(pot,clay)
                drawOval(dark,topLeft=p(25f,32f),size=Size(70*u,14*u))
                for(i in 0..8) line(28f+i*8f,45f,39f+i*5.2f,94f,warm.copy(alpha=.65f),.8f)
                line(10f,105f,110f,105f,dark);line(15f,14f,15f,101f,dark.copy(alpha=.4f))
            }
            "identity" -> {
                disc(60f,60f,52f,warm);disc(60f,60f,45f,dark,true);disc(60f,60f,38f,leaf,true)
                line(60f,96f,60f,24f,dark,1.5f)
                blade(60f,79f,32f,50f,.6f,leaf);blade(60f,62f,89f,39f,.55f,leaf)
                blade(60f,42f,49f,21f,.5f,dark)
            }
            else -> {
                val book=Path().apply {
                    val a=p(12f,24f);moveTo(a.x,a.y)
                    for(q in listOf(p(48f,24f),p(60f,34f),p(72f,24f),p(108f,24f),p(108f,93f),p(72f,93f),p(60f,103f),p(48f,93f),p(12f,93f))) lineTo(q.x,q.y)
                    close()
                }
                drawPath(book,warm);line(60f,35f,60f,102f,clay,1.4f)
                for(i in 0..4) line(72f,44f+i*9f,96f,44f+i*9f,dark.copy(alpha=.5f),.8f)
                line(35f,83f,36f,37f,dark)
                blade(35f,73f,19f,53f,.5f,leaf);blade(36f,58f,52f,42f,.55f,leaf)
            }
        }
    }
}
