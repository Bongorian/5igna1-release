package com.bongorian.signa1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.File

internal object AnalogFpvChecks {
    fun run(context: Context): String {
        val id = Effects.STREAM_ERROR
        val source = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        for (y in 0 until 480) for (x in 0 until 640)
            source.setPixel(x, y, if (x < 320) Color.rgb(110, 110, 110)
                else Color.rgb((x * 255 / 640), y * 255 / 480, if (x % 32 < 16) 180 else 50))
        val jpeg = ByteArrayOutputStream().apply { source.compress(Bitmap.CompressFormat.JPEG, 100, this) }.toByteArray()
        source.recycle()
        val cfg = FaultConfig.defaults()
        val model = FaultModel()
        model.advance(0.0, FaultModel.Inputs(), cfg)
        model.advance(2.0, FaultModel.Inputs(), cfg)
        val directory = File(context.filesDir, "verification").also { it.mkdirs() }
        fun state(p: EffectParameters) = EffectState.defaults().single(id).amount(1f).edit(false, 1 shl id, p)
        fun render(s: EffectState) = requireNotNull(PhotoRenderer.render(context, jpeg, false, model.apply(s.snapshot(false, 0), cfg)))
        fun save(name: String, image: Bitmap) = File(directory, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val p = EffectParameters.defaults().with(id, "streamModel", 1f)
        val clean = render(EffectState.defaults())
        val neutral = render(state(p.with(id, "fpvQuality", 1f).with(id, "fpvInterference", 0f)))
        val image = render(state(p))
        val replay = render(state(p))
        val band = render(state(p.override(id, "fpvNoise", 0f).override(id, "fpvBurst", .9f)
            .override(id, "fpvBandCenter", .75f).override(id, "fpvBandWidth", .2f)
            .override(id, "fpvShift", 0f).override(id, "fpvSoftness", 0f)))
        val field = p.override(id, "fpvNoise", .4f).override(id, "fpvBurst", 0f)
            .override(id, "fpvShift", 0f).override(id, "fpvSoftness", 0f)
            .override(id, "fpvChroma", 1f)
        val fine = render(state(field.with(id, "fpvColorSize", 0f)))
        val broad = render(state(field.with(id, "fpvColorSize", 1f)))
        val moved = render(state(field.with(id, "fpvColorSize", 1f).override(id, "fpvColorX", -.22f)))
        fun chromaRoughness(bitmap: Bitmap): Long {
            var sum = 0L
            for (y in 20 until 460) for (x in 20 until 298) {
                fun chroma(px: Int) = Color.red(px) - Color.blue(px)
                sum += kotlin.math.abs(chroma(bitmap.getPixel(x,y)) - chroma(bitmap.getPixel(x+1,y)))
            }
            return sum
        }
        try {
            check(chromaRoughness(broad) * 3 < chromaRoughness(fine)) { "Color size did not form larger patches" }
            check(!broad.sameAs(moved)) { "Large color patches do not move" }
            save("fpv-color-fine.png", fine); save("fpv-color-broad.png", broad); save("fpv-color-moved.png", moved)
            check(clean.sameAs(neutral)) { "Neutral FPV changed output" }
            check(image.sameAs(replay)) { "FPV snapshot replay differs" }
            check(!image.sameAs(clean)) { "FPV produced no artifact" }
            var center = 0L; var outside = 0L
            for (y in 0 until 480) for (x in 0 until 300) {
                val delta = kotlin.math.abs(Color.red(band.getPixel(x,y)) - Color.red(clean.getPixel(x,y)))
                // GPU output orientation is verified by checking both symmetric band locations.
                if (y in 96..143 || y in 336..383) center += delta
                if (y in 216..263 || y in 0..47) outside += delta
            }
            check(center > outside * 4 + 1000) { "Interference is not localized: $center / $outside" }
            save("fpv-source.png", clean); save("fpv-default.png", image); save("fpv-band.png", band)
            return "PASS neutral, deterministic replay, visible default, large moving color patches, localized band ($center / $outside)"
        } finally { listOf(clean,neutral,image,replay,band,fine,broad,moved).forEach { it.recycle() } }
    }
}
