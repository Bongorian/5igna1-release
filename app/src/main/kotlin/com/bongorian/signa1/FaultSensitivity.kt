package com.bongorian.signa1

/** Per-stage measured-input gains. Native routes retain their old defaults. */
internal object FaultSensitivity {
    val keys = arrayOf("motionSensitivity", "audioSensitivity", "timingSensitivity", "thermalSensitivity", "cpuSensitivity")
    private fun incidents(id: Int) = id == Effects.ROW_ERROR || id == Effects.BIT_ERROR || id == Effects.ADDRESS_ERROR || id == Effects.BLOCK_ERROR || id == Effects.STREAM_ERROR || id == Effects.VHS
    fun native(id: Int, source: Int, kind: Int = 0): Boolean = when {
        id == Effects.CRT && kind != 0 -> false
        id == Effects.VHS && kind != 0 -> kind != 2 && (source == 2 || source == 4)
        else -> when (source) {
        0 -> id == Effects.ROW_ERROR || id == Effects.VHS || id == Effects.CRT || id == Effects.MOTION_BLUR
        1 -> id == Effects.ROW_ERROR || id == Effects.VHS || id == Effects.CRT
        2 -> incidents(id) || id == Effects.EXPOSURE || id == Effects.MOTION_BLUR || id == Effects.THERMAL_NOISE || id == Effects.SMEAR
        3 -> id == Effects.PIXEL_DAMAGE || id == Effects.BIT_ERROR || id == Effects.THERMAL_NOISE
        4 -> incidents(id)
        else -> false
    }
    }
    fun initial(id: Int, source: Int) = if (native(id, source)) 1f else 0f
    private val defaults = Array(Effects.NAMES.size) { id -> FloatArray(5) { initial(id, it) } }
    fun values(parameters: EffectParameters, id: Int, experimental: Boolean): FloatArray =
        if (parameters.transportKind(id) == 0 && (!experimental || keys.none { parameters.manual(id, it) })) defaults[id]
        else FloatArray(5) { source ->
            val initial = if (native(id, source, parameters.transportKind(id))) 1f else 0f
            if (experimental) parameters.resolved(id, keys[source], initial) else initial
        }
}
