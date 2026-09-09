package com.bongorian.signa1

/** Immutable evaluated fault. Named mechanism parameters have no shared dimensionality/strength. */
internal class FaultNode
constructor(
    val id: Int,
    val identity: Identity,
    val motion: Motion,
    val event: Event,
    profile: Map<String, Float>,
    mechanism: Map<String, Float>,
    internal: Map<String, Float> = mutableMapOf<String, Float>(),
) {
    internal class Identity
    constructor(
        val seed: Long,
        val spatialSeed: Float = FaultModel.random(seed) * 997,
        val bias: Float = FaultModel.random(seed xor 0x12ab34cdL) * 2 - 1,
    )

    internal class Motion(val seconds: Double, val drift: Float, val phase: Float)

    internal class Event
    constructor(
        val serial: Long,
        val envelope: Float,
        val position: Float,
        val pattern: Float,
        val identity: Long = 0,
    )

    val profile = profile.immutableCopy()
    val mechanism = mechanism.immutableCopy()
    val internal = internal.immutableCopy()

    constructor(
        id: Int,
        identity: Identity,
        motion: Motion,
        event: Event,
        mechanism: Map<String, Float>,
    ) : this(id, identity, motion, event, mutableMapOf<String, Float>(), mechanism)

    fun get(key: String): Float =
        requireNotNull(mechanism[key] ?: profile[key]) { "Missing signal parameter $key" }

    fun inspect(): Map<String, Float> =
        LinkedHashMap(internal)
            .apply {
                putAll(profile)
                putAll(mechanism)
            }
            .immutableCopy()

    fun describe(): String {
        return Effects.name(id) +
            " profile=" +
            profile +
            " eventIdentity=" +
            event.identity +
            " event=" +
            event.serial +
            ":" +
            event.envelope +
            " motion=" +
            motion.seconds +
            ":" +
            motion.drift +
            ":" +
            motion.phase +
            " internal=" +
            internal +
            " faults=" +
            mechanism
    }
}
