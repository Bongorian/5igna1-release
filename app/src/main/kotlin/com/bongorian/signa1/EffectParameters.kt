package com.bongorian.signa1

import java.util.TreeMap
import kotlin.math.max
import kotlin.math.min

/** Immutable named UI macros. There is no strength/param1/param2/seed stride or physical ABI. */
internal class EffectParameters
private constructor(
    values: Map<Int, Map<String, Float>>,
    identities: Map<Int, Long>,
    overrides: Map<Int, Map<String, Float>> = emptyMap<Int, Map<String, Float>>(),
    eventIdentities: Map<Int, Long> = emptyMap<Int, Long>(),
) {
    private val values =
        values.mapValues { (_, controls) -> controls.immutableCopy() }.immutableCopy()
    private val identities = identities.immutableCopy()
    private val eventIdentities = eventIdentities.immutableCopy()
    private val overrides =
        overrides.mapValues { (_, controls) -> controls.immutableCopy() }.immutableCopy()

    fun get(id: Int, key: String): Float {
        val v = values.getValue(id)[key]
        requireNotNull(v) { "Unknown control: " + id + " / " + key }
        return v
    }

    fun identity(id: Int): Long {
        return identities.getValue(id)
    }

    fun with(id: Int, key: String, value: Float): EffectParameters {
        get(id, key)
        val copy: MutableMap<Int, Map<String, Float>> =
            LinkedHashMap<Int, Map<String, Float>>(values)
        val controls: MutableMap<String, Float> = LinkedHashMap<String, Float>(values.get(id))
        controls.put(key, unit(value))
        copy.put(id, controls)
        return EffectParameters(copy, identities, overrides, eventIdentities)
    }

    fun reseed(id: Int, seed: Long): EffectParameters {
        val copy: MutableMap<Int, Long> = LinkedHashMap<Int, Long>(identities)
        require(copy.containsKey(id)) { "Fault ID" }
        copy.put(id, seed)
        return EffectParameters(values, copy, overrides, eventIdentities)
    }

    fun reset(id: Int): EffectParameters {
        var result = clearOverrides(id)
        for (c in Effects.CONTROLS[id]) result = result.with(id, c.key, c.initial)
        return result
    }

    fun fixedEventIdentity(id: Int): Boolean {
        return eventIdentities.containsKey(id)
    }

    fun eventIdentity(id: Int, fallback: Long): Long {
        return eventIdentities.getOrDefault(id, fallback)
    }

    fun withEventIdentity(id: Int, value: Long?): EffectParameters {
        require(FaultParameters.incidents(id)) { "No event generator" }
        val copy: MutableMap<Int, Long> = LinkedHashMap<Int, Long>(eventIdentities)
        if (value == null) copy.remove(id) else copy.put(id, value)
        return EffectParameters(values, identities, overrides, copy)
    }

    fun overrides(id: Int): Map<String, Float> {
        return overrides.getOrDefault(id, mutableMapOf<String, Float>())
    }

    fun manual(id: Int, key: String): Boolean {
        return overrides(id).containsKey(key)
    }

    fun resolved(id: Int, key: String, automatic: Float): Float {
        return overrides(id).getOrDefault(key, automatic)
    }

    fun override(id: Int, key: String, value: Float): EffectParameters {
        FaultParameters.spec(id, key).validate(value)
        val copy: MutableMap<Int, Map<String, Float>> =
            LinkedHashMap<Int, Map<String, Float>>(overrides)
        val controls: MutableMap<String, Float> = LinkedHashMap<String, Float>(overrides(id))
        controls.put(key, value)
        copy.put(id, controls)
        return EffectParameters(values, identities, copy, eventIdentities)
    }

    fun automatic(id: Int, key: String): EffectParameters {
        FaultParameters.spec(id, key)
        val copy: MutableMap<Int, Map<String, Float>> =
            LinkedHashMap<Int, Map<String, Float>>(overrides)
        val controls: MutableMap<String, Float> = LinkedHashMap<String, Float>(overrides(id))
        controls.remove(key)
        if (controls.isEmpty()) copy.remove(id) else copy.put(id, controls)
        return EffectParameters(values, identities, copy, eventIdentities)
    }

    fun clearOverrides(id: Int): EffectParameters {
        val copy: MutableMap<Int, Map<String, Float>> =
            LinkedHashMap<Int, Map<String, Float>>(overrides)
        copy.remove(id)
        val events: MutableMap<Int, Long> = LinkedHashMap<Int, Long>(eventIdentities)
        events.remove(id)
        return EffectParameters(values, identities, copy, events)
    }

    fun encode(): String {
        val s = StringBuilder()
        for (id in Effects.ORDER) {
            if (id == 0) continue
            s.append(';').append(id).append(':').append(identity(id))
            for (c in Effects.CONTROLS[id]) s.append(',')
                .append(c.key)
                .append('=')
                .append(get(id, c.key))
            if (fixedEventIdentity(id)) s.append(",@eventIdentity=").append(eventIdentity(id, 0))
            for (entry in TreeMap<String, Float>(overrides(id)).entries) s.append(",@")
                .append(entry.key)
                .append('=')
                .append(entry.value)
        }
        return s.toString()
    }

    fun describe(ids: IntArray): String {
        val s = StringBuilder()
        for (id in ids) {
            s.append(" | ").append(Effects.name(id)).append(" identity=").append(identity(id))
            for (c in Effects.CONTROLS[id]) s.append(' ')
                .append(c.key)
                .append('=')
                .append(get(id, c.key))
            if (fixedEventIdentity(id)) s.append(" eventIdentity=").append(eventIdentity(id, 0))
            if (!overrides(id).isEmpty()) s.append(" overrides=").append(overrides(id))
        }
        return s.toString()
    }

    companion object {
        fun unit(value: Float): Float {
            return if (value.isFinite()) max(0f, min(1f, value)) else 0f
        }

        fun defaults(): EffectParameters {
            val values: MutableMap<Int, MutableMap<String, Float>> =
                LinkedHashMap<Int, MutableMap<String, Float>>()
            val identities: MutableMap<Int, Long> = LinkedHashMap<Int, Long>()
            for (id in Effects.ORDER) {
                val controls: MutableMap<String, Float> = LinkedHashMap<String, Float>()
                for (c in Effects.CONTROLS[id]) controls.put(c.key, c.initial)
                values.put(id, controls)
                identities.put(id, 0x51a1L + id * 1000003L)
            }
            return EffectParameters(values, identities)
        }

        fun decode(text: String): EffectParameters {
            var p: EffectParameters = defaults()
            val seen: MutableSet<Int> = HashSet<Int>()
            for (group in text.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (group.isEmpty()) continue
                val entries =
                    group.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val head =
                    entries[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                require(head.size == 2) { "Fault identity" }
                val id = head[0].toInt()
                require(!(id <= 0 || id >= Effects.NAMES.size || !seen.add(id))) { "Fault ID" }
                p = p.reseed(id, head[1].toLong())
                val keys: MutableSet<String> = HashSet<String>()
                for (i in 1..<entries.size) {
                    val pair =
                        entries[i]
                            .split("=".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    require(!(pair.size != 2 || !keys.add(pair[0]))) { "Fault control" }
                    if (pair[0] == "@eventIdentity") {
                        p = p.withEventIdentity(id, pair[1].toLong())
                        continue
                    }
                    val value = pair[1].toFloat()
                    if (pair[0].startsWith("@")) {
                        p = p.override(id, pair[0].substring(1), value)
                    } else {
                        require(!(!value.isFinite() || value < 0 || value > 1)) { "Control bounds" }
                        p = p.with(id, pair[0], value)
                    }
                }
                require(keys.count { !it.startsWith("@") } == Effects.CONTROLS[id].size) {
                    "Missing controls"
                }
            }
            require((1..Effects.CRT).all { it in seen }) { "Missing legacy faults" }
            return p
        }
    }
}
