package com.bongorian.signa1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.File
import org.json.JSONObject

/** Synthetic evidence, independent of the current camera scene or physical device inputs. */
internal object ExperimentalSignalChecks {
    fun run(context: Context): String {
        val source = Bitmap.createBitmap(192, 160, Bitmap.Config.ARGB_8888)
        for (y in 0 until source.height) for (x in 0 until source.width) {
            val v = if (x in 90..102 && y in 74..86) 255 else if (x < 64) (if (x % 8 < 4) 50 else 170) else 24
            source.setPixel(x, y, Color.rgb(v, v, v))
        }
        val jpeg = ByteArrayOutputStream().apply { source.compress(Bitmap.CompressFormat.JPEG, 100, this) }.toByteArray()
        source.recycle()
        val cfg = FaultConfig(true, true, true, true, true, true, .5f, 50).experimental(true)
        val input = FaultModel.Inputs().apply {
            motionAvailable = true; timingAvailable = true; angularSpeed = 4f; ax = 8f
            heat = .9f; cpu = .7f; jitter = .5f; exposureNs = 30_000_000; skewNs = 20_000_000
        }
        val model = FaultModel(77)
        repeat(300) { i -> input.sensorNs = 1L + i * 16_666_667; model.advance(i / 60.0, input, cfg) }
        fun render(state: EffectState, config: FaultConfig = cfg) = requireNotNull(PhotoRenderer.render(context, jpeg, false, model.apply(state.snapshot(false, 0), config)))
        val clean = render(EffectState.defaults())
        val directory = File(context.filesDir, "verification").also { it.mkdirs() }
        fun save(name: String, bitmap: Bitmap) { File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        save("experimental-source.png", clean)
        val report = JSONObject()
        try {
            for (id in intArrayOf(Effects.MOTION_BLUR, Effects.THERMAL_NOISE, Effects.SMEAR)) {
                val state = EffectState.defaults().single(id).amount(.8f)
                val image = render(state)
                val repeat = render(state)
                val bypass = render(state, cfg.experimental(false))
                try {
                    check(image.sameAs(repeat)) { "Non-deterministic artifact ${Effects.name(id)}" }
                    check(bypass.sameAs(clean)) { "Experiment OFF changed pixels" }
                    var changed = 0
                    var outsideHighlight = 0
                    for (y in 0 until image.height) for (x in 0 until image.width) {
                        if (image.getPixel(x, y) != clean.getPixel(x, y)) changed++
                        if (x in 92..100 && (y in 50..70 || y in 90..110) && Color.red(image.getPixel(x, y)) > Color.red(clean.getPixel(x, y))) outsideHighlight++
                    }
                    check(changed > 100) { "No visible artifact ${Effects.name(id)}: $changed pixels" }
                    if (id == Effects.SMEAR) check(outsideHighlight > 20) { "Smear does not extend highlights" }
                    report.put(Effects.name(id), JSONObject().put("changedPixels", changed).put("highlightTrailPixels", outsideHighlight))
                    save("experimental-${id}.png", image)
                } finally { image.recycle(); repeat.recycle(); bypass.recycle() }
            }
            File(directory, "experimental-gpu.json").writeText(report.toString(2))
        } finally { clean.recycle() }
        return report.toString()
    }
}
