package com.bongorian.signa1

/** UI capabilities follow actual consumers. Inactive serialized overrides remain readable. */
internal object FaultCapabilities {
    private fun keys(value: String) = value.split(' ').filter { it.isNotEmpty() }.toSet()
    private val signals = mapOf(
        Effects.PIXEL_DAMAGE to keys("identitySeed pixelDensity columnDensity hotFraction hotValue sensorNoise grainSeed"),
        Effects.EXPOSURE to keys("exposureDepth exposurePhase scanPhase integration exposureClock"),
        Effects.ROW_ERROR to keys("identitySeed weakRows rowGroups rowOffset readoutShear lineLoss linePosition lineHeight lineRetention"),
        Effects.BIT_ERROR to keys("identitySeed eventSeed bitProbability bitIndex bitBlock"),
        Effects.ADDRESS_ERROR to keys("identitySeed byteOffset addressRegion addressProbability"),
        Effects.CFA_ERROR to keys("identitySeed cfaCoverage cfaPhase cfaRegion"),
        Effects.DEMOSAIC_ERROR to keys("identitySeed interpolationMix sampleScale"),
        Effects.CHROMA_ERROR to keys("chromaOffset chromaAngle chromaBlock"),
        Effects.COLOR_MAP to keys("paletteMix palettePhase paletteCycles"),
        Effects.BLOCK_ERROR to keys("identitySeed eventSeed quantLevels blockColumns blockError blockOffset"),
        Effects.STREAM_ERROR to keys("eventSeed streamLoss streamColumns concealment"),
        Effects.MOTION_BLUR to keys("blurX blurY"),
        Effects.THERMAL_NOISE to keys("noiseAmplitude noiseGrain grainSeed"),
        Effects.SMEAR to keys("smearAmount smearLength smearThreshold"),
    )
    fun active(id: Int, p: EffectParameters, experimental: Boolean): List<FaultParameters.Spec> {
        val kind = p.transportKind(id)
        val digital = id == Effects.VHS && kind == 2 || id == Effects.CRT && kind == 1
        val network = id == Effects.CRT && kind == 2
        val led = id == Effects.CRT && kind == 3
        val vhs = id == Effects.VHS && kind == 0
        val analog = id == Effects.VHS && kind == 3
        val crt = id == Effects.CRT && kind == 0
        val incidents = !digital && (FaultParameters.incidents(id) && (id != Effects.CRT || network))
        val drift = id in setOf(Effects.PIXEL_DAMAGE, Effects.ROW_ERROR, Effects.CHROMA_ERROR, Effects.BLOCK_ERROR) || vhs || crt
        val phase = vhs || analog
        val time = drift || phase || incidents || led || id in setOf(Effects.EXPOSURE, Effects.THERMAL_NOISE)
        val imageKeys = when (id) {
            Effects.VHS -> when (kind) {
                0 -> keys("transportKind mediaReduce tapeBandwidth identitySeed eventSeed trackingOffset trackingWave trackingPhase trackingSlip tapeDropout dropoutPosition tapeNoise grainSeed")
                1 -> keys("transportKind mediaReduce identitySeed eventSeed transportDamage transportLoss")
                2 -> keys("transportKind")
                else -> keys("transportKind cableKind transportDamage transportLoss transportNoise trackingPhase dropoutPosition grainSeed")
            }
            Effects.CRT -> when (kind) {
                0 -> keys("transportKind scanDepth scanLines phosphorMix convergenceOffset syncOffset")
                1 -> keys("transportKind upconvert")
                2 -> keys("transportKind transportLoss networkFps networkStall")
                else -> keys("transportKind identitySeed transportLoss transportDamage refreshBand refreshSeed")
            }
            else -> signals.getValue(id)
        }
        val position = id == Effects.ROW_ERROR || vhs || analog
        val pattern = id in setOf(Effects.BIT_ERROR, Effects.BLOCK_ERROR, Effects.STREAM_ERROR) || vhs || id == Effects.VHS && kind == 1
        return FaultParameters.all(id).filter { spec -> when {
            spec.group == FaultParameters.Group.INPUT -> experimental && !digital
            spec.key == "identityBias" -> vhs || crt || id == Effects.BLOCK_ERROR
            spec.key in setOf("timeScale", "timeOffset", "time") -> time
            spec.key in setOf("driftSpeed", "drift") -> drift
            spec.key in setOf("phaseSpeed", "phase") -> phase
            spec.key in setOf("exposureRate", "exposurePhaseOffset") -> id == Effects.EXPOSURE
            spec.key == "refreshRate" -> led
            spec.group == FaultParameters.Group.EVENT -> incidents && when (spec.key) {
                "eventPosition" -> position
                "eventPattern" -> pattern
                "eventEnvelope" -> !network // Network uses the rectangular schedule, or final networkStall FIX.
                else -> true
            }
            else -> spec.key in imageKeys
        } }
    }

    /** Labels use physical units only where the implementation actually has them. */
    fun unit(key: String): String = when (key) {
        "time", "timeOffset", "eventPeriod", "eventDuration" -> "s"
        "phase", "trackingPhase", "exposurePhase", "exposurePhaseOffset", "convergenceAngle", "chromaAngle" -> "rad"
        "phaseSpeed", "exposureRate" -> "rad/s"
        "refreshRate" -> "Hz"
        "networkFps" -> "fps"
        "byteOffset", "addressRegion" -> "B"
        "bitBlock", "cfaRegion", "sampleScale", "chromaBlock", "noiseGrain" -> "px"
        else -> ""
    }
}
