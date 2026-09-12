package com.bongorian.signa1

import com.bongorian.signa1.FaultNode.Motion
import java.security.SecureRandom
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** One camera-driven timeline. Snapshot evaluation is pure and never draws random numbers. */
internal class FaultModel constructor(private val sessionSalt: Long = SecureRandom().nextLong()) {
    internal class Inputs {
        var ax: Float = 0f
        var ay: Float = 0f
        var az: Float = 0f
        var tilt: Float = 0f
        var angularSpeed: Float = 0f
        var rotation: Float = 0f
        var audio: Float = 0f
        var cpu: Float = 0f
        var heat: Float = 0f
        var jitter: Float = 0f
        var sensorNs: Long = 0
        var exposureNs: Long = 0
        var skewNs: Long = 0
        var motionAvailable: Boolean = false
        var timingAvailable: Boolean = false
    }

    private var lastSeconds = Double.NaN
    private var elapsed = 0.0
    private var performancePosition = 0.0
    private var cueAge = Double.POSITIVE_INFINITY
    private var liveWasEnabled = false
    private var heldSignalNs: Long = 0
    private var cpuPressure = 0f
    private var timingPressure = 0f
    private var audioDisplacement = 0f
    private var audioVelocity = 0f
    private var angularSpeed = 0f
    var displacement: Float = 0f
    var velocity: Float = 0f
    var shock: Float = 0f
    var pressure: Float = 0f
    var temperature: Float = 0f
    var audio: Float = 0f
    var readout: Float = 0f
    var tilt: Float = 0f
    var rotation: Float = 0f
    var sensorNs: Long = 0
    var exposureNs: Long = 0
    var skewNs: Long = 0
    var timingAvailable: Boolean = false
    var motionAvailable: Boolean = false

    fun copy(): FaultModel {
        val n = FaultModel(sessionSalt)
        n.lastSeconds = lastSeconds
        n.elapsed = elapsed
        n.performancePosition = performancePosition
        n.cueAge = cueAge
        n.liveWasEnabled = liveWasEnabled
        n.heldSignalNs = heldSignalNs
        n.cpuPressure = cpuPressure
        n.timingPressure = timingPressure
        n.audioDisplacement = audioDisplacement
        n.audioVelocity = audioVelocity
        n.angularSpeed = angularSpeed
        n.displacement = displacement
        n.velocity = velocity
        n.shock = shock
        n.pressure = pressure
        n.temperature = temperature
        n.audio = audio
        n.readout = readout
        n.tilt = tilt
        n.rotation = rotation
        n.sensorNs = sensorNs
        n.exposureNs = exposureNs
        n.skewNs = skewNs
        n.timingAvailable = timingAvailable
        n.motionAvailable = motionAvailable
        return n
    }

    fun reset() {
        lastSeconds = Double.NaN
        cpuPressure = 0f; timingPressure = 0f; audioDisplacement = 0f; audioVelocity = 0f; angularSpeed = 0f
        rotation = 0f
        tilt = rotation
        readout = tilt
        audio = readout
        temperature = audio
        pressure = temperature
        shock = pressure
        velocity = shock
        displacement = velocity
        motionAvailable = false
        timingAvailable = motionAvailable
    }

    fun hit() {
        cueAge = 0.0
    }

    fun rewind() {
        performancePosition = 0.0
        cueAge = Double.POSITIVE_INFINITY
    }

    fun time(config: FaultConfig): Double {
        return if (config.enabled) config.performance.time(performancePosition) else elapsed
    }

    fun cue(config: FaultConfig): Float {
        return if (config.enabled) clamp((1 - cueAge / .65).toFloat(), 0f, 1f) else 0f
    }

    fun advance(now: Double, input: Inputs, config: FaultConfig) {
        if (
            !java.lang.Double.isFinite(now) ||
                (!java.lang.Double.isNaN(lastSeconds) && now <= lastSeconds)
        )
            return
        val delta = if (java.lang.Double.isNaN(lastSeconds)) 0.0 else now - lastSeconds
        lastSeconds = now
        elapsed += delta
        if (config.enabled && !liveWasEnabled) performancePosition = elapsed - delta
        liveWasEnabled = config.enabled
        sensorNs = input.sensorNs
        if (config.enabled && config.performance.hold) return
        heldSignalNs = input.sensorNs
        if (config.enabled) performancePosition += delta * config.performance.speed
        cueAge += delta
        val dt = min(.25, delta).toFloat()
        sensorNs = input.sensorNs
        exposureNs = input.exposureNs
        skewNs = input.skewNs
        val gain = .25f + 1.75f * config.sensitivity
        motionAvailable = config.enabled && config.motion && input.motionAvailable
        timingAvailable = config.enabled && config.timing && input.timingAvailable
        val ax = if (motionAvailable) clamp(input.ax / 9.80665f, -4f, 4f) else 0f
        val acceleration =
            if (motionAvailable)
                clamp(
                    sqrt(
                            (input.ax * input.ax + input.ay * input.ay + input.az * input.az)
                                .toDouble()
                        )
                        .toFloat() / 9.80665f,
                    0f,
                    4f,
                )
            else 0f
        val hit: Float = clamp((acceleration - .06f) * gain, 0f, 1f)
        shock = max(hit, follow(shock, 0f, dt, .38f))
        tilt = if (motionAvailable) clamp(input.tilt, -1f, 1f) else 0f
        angularSpeed = if (motionAvailable) clamp(input.angularSpeed, 0f, 12f) else 0f
        rotation = if (motionAvailable) clamp(input.rotation, -6f, 6f) else 0f
        audio =
            follow(
                audio,
                if (config.enabled && config.audio) clamp(input.audio * gain, 0f, 1f) else 0f,
                dt,
                .12f,
            )
        temperature =
            follow(
                temperature,
                if (config.enabled && config.thermal) clamp(input.heat, 0f, 1f) else 0f,
                dt,
                3f,
            )
        readout =
            follow(
                readout,
                if (config.enabled && config.timing) clamp(input.jitter * gain, 0f, 1f) else 0f,
                dt,
                .18f,
            )
        val demand =
            max(
                readout,
                if (config.enabled && config.cpu)
                    clamp(
                        (input.cpu - .28f) * 1.4f * gain,
                        0f,
                        1f,
                    )
                else 0f,
            )
        pressure = follow(pressure, demand, dt, if (demand > pressure) .045f else .45f)
        val cpuDemand = if (config.enabled && config.cpu) clamp((input.cpu - .28f) * 1.4f * gain, 0f, 1f) else 0f
        cpuPressure = follow(cpuPressure, cpuDemand, dt, if (cpuDemand > cpuPressure) .045f else .45f)
        timingPressure = follow(timingPressure, readout, dt, if (readout > timingPressure) .045f else .45f)
        val audioForce = audio * .10f * sin(elapsed * 2 * Math.PI * 37).toFloat()
        val force =
            (-ax * .75f + rotation * .045f) * gain +
                audio * .10f * sin(elapsed * 2 * Math.PI * 37).toFloat()
        val steps = max(1, ceil((dt * 120).toDouble()).toInt())
        val step = dt / steps
        for (i in 0..<steps) {
            audioVelocity += (-100 * audioDisplacement - 12 * audioVelocity + audioForce * 80) * step
            audioDisplacement = clamp(audioDisplacement + audioVelocity * step, -1f, 1f)
            audioVelocity = clamp(audioVelocity, -8f, 8f)
            velocity += (-100 * (displacement - tilt * .10f) - 12 * velocity + force * 80) * step
            displacement = clamp(displacement + velocity * step, -1f, 1f)
            velocity = clamp(velocity, -8f, 8f)
        }
        // Disabling/unavailable inputs cannot leave a hidden bias coupled into future frames.
        if (!config.enabled) {
            cpuPressure = 0f; timingPressure = 0f; audioDisplacement = 0f; audioVelocity = 0f; angularSpeed = 0f
            rotation = 0f
            tilt = rotation
            readout = tilt
            audio = readout
            temperature = audio
            pressure = temperature
            shock = pressure
            velocity = shock
            displacement = velocity
        }
    }

    private fun event(
        identity: Long,
        id: Int,
        time: Double,
        period: Double,
        duration: Double,
        probability: Float,
        serial: Long,
    ): FaultNode.Event {
        val age: Double = LivePerformance.wrap(time, period)
        val key: Long = mix(identity xor (id.toLong() shl 48) xor mix(serial) xor 0x4556454e54L)
        val gate =
            (if (random(key) < clamp(probability, 0f, 1f) && age < duration) 1 else 0).toFloat()
        val envelope: Float =
            gate * clamp(min(age / .025, (duration - age) / .09).toFloat(), 0f, 1f)
        return FaultNode.Event(serial, envelope, random(key + 1), random(key + 2) * 997, identity)
    }

    fun apply(base: EffectState.Frame, config: FaultConfig): EffectState.Frame {
        val nodes: MutableList<FaultNode> = ArrayList<FaultNode>()
        val ids = base.ids().filter { config.experimental || !Effects.physical(it) }.toIntArray()
        val time = time(config)
        val cue = cue(config)
        if (base.amount > 0)
            for (index in ids.indices) {
                val directed =
                    if (config.enabled)
                        config.performance.envelope(
                            time,
                            index,
                            ids.size,
                            sessionSalt,
                        )
                    else 1f
                val level: Float =
                    clamp(base.amount * directed + (1 - base.amount * directed) * cue, 0f, 1f)
                // A fully gated stage bypasses its profile as well as its mechanism.
                if (level > 0) nodes.add(compile(ids[index], base.parameters, level, config))
            }
        return EffectState.Frame(ids, base.amount, base.parameters, sensorNs, time, nodes, config.experimental)
    }

    fun inspect(id: Int, controls: EffectParameters, level: Float, config: FaultConfig): FaultNode {
        return compile(id, controls, level, config)
    }

    private fun compile(
        id: Int,
        controls: EffectParameters,
        baseLevel: Float,
        config: FaultConfig,
    ): FaultNode {
        val gains = FaultSensitivity.values(controls, id, config.experimental)
        var extra = 0f
        if (config.enabled && config.experimental) for (source in gains.indices) {
            if (!FaultSensitivity.native(id, source)) extra += gains[source] * when (source) {
                0 -> if (config.motion) clamp(max(shock, angularSpeed / 6f), 0f, 1f) else 0f
                1 -> if (config.audio) audio else 0f
                2 -> if (config.timing) readout else 0f
                3 -> if (config.thermal) temperature else 0f
                else -> if (config.cpu) cpuPressure else 0f
            }
        }
        val level = if (extra == 0f) baseLevel else clamp(baseLevel + (1 - baseLevel) * extra, 0f, 1f)
        val seed = controls.identity(id)
        val initial = FaultNode.Identity(seed)
        val identity =
            FaultNode.Identity(
                seed,
                controls.resolved(id, "identitySeed", initial.spatialSeed),
                controls.resolved(id, "identityBias", initial.bias),
            )
        val timeScale = controls.resolved(id, "timeScale", 1f)
        val timeOffset = controls.resolved(id, "timeOffset", 0f)
        val time =
            if (controls.manual(id, "time")) controls.resolved(id, "time", 0f).toDouble()
            else time(config) * timeScale + timeOffset
        val speed =
            controls.resolved(
                id,
                "driftSpeed",
                if (id == Effects.VHS) .24f
                else if (id == Effects.CRT) .16f else if (id == Effects.EXPOSURE) .8f else .42f,
            )
        val moving =
            id == Effects.PIXEL_DAMAGE ||
                id == Effects.EXPOSURE ||
                id == Effects.ROW_ERROR ||
                id == Effects.CHROMA_ERROR ||
                id == Effects.BLOCK_ERROR ||
                id == Effects.VHS ||
                id == Effects.CRT
        val drift =
            controls.resolved(
                id,
                "drift",
                if (moving) drift(seed xor 0x4d4f54494f4eL, time * speed) else 0f,
            )
        val phaseSpeed =
            controls.resolved(id, "phaseSpeed", if (id == Effects.EXPOSURE) .5f else 1.7f)
        val phase =
            controls.resolved(
                id,
                "phase",
                ((time * phaseSpeed + random(seed) * Math.PI * 2) % (Math.PI * 2)).toFloat(),
            )
        val motion = Motion(time, drift, phase)
        val coupling = (if (config.enabled) 1 else 0).toFloat()
        // Keep the original arithmetic exactly when native gains are unchanged.
        val pressure = (if (gains[2] == 1f && gains[4] == 1f) this.pressure
            else max(timingPressure * gains[2], cpuPressure * gains[4])) * coupling
        val heat = temperature * coupling * gains[3]
        val move = (if (gains[0] == 1f && gains[1] == 1f) displacement
            else (displacement - audioDisplacement) * gains[0] + audioDisplacement * gains[1]) * coupling
        val rotation = this.rotation * gains[0]
        val readout = this.readout * gains[2]
        val audio = this.audio * gains[1]
        val timingAvailable = this.timingAvailable && gains[2] > 0f
        val period =
            controls.resolved(
                id,
                "eventPeriod",
                if (id == Effects.VHS) 2.3f
                else if (id == Effects.STREAM_ERROR) 1.1f
                else if (id == Effects.ROW_ERROR) 1.7f
                else if (id == Effects.BIT_ERROR) .6f else 2.7f,
            )
        val duration =
            controls.resolved(
                id,
                "eventDuration",
                if (id == Effects.VHS) .48f else if (id == Effects.STREAM_ERROR) .38f else .19f,
            )
        val activity =
            if (id == Effects.ROW_ERROR || id == Effects.STREAM_ERROR)
                controls.get(
                    id,
                    "loss",
                )
            else if (id == Effects.VHS)
                max(
                    controls.get(id, "dropout"),
                    controls.get(id, "tracking") * .5f,
                )
            else if (id == Effects.BIT_ERROR || id == Effects.ADDRESS_ERROR)
                controls.get(
                    id,
                    "activity",
                )
            else if (id == Effects.BLOCK_ERROR) controls.get(id, "misaddress") else 0f
        val incidents = FaultParameters.incidents(id)
        val probability =
            controls.resolved(
                id,
                "eventProbability",
                clamp(activity * .8f + pressure * .5f, 0f, 1f),
            )
        val serial =
            if (controls.manual(id, "eventSerial"))
                Math.round(
                        controls.resolved(
                            id,
                            "eventSerial",
                            0f,
                        )
                    )
                    .toLong()
            else floor(time / period).toLong()
        val automatic =
            if (incidents)
                event(
                    controls.eventIdentity(id, seed xor sessionSalt),
                    id,
                    time,
                    period.toDouble(),
                    duration.toDouble(),
                    probability,
                    serial,
                )
            else FaultNode.Event(-1, 0f, 0f, 0f)
        val event =
            FaultNode.Event(
                automatic.serial,
                controls.resolved(id, "eventEnvelope", max(automatic.envelope, cue(config))),
                controls.resolved(id, "eventPosition", automatic.position),
                controls.resolved(id, "eventPattern", automatic.pattern),
                automatic.identity,
            )
        val internal: MutableMap<String, Float> = LinkedHashMap<String, Float>()
        put(
            internal,
            "timeScale",
            timeScale,
            "timeOffset",
            timeOffset,
            "time",
            time.toFloat(),
            "driftSpeed",
            speed,
            "drift",
            drift,
            "phaseSpeed",
            phaseSpeed,
            "phase",
            phase,
            "identityBias",
            identity.bias,
        )
        if (incidents)
            put(
                internal,
                "eventPeriod",
                period,
                "eventDuration",
                duration,
                "eventProbability",
                probability,
                "eventSerial",
                event.serial.toFloat(),
                "eventEnvelope",
                event.envelope,
                "eventPosition",
                event.position,
                "eventPattern",
                event.pattern,
            )
        if (config.experimental) FaultSensitivity.keys.forEachIndexed { source, key ->
            if (controls.manual(id, key)) internal[key] = gains[source]
        }
        val warped =
            config.enabled &&
                (config.performance.clock != LivePerformance.FREE ||
                    config.performance.speed != 1f) ||
                timeScale != 1f ||
                timeOffset != 0f ||
                controls.manual(
                    id,
                    "time",
                )
        val signalNs =
            if (warped) (time * 60).toLong() * 16666667L
            else if (config.enabled && config.performance.hold) heldSignalNs else sensorNs
        val p: MutableMap<String, Float> = LinkedHashMap<String, Float>()
        val profile: MutableMap<String, Float> = LinkedHashMap<String, Float>()
        // Domain-specific values, in sample/normalized signal units; no universal strength uniform.
        p.put("identitySeed", identity.spatialSeed)
        p.put("eventSeed", if (event.envelope > 0) event.pattern else identity.spatialSeed)
        when (id) {
            Effects.PIXEL_DAMAGE -> {
                put(
                    p,
                    "pixelDensity",
                    level * controls.get(id, "density") * .025f,
                    "columnDensity",
                    level * controls.get(id, "columns") * .07f,
                    "hotFraction",
                    controls.get(id, "hot"),
                    "hotValue",
                    clamp(.8f + drift * .12f + heat * .3f, 0f, 1f),
                    "sensorNoise",
                    heat * level * .035f,
                )
                p.put("grainSeed", random(seed xor mix(signalNs)) * 997)
            }

            Effects.EXPOSURE -> {
                val rate = controls.get(id, "rate")
                val bands = controls.get(id, "bands")
                var exposurePhase =
                    ((time * rate * 18 + identity.spatialSeed) % (Math.PI * 2)).toFloat()
                var scan = 4 + bands * 160
                var integrate = 1f
                if (timingAvailable) {
                    val basePhase = exposurePhase
                    val baseScan = scan
                    val hz = config.mains * 2.0
                    exposurePhase =
                        ((((signalNs % 1000000000L) * 1e-9 * hz % 1) * Math.PI * 2 +
                                time * rate * 18) % (Math.PI * 2))
                            .toFloat()
                    scan = (skewNs * 1e-9 * hz * Math.PI * 2).toFloat() * (1 + bands * 8)
                    val x = Math.PI * exposureNs * 1e-9 * hz
                    integrate = if (x < 1e-6) 1f else (sin(x) / x).toFloat()
                    if (gains[2] != 1f) {
                        exposurePhase = basePhase + (exposurePhase - basePhase) * gains[2]
                        scan = clamp(baseScan + (scan - baseScan) * gains[2], 0f, 2000f)
                        integrate = clamp(1 + (integrate - 1) * gains[2], -1f, 1f)
                    }
                }
                put(
                    p,
                    "exposureDepth",
                    level * controls.get(id, "depth"),
                    "exposurePhase",
                    exposurePhase,
                    "scanPhase",
                    scan,
                    "integration",
                    integrate,
                )
            }

            Effects.ROW_ERROR -> {
                val width = controls.get(id, "displacement") * level
                put(
                    p,
                    "weakRows",
                    .15f + .65f * level,
                    "rowGroups",
                    12 + 228 * (1 - controls.get(id, "bands")),
                    "rowOffset",
                    width * (.08f + drift * .20f),
                    "readoutShear",
                    width * (move * .18f + rotation * coupling * .008f + readout * coupling * .1f),
                    "lineLoss",
                    event.envelope * controls.get(id, "loss") * level,
                    "linePosition",
                    event.position,
                    "lineHeight",
                    .005f + controls.get(id, "bands") * .13f,
                    "lineRetention",
                    controls.get(id, "concealment"),
                )
            }

            Effects.BIT_ERROR ->
                put(
                    p,
                    "bitProbability",
                    level *
                        controls.get(id, "activity") *
                        (.08f + .8f * event.envelope + heat * .2f),
                    "bitIndex",
                    controls.get(id, "bit"),
                    "bitBlock",
                    2 + controls.get(id, "burst_size") * 126,
                )

            Effects.ADDRESS_ERROR ->
                put(
                    p,
                    "byteOffset",
                    Math.round(controls.get(id, "offset") * 31 * level).toFloat(),
                    "addressRegion",
                    4 + 2 * Math.round(controls.get(id, "region") * 254).toFloat(),
                    "addressProbability",
                    level * controls.get(id, "activity") * (.25f + .75f * event.envelope),
                )

            Effects.CFA_ERROR ->
                put(
                    p,
                    "cfaCoverage",
                    level * controls.get(id, "coverage"),
                    "cfaPhase",
                    Math.round(controls.get(id, "phase") * 2).toFloat(),
                    "cfaRegion",
                    2 + 2 * Math.round(controls.get(id, "region") * 127).toFloat(),
                )

            Effects.DEMOSAIC_ERROR ->
                put(
                    p,
                    "interpolationMix",
                    level * controls.get(id, "interpolation"),
                    "sampleScale",
                    1 + floor((controls.get(id, "sampling") * 15).toDouble()).toFloat(),
                )

            Effects.CHROMA_ERROR ->
                put(
                    p,
                    "chromaOffset",
                    level * controls.get(id, "separation") * (.006f + .065f * (.5f + .5f * drift)),
                    "chromaAngle",
                    controls.get(id, "direction") * Math.PI.toFloat(),
                    "chromaBlock",
                    1 + level * controls.get(id, "sampling") * 95,
                )

            Effects.COLOR_MAP ->
                put(
                    p,
                    "paletteMix",
                    level * controls.get(id, "mix"),
                    "palettePhase",
                    controls.get(id, "palette"),
                    "paletteCycles",
                    .5f + controls.get(id, "cycles") * 5,
                )

            Effects.BLOCK_ERROR ->
                put(
                    p,
                    "quantLevels",
                    256 -
                        floor((level * controls.get(id, "quantization") * 252).toDouble())
                            .toFloat(),
                    "blockColumns",
                    8 + (1 - controls.get(id, "block_size")) * 72,
                    "blockError",
                    level * controls.get(id, "misaddress") * event.envelope,
                    "blockOffset",
                    (identity.bias + drift) * .25f,
                )

            Effects.STREAM_ERROR ->
                put(
                    p,
                    "streamLoss",
                    level * controls.get(id, "loss") * event.envelope,
                    "streamColumns",
                    8 + (1 - controls.get(id, "region")) * 56,
                    "concealment",
                    controls.get(id, "concealment"),
                )

            Effects.VHS -> {
                // Media characteristics (bandwidth, chroma leakage) and faults are independent.
                val tracking = level * controls.get(id, "tracking")
                profile.put("tapeBandwidth", level * controls.get(id, "bandwidth"))
                put(
                    p,
                    "trackingOffset",
                    tracking * (identity.bias * .012f + drift * .03f + move * .12f),
                    "trackingWave",
                    tracking * (.003f + audio * coupling * .012f),
                    "trackingPhase",
                    phase,
                    "trackingSlip",
                    tracking * event.envelope * .12f,
                    "tapeDropout",
                    level * controls.get(id, "dropout") * event.envelope,
                    "dropoutPosition",
                    event.position,
                    "tapeNoise",
                    level * controls.get(id, "noise") * .22f,
                )
                p.put("grainSeed", random(seed xor mix(signalNs) xor 0x54415045L) * 997)
            }

            Effects.CRT -> {
                val scanLevel = level * controls.get(id, "scan")
                put(
                    profile,
                    "scanDepth",
                    scanLevel * .5f,
                    "scanLines",
                    240 + controls.get(id, "scan") * 760,
                    "phosphorMix",
                    level * controls.get(id, "phosphor"),
                )
                put(
                    p,
                    "convergenceOffset",
                    level *
                        controls.get(
                            id,
                            "convergence",
                        ) *
                        (identity.bias * .009f + drift * .003f),
                    "syncOffset",
                    level * controls.get(id, "sync") * (drift * .035f + move * .025f),
                )
            }

            Effects.MOTION_BLUR -> {
                val exposure = if (timingAvailable) clamp(exposureNs * 1e-9f * gains[2], 0f, .1f) else 1f / 60f
                val movement = if (config.enabled) (angularSpeed * exposure * 8f + shock * .2f) * gains[0] else 0f
                val length = clamp(level * controls.get(id, "amount") * (controls.get(id, "floor") * .025f + movement * .08f), 0f, .12f)
                val angle = controls.get(id, "direction") * Math.PI * 2
                put(p, "blurX", length * kotlin.math.cos(angle).toFloat(), "blurY", length * sin(angle).toFloat())
            }
            Effects.THERMAL_NOISE -> {
                val exposure = if (timingAvailable) clamp(exposureNs * 1e-9f * 30f * gains[2], 0f, 4f) else 1f
                val amplitude = level * controls.get(id, "amount") * (controls.get(id, "floor") * .08f + heat * (.06f + .10f * exposure))
                put(p, "noiseAmplitude", clamp(amplitude, 0f, .5f), "noiseGrain", 1 + controls.get(id, "grain") * 15, "grainSeed", random(seed xor mix(signalNs) xor 0x4e4f495345L) * 997)
            }
            Effects.SMEAR -> {
                val readoutDrive = if (config.enabled && timingAvailable) clamp(skewNs * 1e-9f * 45f * gains[2] + readout, 0f, 4f) else 0f
                put(p, "smearAmount", clamp(level * controls.get(id, "amount") * readoutDrive, 0f, 2f), "smearLength", controls.get(id, "length") * .4f, "smearThreshold", controls.get(id, "threshold") * .99f)
            }
            else -> throw IllegalArgumentException("Fault ID")
        }
        if ((id == Effects.VHS || id == Effects.CRT) && (controls.get(id, "transport") > 0f ||
                (id == Effects.VHS && controls.get(id, "reduce") >= .5f) || controls.overrides(id).keys.any { it in FaultNode.transportKeys })) {
            val kind = Math.round(controls.get(id, "transport") * 3).toFloat()
            profile["transportKind"] = kind
            if (id == Effects.VHS) {
                profile["mediaReduce"] = if (controls.get(id, "reduce") >= .5f) 1f else 0f
                profile["cableKind"] = if (controls.get(id, "cable") >= .5f) 1f else 0f
                p["transportDamage"] = level * controls.get(id, "tracking")
                p["transportLoss"] = level * controls.get(id, "dropout") * (.15f + .85f * event.envelope)
                p["transportNoise"] = level * controls.get(id, "noise")
            } else {
                profile["upconvert"] = if (controls.get(id, "upconvert") >= .5f) 1f else 0f
                p["transportDamage"] = level * controls.get(id, "convergence")
                p["transportLoss"] = level * controls.get(id, "scan")
                val slot = floor(time / .7).toLong()
                // Intrinsic payload variation follows fault time; automatic congestion requires LIVE.
                p["networkStall"] = if (config.enabled && random(seed xor mix(slot)) < level * controls.get(id, "sync") * .8f) 1f else 0f
                p["networkFps"] = 0f
                if (kind == 2f) {
                    p["transportLoss"] = level * (1f - NetworkDisplay.scale(controls.get(id, "networkResolution"))) / .8f
                    val rate = NetworkDisplay.fps(controls.get(id, "networkRate"))
                    p["networkFps"] = if (level <= 0f || rate == 0) 0f else 60f + (rate - 60f) * level
                    p["networkStall"] = if (config.enabled && level > 0f && NetworkDisplay.stalled(
                        time, seed, NetworkDisplay.interval(controls.get(id, "networkInterval")),
                        NetworkDisplay.duration(controls.get(id, "networkDuration")) * level)) 1f else 0f
                }
                p["refreshBand"] = level * controls.get(id, "sync") * .5f
                p["networkSeed"] = random(seed xor mix(slot)) * 997f
            }
        }
        for (value in controls.overrides(id).entries) {
            if (profile.containsKey(value.key)) profile.put(value.key, value.value)
            if (p.containsKey(value.key)) p.put(value.key, value.value)
        }
        return FaultNode(id, identity, motion, event, profile, p, internal)
    }

    companion object {
        fun mix(x: Long): Long {
            var x = x
            x = (x xor (x ushr 30)) * -0x40a7b892e31b1a47L
            x = (x xor (x ushr 27)) * -0x6b2fb644ecceee15L
            return x xor (x ushr 31)
        }

        fun random(seed: Long): Float {
            return (mix(seed) ushr 40) * 5.9604645E-8f
        }

        fun finite(x: Float): Float {
            return if (java.lang.Float.isFinite(x)) x else 0f
        }

        fun clamp(x: Float, lo: Float, hi: Float): Float {
            return max(lo, min(hi, finite(x)))
        }

        fun follow(
            value: Float,
            target: Float,
            dt: Float,
            tau: Float,
        ): Float {
            return target + (value - target) * exp((-dt / tau).toDouble()).toFloat()
        }

        fun drift(seed: Long, time: Double): Float {
            val cell = floor(time).toLong()
            var f = (time - cell).toFloat()
            f = f * f * (3 - 2 * f)
            return (random(seed xor mix(cell)) * (1 - f) + random(seed xor mix(cell + 1)) * f) * 2 -
                1
        }

        private fun put(p: MutableMap<String, Float>, vararg pairs: Any?) {
            var i = 0
            while (i < pairs.size) {
                p.put(pairs[i] as String, (pairs[i + 1] as Number).toFloat())
                i += 2
            }
        }
    }
}
