package com.bongorian.signa1

import java.util.Arrays
import java.util.Collections
import java.util.function.IntPredicate

/** A committed route and its controls. Rendering never mutates settings. */
internal class EffectState
private constructor(
    chained: Boolean,
    mask: Int,
    amount: Float,
    parameters: EffectParameters?,
) {
    val chained: Boolean
    val mask: Int
    val amount: Float
    private val parameters: EffectParameters

    init {
        this.mask = mask and VALID_MASK
        this.chained = chained && this.mask != 0
        require(!(!this.chained && Integer.bitCount(this.mask) > 1)) {
            "Single fault has multiple stages"
        }
        this.amount = EffectParameters.unit(amount)
        this.parameters = if (parameters == null) EffectParameters.defaults() else parameters
    }

    fun selected(): Int {
        for (id in Effects.ORDER) if (enabled(id)) return id
        return Effects.CLEAN
    }

    fun ids(): IntArray {
        return Effects.ordered(mask, false)
    }

    fun enabled(id: Int): Boolean {
        return (mask and bit(id)) != 0
    }

    fun parameters(): EffectParameters {
        return parameters
    }

    fun single(id: Int): EffectState {
        return EffectState(false, bit(id), amount, parameters)
    }

    fun chain(mask: Int): EffectState {
        return EffectState(true, mask, amount, parameters)
    }

    fun amount(value: Float): EffectState {
        return EffectState(chained, mask, value, parameters)
    }

    fun edit(chain: Boolean, mask: Int, p: EffectParameters?): EffectState {
        return EffectState(chain, mask, amount, p)
    }

    fun forContext(video: Boolean, format: Int): EffectState {
        if (!video && format == 1) return this
        var allowed = 0
        for (id in ids()!!) if (Effects.available(id, video, !video && format == 2))
            allowed = allowed or bit(id)
        return if (allowed == mask) this else EffectState(chained, allowed, amount, parameters)
    }

    fun snapshot(video: Boolean, format: Int): Frame {
        val valid = forContext(video, format)
        return EffectState.Frame(
            (if (!video && format == 1) IntArray(0) else valid.ids())!!,
            amount,
            parameters,
            0,
            0.0,
            mutableListOf<FaultNode>(),
        )
    }

    internal class Frame(
        ids: IntArray,
        amount: Float,
        parameters: EffectParameters,
        cameraNs: Long,
        time: Double,
        nodes: List<FaultNode>,
        val experimental: Boolean = false,
        val injection: Boolean = false,
        val deliveryNs: Long = cameraNs,
        val sourceEpoch: Long = 0,
        val clockVersion: Int = 0,
    ) {
        private val route: IntArray
        val amount: Float
        val parameters: EffectParameters
        val cameraNs: Long
        val time: Double
        val nodes: List<FaultNode>

        init {
            var previous = 0
            for (id in ids) {
                require(!(Effects.rank(id) <= previous || id >= Effects.NAMES.size)) { "Non-causal fault route" }
                previous = Effects.rank(id)
            }
            previous = 0
            for (n in nodes) {
                require(
                    !(Effects.rank(n.id) <= previous || n.id !in ids)
                ) {
                    "Fault node outside route"
                }
                previous = Effects.rank(n.id)
            }
            this.route = ids.clone()
            this.amount = amount
            this.parameters = parameters
            this.cameraNs = cameraNs
            this.time = time
            this.nodes = Collections.unmodifiableList<FaultNode>(ArrayList<FaultNode>(nodes))
        }

        fun ids(): IntArray {
            return route.clone()
        }

        // Recordable causal prefix; profile/representation after the chosen point is explicit.
        fun through(point: Effects.Point): Frame {
            val prefix =
                Arrays.stream(route)
                    .filter(IntPredicate { id: Int -> Effects.point(id).ordinal <= point.ordinal })
                    .toArray()
            val selected: MutableList<FaultNode> = ArrayList<FaultNode>()
            for (n in nodes) if (Effects.point(n.id).ordinal <= point.ordinal) selected.add(n)
            return Frame(prefix, amount, parameters, cameraNs, time, selected, experimental, injection, deliveryNs, sourceEpoch, clockVersion)
        }

        fun afterReadout(): Frame {
            val suffix = route.filter { Effects.point(it).ordinal > Effects.Point.READOUT.ordinal }.toIntArray()
            return Frame(suffix, amount, parameters, cameraNs, time,
                nodes.filter { Effects.point(it.id).ordinal > Effects.Point.READOUT.ordinal }, experimental, true, deliveryNs, sourceEpoch, clockVersion)
        }

        fun withSourceEpoch(epoch: Long): Frame = Frame(route, amount, parameters, cameraNs, time, nodes,
            experimental, injection, deliveryNs, epoch, clockVersion)

        fun withContentTimestamp(stamp: Long): Frame = Frame(route, amount, parameters, stamp, time, nodes,
            experimental, injection, deliveryNs, sourceEpoch, clockVersion)

        fun deliveredAt(current: Frame): Frame = Frame(route, amount, parameters, cameraNs, time, nodes,
            experimental, injection, current.deliveryNs, current.sourceEpoch, clockVersion)

        fun describe(): String {
            val s =
                StringBuilder("cameraNs=")
                    .append(cameraNs)
                    .append(" t=")
                    .append(time)
                    .append(" LEVEL=")
                    .append(amount)
            if (clockVersion > 0) s.append(" clockModel=").append(clockVersion)
                .append(" deliveryNs=").append(deliveryNs).append(" sourceEpoch=").append(sourceEpoch)
            s.append(parameters.describe(route))
            if (experimental || route.any { id -> FaultSensitivity.keys.any { parameters.manual(id, it) } })
                s.append(" experimental=").append(experimental)
            if (injection) s.append(" input=TAP@READOUT/DATA")
            for (n in nodes) s.append(" | ").append(n.describe())
            return s.toString()
        }
    }

    fun encode(): String {
        return "3|" +
            (if (chained) 1 else 0) +
            "|" +
            mask +
            "|" +
            amount +
            "|" +
            parameters.encode()
    }

    companion object {
        private val VALID_MASK = ((1 shl Effects.NAMES.size) - 1) and 1.inv()

        fun defaults(): EffectState {
            return EffectState(false, 0, .55f, null)
        }

        fun create(
            selected: Int,
            mask: Int,
            level: Float,
            parameters: EffectParameters?,
        ): EffectState {
            val valid = mask and VALID_MASK
            return EffectState(
                valid != 0,
                if (valid != 0) valid else bit(selected),
                level,
                parameters,
            )
        }

        private fun bit(id: Int): Int {
            return if (id > 0 && id < Effects.NAMES.size) 1 shl id else 0
        }

        fun decode(text: String): EffectState {
            val p: Array<String> = text.split("\\|".toRegex()).toTypedArray()
            require(!(p.size != 5 || (p[0] != "3") || !(p[1] == "0" || p[1] == "1"))) {
                "Fault schema"
            }
            val mask = p[2]!!.toInt()
            val level = p[3]!!.toFloat()
            require(
                !((mask and VALID_MASK.inv()) != 0 ||
                    !java.lang.Float.isFinite(level) ||
                    level < 0 ||
                    level > 1)
            ) {
                "Fault settings"
            }
            return EffectState(p[1] == "1", mask, level, EffectParameters.decode(p[4]))
        }
    }
}
