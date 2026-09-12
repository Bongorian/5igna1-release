package com.bongorian.signa1

import java.util.Collections

/** Complete editable catalog of compiled fault values and their time/event generators. */
internal object FaultParameters {
    private val CATALOG: MutableMap<Int, MutableList<Spec>> =
        LinkedHashMap<Int, MutableList<Spec>>()

    init {
        for (id in Effects.ORDER) if (id != 0) {
            val p: MutableList<Spec> = ArrayList<Spec>()
            CATALOG.put(id, p)
            for (key in FaultSensitivity.keys) add(p, Group.INPUT, key, 0, 4, .01f)
            add(
                p,
                Group.TIME,
                "timeScale",
                -4,
                4,
                .01f,
                "timeOffset",
                -3600,
                3600,
                .1f,
                "time",
                -86400,
                86400,
                .01f,
                "driftSpeed",
                0,
                10,
                .01f,
                "drift",
                -1,
                1,
                .001f,
                "phaseSpeed",
                -40,
                40,
                .01f,
                "phase",
                -6.283186f,
                6.283186f,
                .001f,
            )
            add(p, Group.PROFILE, "identityBias", -1, 1, .001f)
            if (incidents(id))
                add(
                    p,
                    Group.EVENT,
                    "eventPhase", 0, 1, .001f,
                    "eventPeriod",
                    if (id == Effects.CRT) 0f else .03f,
                    60,
                    .01f,
                    "eventDuration",
                    .005f,
                    60,
                    .005f,
                    "eventProbability",
                    0,
                    1,
                    .001f,
                    "eventSerial",
                    -1000000,
                    1000000,
                    1,
                    "eventEnvelope",
                    0,
                    1,
                    .001f,
                    "eventPosition",
                    0,
                    1,
                    .001f,
                    "eventPattern",
                    0,
                    997,
                    .1f,
                )
            add(p, Group.SIGNAL, "identitySeed", 0, 997, .1f, "eventSeed", 0, 997, .1f)
            when (id) {
                Effects.PIXEL_DAMAGE ->
                    add(
                        p,
                        Group.SIGNAL,
                        "pixelDensity",
                        0,
                        1,
                        .001f,
                        "columnDensity",
                        0,
                        1,
                        .001f,
                        "hotFraction",
                        0,
                        1,
                        .001f,
                        "hotValue",
                        0,
                        1,
                        .001f,
                        "sensorNoise",
                        0,
                        1,
                        .001f,
                        "grainSeed",
                        0,
                        997,
                        .1f,
                    )

                Effects.EXPOSURE ->
                    add(
                        p,
                        Group.SIGNAL,
                        "exposureDepth",
                        0,
                        2,
                        .001f,
                        "exposurePhase",
                        -6.283186f,
                        6.283186f,
                        .001f,
                        "scanPhase",
                        0,
                        2000,
                        .1f,
                        "integration",
                        -1,
                        1,
                        .001f,
                    )

                Effects.ROW_ERROR ->
                    add(
                        p,
                        Group.SIGNAL,
                        "weakRows",
                        0,
                        1,
                        .001f,
                        "rowGroups",
                        1,
                        2048,
                        1,
                        "rowOffset",
                        -1,
                        1,
                        .001f,
                        "readoutShear",
                        -1,
                        1,
                        .001f,
                        "lineLoss",
                        0,
                        1,
                        .001f,
                        "linePosition",
                        0,
                        1,
                        .001f,
                        "lineHeight",
                        0,
                        1,
                        .001f,
                        "lineRetention",
                        0,
                        1,
                        .001f,
                    )

                Effects.BIT_ERROR ->
                    add(
                        p,
                        Group.SIGNAL,
                        "bitProbability",
                        0,
                        1,
                        .001f,
                        "bitIndex",
                        0,
                        1,
                        .001f,
                        "bitBlock",
                        2,
                        512,
                        1,
                    )

                Effects.ADDRESS_ERROR ->
                    add(
                        p,
                        Group.SIGNAL,
                        "byteOffset",
                        0,
                        4096,
                        1,
                        "addressRegion",
                        2,
                        4096,
                        2,
                        "addressProbability",
                        0,
                        1,
                        .001f,
                    )

                Effects.CFA_ERROR ->
                    add(
                        p,
                        Group.SIGNAL,
                        "cfaCoverage",
                        0,
                        1,
                        .001f,
                        "cfaPhase",
                        0,
                        2,
                        1,
                        "cfaRegion",
                        2,
                        1024,
                        2,
                    )

                Effects.DEMOSAIC_ERROR ->
                    add(
                        p,
                        Group.SIGNAL,
                        "interpolationMix",
                        0,
                        1,
                        .001f,
                        "sampleScale",
                        1,
                        64,
                        1,
                    )

                Effects.CHROMA_ERROR ->
                    add(
                        p,
                        Group.SIGNAL,
                        "chromaOffset",
                        -1,
                        1,
                        .001f,
                        "chromaAngle",
                        -3.141593f,
                        3.141593f,
                        .001f,
                        "chromaBlock",
                        1,
                        512,
                        1,
                    )

                Effects.COLOR_MAP ->
                    add(
                        p,
                        Group.SIGNAL,
                        "paletteMix",
                        0,
                        1,
                        .001f,
                        "palettePhase",
                        0,
                        1,
                        .001f,
                        "paletteCycles",
                        .01f,
                        32,
                        .01f,
                    )

                Effects.BLOCK_ERROR ->
                    add(
                        p,
                        Group.SIGNAL,
                        "quantLevels",
                        2,
                        256,
                        1,
                        "blockColumns",
                        1,
                        512,
                        1,
                        "blockError",
                        0,
                        1,
                        .001f,
                        "blockOffset",
                        -1,
                        1,
                        .001f,
                    )

                Effects.STREAM_ERROR ->
                    add(
                        p,
                        Group.SIGNAL,
                        "streamLoss",
                        0,
                        1,
                        .001f,
                        "streamColumns",
                        1,
                        256,
                        1,
                        "concealment",
                        0,
                        1,
                        .001f,
                    )

                Effects.VHS -> {
                    add(p, Group.PROFILE, "tapeBandwidth", 0, 1, .001f)
                    add(
                        p,
                        Group.SIGNAL,
                        "trackingOffset",
                        -.5f,
                        .5f,
                        .001f,
                        "trackingWave",
                        0,
                        .25f,
                        .001f,
                        "trackingPhase",
                        -6.283186f,
                        6.283186f,
                        .001f,
                        "trackingSlip",
                        -.5f,
                        .5f,
                        .001f,
                        "tapeDropout",
                        0,
                        1,
                        .001f,
                        "dropoutPosition",
                        0,
                        1,
                        .001f,
                        "tapeNoise",
                        0,
                        1,
                        .001f,
                        "grainSeed",
                        0,
                        997,
                        .1f,
                    )
                }

                Effects.CRT -> {
                    add(
                        p,
                        Group.PROFILE,
                        "scanDepth",
                        0,
                        1,
                        .001f,
                        "scanLines",
                        1,
                        2160,
                        1,
                        "phosphorMix",
                        0,
                        1,
                        .001f,
                    )
                    add(
                        p,
                        Group.SIGNAL,
                        "convergenceOffset",
                        -.5f,
                        .5f,
                        .001f,
                        "syncOffset",
                        -.5f,
                        .5f,
                        .001f,
                    )
                }

                Effects.MOTION_BLUR -> add(p, Group.SIGNAL, "blurX", -.12f, .12f, .001f, "blurY", -.12f, .12f, .001f)
                Effects.THERMAL_NOISE -> add(p, Group.SIGNAL, "noiseAmplitude", 0, .5f, .001f, "noiseGrain", 1, 16, 1, "grainSeed", 0, 997, .1f)
                Effects.SMEAR -> add(p, Group.SIGNAL, "smearAmount", 0, 2, .01f, "smearLength", 0, .4f, .001f, "smearThreshold", 0, .99f, .001f)
                else -> throw IllegalArgumentException("Fault ID")
            }
            if (id == Effects.EXPOSURE) {
                add(p, Group.TIME, "exposureRate", -40, 40, .01f, "exposurePhaseOffset", -6.283186f, 6.283186f, .001f)
                add(p, Group.PROFILE, "exposureClock", 0, 1, 1)
            }
            if (id == Effects.CRT) {
                add(p, Group.TIME, "refreshRate", 0, 60, .1f)
                add(p, Group.SIGNAL, "refreshSeed", 0, 997, .1f)
            }
            if (id == Effects.VHS || id == Effects.CRT) {
                add(p, Group.PROFILE, "transportKind", 0, 3, 1)
                add(p, Group.SIGNAL, "transportDamage", 0, 1, .001f, "transportLoss", 0, 1, .001f)
                if (id == Effects.VHS) {
                    add(p, Group.PROFILE, "mediaReduce", 0, 1, 1, "cableKind", 0, 1, 1)
                    add(p, Group.SIGNAL, "transportNoise", 0, 1, .001f)
                } else {
                    add(p, Group.PROFILE, "upconvert", 0, 1, 1)
                    add(p, Group.SIGNAL, "networkFps", 0, 60, 1, "networkStall", 0, 1, 1, "refreshBand", 0, 1, .001f, "networkSeed", 0, 997, .1f)
                }
            }
            CATALOG.put(id, Collections.unmodifiableList<Spec>(p))
        }
    }

    fun incidents(id: Int): Boolean {
        return id == Effects.ROW_ERROR ||
            id == Effects.BIT_ERROR ||
            id == Effects.ADDRESS_ERROR ||
            id == Effects.BLOCK_ERROR ||
            id == Effects.STREAM_ERROR ||
            id == Effects.VHS || id == Effects.CRT
    }

    private fun add(p: MutableList<Spec>, group: Group, vararg values: Any) {
        var n = 0
        while (n < values.size) {
            p.add(
                FaultParameters.Spec(
                    (values[n] as kotlin.String),
                    (values[n + 1] as Number).toFloat(),
                    (values[n + 2] as Number).toFloat(),
                    (values[n + 3] as Number).toFloat(),
                    group,
                )
            )
            n += 4
        }
    }

    fun all(id: Int): MutableList<Spec> {
        val p = CATALOG.get(id)
        requireNotNull(p) { "Fault ID" }
        return p
    }

    fun spec(id: Int, key: String): Spec {
        for (p in all(id)) if (p.key == key) return p
        throw IllegalArgumentException("Unknown internal parameter: " + id + " / " + key)
    }

    internal enum class Group {
        INPUT,
        TIME,
        EVENT,
        SIGNAL,
        PROFILE,
    }

    internal class Spec(
        val key: String,
        val min: Float,
        val max: Float,
        val step: Float,
        val group: Group,
    ) {
        fun validate(value: Float): Float {
            require(!(!java.lang.Float.isFinite(value) || value < min || value > max)) {
                key + " range " + min + "…" + max
            }
            return value
        }
    }
}
