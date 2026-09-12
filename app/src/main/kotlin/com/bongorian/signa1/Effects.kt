package com.bongorian.signa1

import java.util.Arrays
import java.util.function.IntPredicate
import java.util.stream.IntStream
import kotlin.math.max
import kotlin.math.min

/** Owns fault IDs and causal order. IDs are deliberately incompatible with release 1.0. */
internal object Effects {
    const val CLEAN: Int = 0
    const val PIXEL_DAMAGE: Int = 1
    const val EXPOSURE: Int = 2
    const val ROW_ERROR: Int = 3
    const val BIT_ERROR: Int = 4
    const val ADDRESS_ERROR: Int = 5
    const val CFA_ERROR: Int = 6
    const val DEMOSAIC_ERROR: Int = 7
    const val CHROMA_ERROR: Int = 8
    const val COLOR_MAP: Int = 9
    const val BLOCK_ERROR: Int = 10
    const val STREAM_ERROR: Int = 11
    const val VHS: Int = 12
    const val CRT: Int = 13
    const val MOTION_BLUR = 14
    const val THERMAL_NOISE = 15
    const val SMEAR = 16
    val NAMES: Array<String> =
        arrayOf<String>(
            "CLEAN",
            "PIXEL DAMAGE",
            "EXPOSURE",
            "ROW ERROR",
            "BIT ERROR",
            "ADDRESS ERROR",
            "CFA ERROR",
            "DEMOSAIC ERROR",
            "CHROMA ERROR",
            "COLOR MAP",
            "BLOCK ERROR",
            "STREAM ERROR",
            "VHS",
            "CRT",
            "MOTION BLUR",
            "THERMAL NOISE",
            "SMEAR",
        )
    val ORDER: IntArray = intArrayOf(0, MOTION_BLUR, THERMAL_NOISE, PIXEL_DAMAGE, EXPOSURE, SMEAR, ROW_ERROR, BIT_ERROR, ADDRESS_ERROR, CFA_ERROR, DEMOSAIC_ERROR, CHROMA_ERROR, COLOR_MAP, BLOCK_ERROR, STREAM_ERROR, VHS, CRT)

    fun rank(id: Int): Int = ORDER.indexOf(id)

    private fun c(key: String, initial: Float): Control {
        return Control(key, initial)
    }

    // Compact artistic controls, not a fixed renderer ABI. Each fault can add its own controls.
    val CONTROLS: Array<Array<Control>> =
        arrayOf<Array<Control>>(
            arrayOf<Control>(),
            arrayOf<Control>(c("density", .5f), c("hot", .5f), c("columns", .2f)),
            arrayOf<Control>(c("depth", .6f), c("rate", .35f), c("bands", .4f)),
            arrayOf<Control>(
                c("displacement", .5f),
                c("bands", .4f),
                c("loss", .35f),
                c("concealment", .7f),
            ),
            arrayOf<Control>(c("activity", .5f), c("bit", .6f), c("burst_size", .4f)),
            arrayOf<Control>(c("offset", .35f), c("region", .4f), c("activity", .6f)),
            arrayOf<Control>(c("coverage", .55f), c("phase", 0f), c("region", .4f)),
            arrayOf<Control>(c("interpolation", .65f), c("sampling", .4f)),
            arrayOf<Control>(c("separation", .45f), c("sampling", .35f), c("direction", 0f)),
            arrayOf<Control>(c("palette", .5f), c("cycles", .4f), c("mix", .8f)),
            arrayOf<Control>(c("quantization", .5f), c("block_size", .45f), c("misaddress", .4f)),
            arrayOf<Control>(c("loss", .5f), c("region", .4f), c("concealment", .7f)),
            arrayOf<Control>(
                c("transport", 0f), c("reduce", 0f), c("cable", 0f),
                c("bandwidth", .6f),
                c("tracking", .5f),
                c("dropout", .4f),
                c("noise", .25f),
            ),
            arrayOf<Control>(
                c("transport", 0f), c("upconvert", 1f), c("ledRate", .2f),
                c("networkInterval", 3f / 28), c("networkDuration", 6f / 14),
                c("networkRate", 11f / 29), c("networkResolution", .5f),
                c("scan", .5f),
                c("phosphor", 0f),
                c("convergence", .4f),
                c("sync", .4f),
            ),
            arrayOf(c("amount", .6f), c("direction", .5f), c("floor", 0f)),
            arrayOf(c("amount", .6f), c("grain", .2f), c("floor", 0f)),
            arrayOf(c("amount", .6f), c("length", .6f), c("threshold", .75f)),
        )

    fun label(id: Int) = if (id == VHS) "MEDIA" else if (id == CRT) "DISPLAY" else name(id)

    fun physical(id: Int) = id >= MOTION_BLUR && id <= SMEAR

    fun point(id: Int): Point {
        if (id == MOTION_BLUR || id == THERMAL_NOISE) return Point.SENSOR
        if (id == SMEAR) return Point.READOUT
        if (id <= EXPOSURE) return Point.SENSOR
        if (id == ROW_ERROR) return Point.READOUT
        if (id <= ADDRESS_ERROR) return Point.DATA
        if (id <= DEMOSAIC_ERROR) return Point.RECONSTRUCTION
        if (id <= COLOR_MAP) return Point.COLOR
        if (id <= STREAM_ERROR) return Point.STREAM
        return if (id == VHS) Point.MEDIA else Point.DISPLAY
    }

    fun stage(id: Int): String {
        val names =
            arrayOf<String>(
                "01 / SENSOR",
                "02 / READOUT",
                "03 / DATA",
                "04 / CFA / RECONSTRUCTION",
                "05 / COLOR",
                "06 / CODEC / STREAM",
                "07 / MEDIA",
                "08 / DISPLAY",
            )
        return names[point(id).ordinal]
    }

    fun raw(id: Int): Boolean {
        return id >= PIXEL_DAMAGE && id <= CFA_ERROR || physical(id)
    }

    fun available(id: Int, video: Boolean, rawOnly: Boolean): Boolean {
        return id >= 0 && id < NAMES.size && (!rawOnly || id == CLEAN || raw(id))
    }

    fun choices(video: Boolean, rawOnly: Boolean): IntArray {
        return Arrays.stream(ORDER)
            .filter(IntPredicate { id: Int -> available(id, video, rawOnly) })
            .toArray()
    }

    fun ordered(mask: Int, rawOnly: Boolean): IntArray {
        return Arrays.stream(ORDER)
            .filter(
                IntPredicate { id: Int ->
                    id != CLEAN && (mask and (1 shl id)) != 0 && (!rawOnly || raw(id))
                }
            )
            .toArray()
    }

    fun shaderDefines(): String {
        val s = StringBuilder()
        for (id in ORDER) s.append("#define FX_")
            .append((if (id == VHS) "VHS" else if (id == CRT) "CRT" else NAMES[id]).replace(' ', '_'))
            .append(' ')
            .append(id)
            .append('\n')
        return s.toString()
    }

    fun chainName(ids: IntArray): String {
        if (ids.size == 0) return NAMES[CLEAN]
        val s = StringBuilder()
        for (id in ids) {
            if (s.length > 0) s.append(" → ")
            s.append(name(id))
        }
        return s.toString()
    }

    fun name(id: Int): String {
        return NAMES[max(0, min(NAMES.size - 1, id))]
    }

    internal enum class Point {
        SENSOR,
        READOUT,
        DATA,
        RECONSTRUCTION,
        COLOR,
        STREAM,
        MEDIA,
        DISPLAY,
    }

    internal class Control(val key: String, val initial: Float)
}
