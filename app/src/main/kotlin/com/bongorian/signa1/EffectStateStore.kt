package com.bongorian.signa1

import android.content.SharedPreferences

internal object EffectStateStore {
    const val KEY: String = "fault_state_v3"

    fun load(prefs: SharedPreferences): EffectState {
        // Fault v3 deliberately resets release effect/AUTO settings; camera preferences survive.
        try {
            return EffectState.decode(prefs.getString(KEY, "").orEmpty())
        } catch (invalid: RuntimeException) {
            return EffectState.defaults()
        }
    }

    fun write(prefs: SharedPreferences.Editor, state: EffectState) {
        prefs
            .putString(KEY, state.encode())
            .remove("effect_state_v2")
            .remove("effect_state_v1")
            .remove("effect")
            .remove("chainMask")
            .remove("amount")
        for (n in 0..<17 * 4) prefs.remove("parameter_" + n)
    }
}
