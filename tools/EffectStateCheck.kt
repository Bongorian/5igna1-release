package com.bongorian.signa1

import java.util.*

object EffectStateCheck {
    internal fun check(ok: Boolean, label: String) {
        if (!ok) throw AssertionError(label)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val p = EffectParameters.defaults().with(Effects.VHS, "tracking", .2f)
        val state =
            EffectState.create(
                Effects.CLEAN,
                (1 shl Effects.PIXEL_DAMAGE) or
                    (1 shl Effects.ROW_ERROR) or
                    (1 shl Effects.STREAM_ERROR),
                1f,
                p,
            )
        check(
            Effects.CONTROLS[Effects.VHS].size != Effects.CONTROLS[Effects.DEMOSAIC_ERROR].size,
            "variable control counts",
        )
        val changed = p.with(Effects.VHS, "tracking", 1f)
        check(
            p.get(Effects.VHS, "tracking") == .2f && changed.get(Effects.VHS, "tracking") == 1f,
            "immutable controls",
        )
        val identity = p.identity(Effects.ROW_ERROR)
        val other = p.reseed(Effects.ROW_ERROR, java.lang.Long.MIN_VALUE)
        check(
            p.identity(Effects.ROW_ERROR) == identity &&
                other.identity(Effects.ROW_ERROR) == java.lang.Long.MIN_VALUE,
            "long identity and immutable reseed",
        )
        check(
            other.get(Effects.ROW_ERROR, "displacement") ==
                p.get(Effects.ROW_ERROR, "displacement"),
            "reseed keeps controls",
        )
        check(other.identity(Effects.VHS) == p.identity(Effects.VHS), "reseed isolates faults")
        check(state.single(Effects.CLEAN).ids().size == 0, "CLEAN empty route")
        check(state.single(Effects.CRT).ids().size == 1, "single selection")
        check(
            Arrays.equals(state.forContext(false, 0).ids(), state.forContext(true, 0).ids()),
            "photo and video share STREAM ERROR",
        )
        check(
            Arrays.equals(
                state.forContext(false, 2).ids(),
                intArrayOf(Effects.PIXEL_DAMAGE, Effects.ROW_ERROR),
            ),
            "RAW representation capability",
        )
        check(
            state.snapshot(false, 1).ids().size == 0 && state.forContext(false, 1) == state,
            "RAW original tap remembers settings",
        )
        val frame = state.snapshot(true, 0)
        val ids = frame.ids()
        ids[0] = Effects.CRT
        check(frame.ids()[0] == Effects.PIXEL_DAMAGE, "immutable snapshot route")
        check(
            EffectState.decode(state.encode()).encode() == state.encode(),
            "named schema roundtrip",
        )
        check(
            EffectState.decode(state.edit(true, state.mask, other).encode())
                .parameters()
                .identity(Effects.ROW_ERROR) == java.lang.Long.MIN_VALUE,
            "identity roundtrip",
        )
        for (invalid in
            arrayOf<String>(
                "",
                "2|0|0|.5",
                "3|0|0|NaN|" + p.encode(),
                state.encode().replace("tracking=0.2", "tracking=NaN"),
                state.encode().replace("tracking=0.2", "unknown=0.2"),
            )) try {
            EffectState.decode(invalid)
            throw AssertionError("invalid settings accepted")
        } catch (expected: IllegalArgumentException) {}

        try {
            p.with(Effects.VHS, "strength", .5f)
            throw AssertionError("universal physical strength accepted")
        } catch (expected: IllegalArgumentException) {}

        try {
            EffectState.Frame(
                intArrayOf(Effects.CRT, Effects.VHS),
                1f,
                p,
                0,
                0.0,
                emptyList<FaultNode>(),
            )
            throw AssertionError("Arbitrary stack accepted")
        } catch (expected: IllegalArgumentException) {}

        check(state.chain(1).mask == 0, "CLEAN cannot become a pass")
        println(
            "PASS named settings, immutable route, identity isolation, schema reset and shared photo/video capabilities"
        )
    }
}
