package com.bongorian.signa1

import java.util.Collections
import java.util.Random
import kotlin.math.min

/** Random routes use only the current capture format's available faults. */
internal object EffectRandomizer {
    fun supportsSeed(id: Int, parameters: EffectParameters): Boolean {
        if (id in intArrayOf(Effects.CLEAN, Effects.COLOR_MAP, Effects.MOTION_BLUR, Effects.SMEAR)) return false
        if (id == Effects.VHS || id == Effects.CRT) {
            val kind = parameters.transportKind(id)
            if (kind == (if (id == Effects.VHS) 2 else 1)) return false
        }
        return true
    }

    fun reseed(base: EffectState, available: IntArray, random: Random): EffectState {
        var parameters = base.parameters()
        for (id in base.ids()) if (id in available && supportsSeed(id, parameters)) {
            val before = parameters.identity(id)
            val candidate = random.nextLong()
            parameters = parameters.reseed(id, if (candidate == before) candidate xor 1L else candidate)
        }
        return base.edit(base.chained, base.mask, parameters)
    }

    fun chain(base: EffectState, available: IntArray, random: Random): EffectState {
        val choices: MutableList<Int> = ArrayList<Int>()
        for (id in available) if (
            id > Effects.CLEAN && id < Effects.NAMES.size && !choices.contains(id)
        )
            choices.add(id)
        Collections.shuffle(choices, random)
        if (choices.isEmpty()) return base.single(Effects.CLEAN)
        val minimum = min(2, choices.size)
        val maximum = min(5, choices.size)
        val count = minimum + random.nextInt(maximum - minimum + 1)
        var mask = 0
        var parameters = base.parameters()
        for (i in 0..<count) {
            val id: Int = choices.get(i)
            mask = mask or (1 shl id)
            parameters = parameters.clearOverrides(id).reseed(id, random.nextLong())
            for (control in Effects.CONTROLS[id]) parameters =
                parameters.with(id, control.key, random.nextFloat())
        }
        return base.edit(true, mask, parameters).amount(.35f + random.nextFloat() * .65f)
    }
}
