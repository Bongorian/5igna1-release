package com.bongorian.signa1

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.BatteryManager
import android.os.Debug
import android.os.PowerManager
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Investigation-only alternatives. None of these shader or preview changes ship in the APK. */
internal object GpuDetailInvestigation {
    private val identity = floatArrayOf(1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f)
    private val mirror = floatArrayOf(-1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 1f,0f,0f,1f)
    private val rotate = floatArrayOf(0f,1f,0f,0f, -1f,0f,0f,0f, 0f,0f,1f,0f, 1f,0f,0f,1f)
    private fun hash(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
    private fun thermal(c: Context): JSONObject {
        val b = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
        return JSONObject().put("status", c.getSystemService(PowerManager::class.java).currentThermalStatus)
            .put("batteryC", b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0)
            .put("plugged", b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
    }
    private fun shader(source: String, lazy: Boolean, shared: Boolean): String {
        val start = source.indexOf("vec3 interpolateCfa(")
        val end = source.indexOf("vec3 toYuv(", start)
        check(start > 0 && end > start)
        var code = """
vec3 interpolateCfa(vec2 pixel,vec2 cell,vec2 offset){
    vec2 phase=mod(pixel+offset,2.);
    vec2 sitePhase=mod(pixel,2.);
    float v=mosaic(pixel,cell);
    float horizontal=(mosaic(pixel+vec2(-1.,0.),cell)+mosaic(pixel+vec2(1.,0.),cell))*.5;
    float vertical=(mosaic(pixel+vec2(0.,-1.),cell)+mosaic(pixel+vec2(0.,1.),cell))*.5;
    ${if (lazy) "if(phase.y<.5&&phase.x>=.5)return vec3(horizontal,v,vertical);\n    if(phase.y>=.5&&phase.x<.5)return vec3(vertical,v,horizontal);" else ""}
    float diagonal=(mosaic(pixel+vec2(-1.,-1.),cell)+mosaic(pixel+vec2(1.,-1.),cell)+mosaic(pixel+vec2(-1.,1.),cell)+mosaic(pixel+vec2(1.,1.),cell))*.25;
    if(phase.y<.5)return phase.x<.5?vec3(v,(horizontal+vertical)*.5,diagonal):vec3(horizontal,v,vertical);
    return phase.x<.5?vec3(vertical,v,horizontal):vec3(diagonal,(horizontal+vertical)*.5,v);
}
vec3 brokenDemosaic(vec2 pixel,vec2 cell,float seed){
    vec2 phase=mod(pixel,2.);
    vec2 sitePhase=phase;
    vec2 direction=vec2(hash(floor(pixel/8.)+seed)<.5?-1.:1.,hash(floor(pixel/8.)+seed+37.)<.5?-1.:1.);
    float v=mosaic(pixel,cell);
    ${if (lazy) """
    if(phase.y<.5&&phase.x<.5)return vec3(v,mosaic(pixel+vec2(direction.x,0.),cell),mosaic(pixel+direction,cell));
    if(phase.y>=.5&&phase.x>=.5)return vec3(mosaic(pixel+direction,cell),mosaic(pixel+vec2(0.,direction.y),cell),v);
    float horizontal=mosaic(pixel+vec2(direction.x,0.),cell),vertical=mosaic(pixel+vec2(0.,direction.y),cell);
    return phase.y<.5?vec3(horizontal,v,vertical):vec3(vertical,v,horizontal);
    """ else """
    float horizontal=mosaic(pixel+vec2(direction.x,0.),cell);
    float vertical=mosaic(pixel+vec2(0.,direction.y),cell),diagonal=mosaic(pixel+direction,cell);
    if(phase.y<.5)return phase.x<.5?vec3(v,horizontal,diagonal):vec3(horizontal,v,vertical);
    return phase.x<.5?vec3(vertical,v,horizontal):vec3(diagonal,vertical,v);
    """}
}

"""
        if (shared) {
            val sites = linkedMapOf("pixel" to "sitePhase", "pixel+vec2(-1.,0.)" to "vec2(1.-sitePhase.x,sitePhase.y)",
                "pixel+vec2(1.,0.)" to "vec2(1.-sitePhase.x,sitePhase.y)", "pixel+vec2(0.,-1.)" to "vec2(sitePhase.x,1.-sitePhase.y)",
                "pixel+vec2(0.,1.)" to "vec2(sitePhase.x,1.-sitePhase.y)", "pixel+vec2(-1.,-1.)" to "1.-sitePhase",
                "pixel+vec2(1.,-1.)" to "1.-sitePhase", "pixel+vec2(-1.,1.)" to "1.-sitePhase", "pixel+vec2(1.,1.)" to "1.-sitePhase",
                "pixel+vec2(direction.x,0.)" to "vec2(1.-sitePhase.x,sitePhase.y)",
                "pixel+vec2(0.,direction.y)" to "vec2(sitePhase.x,1.-sitePhase.y)", "pixel+direction" to "1.-sitePhase")
            for ((position, phase) in sites) code = code.replace("mosaic($position,cell)", "mosaicPhase($position,cell,$phase)")
            code = """
float mosaicPhase(vec2 pixel,vec2 cell,vec2 phase){
    vec3 rgb=sampleAt((pixel+.5)*cell);
    return phase.y<.5?(phase.x<.5?rgb.r:rgb.g):(phase.x<.5?rgb.g:rgb.b);
}
""" + code
        }
        return source.substring(0, start) + code + source.substring(end)
    }
    private fun frame(mask: Int, p: EffectParameters = EffectParameters.defaults(), time: Double = 1.2): EffectState.Frame {
        val base = EffectState.defaults().edit(true, mask, p).amount(.7f).snapshot(true,0)
        val model = FaultModel(9981)
        val config = FaultConfig.defaults().enabled(false).experimental(true)
        model.advance(time, FaultModel.Inputs(), config)
        return model.apply(EffectState.Frame(base.ids(), base.amount, base.parameters, 1234567890, time, base.nodes, true), config)
    }
    private fun upload(target: SignalBuffer, w: Int, h: Int, pattern: Int) {
        target.allocate(w,h)
        val colors = IntArray(w*h) { i -> when (pattern) {
            1 -> if ((i%w+i/w)%2 == 0) -1 else -0x1000000
            2 -> -0x1000000 or (((i*1103515245+12345) ushr 4) and 0xffffff)
            else -> -0x1000000 or (((i%w*13+i/w*3)and 255) shl 16) or (((i/w*7+i%w)and 255)shl 8) or ((i/7)and 255)
        } }
        val bitmap = Bitmap.createBitmap(colors,w,h,Bitmap.Config.ARGB_8888)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,target.texture)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0);bitmap.recycle()
    }
    private fun read(target: SignalBuffer): ByteArray {
        val bytes = ByteBuffer.allocateDirect(target.width*target.height*4)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,target.fbo)
        GLES20.glReadPixels(0,0,target.width,target.height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bytes)
        return ByteArray(bytes.capacity()).also { bytes.rewind();bytes.get(it) }
    }
    private fun diff(a: ByteArray,b: ByteArray): JSONObject {
        check(a.size == b.size)
        var count=0;var max=0
        for (i in a.indices) if(a[i]!=b[i]) { count++;max=maxOf(max,kotlin.math.abs((a[i].toInt()and 255)-(b[i].toInt()and 255))) }
        return JSONObject().put("differentBytes",count).put("maximumChannelError",max)
    }
    fun run(context: Context, view: JSONObject): JSONObject {
        val source = SignalBuffer();val outputs=Array(5){SignalBuffer()};val display=SignalBuffer()
        val original=PhotoRenderer.shaderSource(context)
        val sources=listOf(original,shader(original,true,false),shader(original,false,true),shader(original,true,true), original.replace("(st*vec4(p,0.,1.)).xy", "p"))
        val names=listOf("current","conditional-reads","shared-phase","conditional-and-shared","identity-transform")
        val chains=sources.map{EffectChain(it,false)}
        val blit=EffectChain(original,false)
        val comparisons=JSONArray();val records=JSONArray()
        val clean=frame(0)
        val cfa=1 shl Effects.CFA_ERROR;val demo=1 shl Effects.DEMOSAIC_ERROR
        try {
            // Edges, odd sizes, three fixtures, transforms, phases, seeds, coverage and interpolation scales.
            for ((w,h) in listOf(83 to 61,128 to 96,257 to 193)) for (pattern in 0..2) {
                upload(source,w,h,pattern);outputs.forEach{it.allocate(w,h)}
                for (case in 0..26) {
                    var p=EffectParameters.defaults().reseed(Effects.CFA_ERROR,1001L+case*7919)
                        .reseed(Effects.DEMOSAIC_ERROR,3001L+case*3571)
                        .override(Effects.CFA_ERROR,"cfaPhase",(case%3).toFloat())
                        .override(Effects.CFA_ERROR,"cfaCoverage",floatArrayOf(0f,.37f,1f)[case/3%3])
                        .override(Effects.DEMOSAIC_ERROR,"sampleScale",floatArrayOf(1f,2f,3.5f,8f)[case%4])
                        .override(Effects.DEMOSAIC_ERROR,"interpolationMix",floatArrayOf(0f,.4f,1f)[case/3%3])
                    val f=frame(if(case/9==0)cfa else if(case/9==1)demo else cfa or demo,p,case*.37)
                    val transform=listOf(identity,mirror,rotate)[case%3]
                    for(v in 0..3) chains[v].render(source.texture,false,transform,f,w,h,w,h,outputs[v].fbo,outputs[v].texture)
                    val expected=read(outputs[0])
                    for(v in 1..3) comparisons.put(diff(expected,read(outputs[v])).put("variant",names[v])
                        .put("width",w).put("height",h).put("pattern",pattern).put("case",case))
                }
            }
            val p=EffectParameters.defaults().override(Effects.CFA_ERROR,"cfaCoverage",1f)
                .override(Effects.DEMOSAIC_ERROR,"interpolationMix",1f)
            val workloads=listOf("CFA full coverage" to frame(cfa,p),"DEMOSAIC" to frame(demo,p),
                "CFA + DEMOSAIC" to frame(cfa or demo,p),"All thirteen" to frame(16382,p))
            for ((w,h) in listOf(1280 to 720,1920 to 1080)) {
                upload(source,w,h,0);outputs.forEach{it.allocate(w,h)}
                for((name,f) in workloads) {
                    fun draw(v:Int)=chains[v].render(source.texture,false,identity,f,w,h,w,h,outputs[v].fbo,outputs[v].texture)
                    repeat(5){ for(v in chains.indices){draw(v);GLES20.glFinish()} }
                    for(round in 0..3) {
                        val walls=Array(chains.size){JSONArray()};val cpu=Array(chains.size){JSONArray()}
                        repeat(15){i -> for(pos in chains.indices){val v=(i+round+pos)%chains.size
                            val started=System.nanoTime();val thread=Debug.threadCpuTimeNanos();draw(v)
                            cpu[v].put((Debug.threadCpuTimeNanos()-thread)/1e6);GLES20.glFinish()
                            walls[v].put((System.nanoTime()-started)/1e6)
                        }}
                        val expected=read(outputs[0])
                        for(v in chains.indices)records.put(JSONObject().put("kind","shader").put("name",name).put("variant",names[v])
                            .put("width",w).put("height",h).put("round",round).put("wallMs",walls[v]).put("cpuMs",cpu[v])
                            .put("difference",diff(expected,read(outputs[v]))).put("hash",hash(read(outputs[v]))).put("thermal",thermal(context)))
                    }
                }
            }
            // Same-resolution final-display transfer, separate from transport's internal final copy.
            val w=1920;val h=1080
            upload(source,w,h,0);outputs.forEach{it.allocate(w,h)};display.allocate(w,h)
            for((name,f) in listOf("CLEAN" to clean)+workloads) {
                fun draw(v:Int) {
                    val out=if(v==0)outputs[0] else outputs[1]
                    chains[0].render(source.texture,false,identity,f,w,h,w,h,out.fbo,out.texture)
                    if(v==0)blit.render(out.texture,false,identity,clean,w,h,w,h,display.fbo,display.texture)
                }
                repeat(5){for(v in 0..1){draw(v);GLES20.glFinish()}}
                for(round in 0..3) {
                    val walls=Array(2){JSONArray()};val cpu=Array(2){JSONArray()}
                    repeat(16){i->for(pos in 0..1){val v=(i+round+pos)%2
                        val start=System.nanoTime();val thread=Debug.threadCpuTimeNanos();draw(v)
                        cpu[v].put((Debug.threadCpuTimeNanos()-thread)/1e6);GLES20.glFinish();walls[v].put((System.nanoTime()-start)/1e6)
                    }}
                    val difference=diff(read(display),read(outputs[1]))
                    for(v in 0..1)records.put(JSONObject().put("kind","display-transfer").put("name",name)
                        .put("variant",if(v==0)"render-then-display-copy" else "render-directly")
                        .put("width",w).put("height",h).put("round",round).put("wallMs",walls[v]).put("cpuMs",cpu[v])
                        .put("difference",difference).put("thermal",thermal(context)))
                }
            }
            // User permits a preview capped to the actual view. Source/capture size remains constant.
            val sw=view.getInt("signalWidth").coerceAtLeast(2);val sh=view.getInt("signalHeight").coerceAtLeast(2)
            val vw=view.getInt("viewWidth").coerceAtLeast(2);val vh=view.getInt("viewHeight").coerceAtLeast(2)
            val scale=minOf(1.0,vw.toDouble()/sw,vh.toDouble()/sh)
            val pw=maxOf(2,(sw*scale).toInt());val ph=maxOf(2,(sh*scale).toInt())
            upload(source,sw,sh,0);outputs[0].allocate(sw,sh);outputs[1].allocate(pw,ph);display.allocate(vw,vh)
            for((name,f) in listOf("CLEAN" to clean)+workloads) {
                fun draw(v:Int) {
                    val out=outputs[v]
                    chains[0].render(source.texture,false,identity,f,out.width,out.height,sw,sh,out.fbo,out.texture)
                    blit.render(out.texture,false,identity,clean,vw,vh,out.width,out.height,display.fbo,display.texture)
                }
                repeat(5){for(v in 0..1){draw(v);GLES20.glFinish()}}
                for(round in 0..3) {
                    val walls=Array(2){JSONArray()};val cpu=Array(2){JSONArray()}
                    repeat(16){i->for(pos in 0..1){val v=(i+round+pos)%2
                        val start=System.nanoTime();val thread=Debug.threadCpuTimeNanos();draw(v)
                        cpu[v].put((Debug.threadCpuTimeNanos()-thread)/1e6);GLES20.glFinish();walls[v].put((System.nanoTime()-start)/1e6)
                    }}
                    for(v in 0..1)records.put(JSONObject().put("kind","preview-cap").put("name",name)
                        .put("variant",if(v==0)"source-resolution" else "view-capped")
                        .put("width",outputs[v].width).put("height",outputs[v].height).put("round",round)
                        .put("wallMs",walls[v]).put("cpuMs",cpu[v]).put("thermal",thermal(context)))
                }
            }
            check(GLES20.glGetError()==GLES20.GL_NO_ERROR)
            return JSONObject().put("gpu",GLES20.glGetString(GLES20.GL_RENDERER)).put("view",view)
                .put("shaderHashes",JSONArray(sources.map{hash(it.toByteArray())})).put("variants",JSONArray(names))
                .put("comparisons",comparisons).put("records",records)
        } finally {source.release();outputs.forEach{it.release()};display.release();chains.forEach{it.release()};blit.release()}
    }
}
