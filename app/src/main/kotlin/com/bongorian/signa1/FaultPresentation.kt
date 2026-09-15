package com.bongorian.signa1

import android.content.Context

/** Shared plain-language descriptions for selection, adjustment and inspection. */
internal object FaultPresentation {
    private val descriptions = intArrayOf(
        R.string.fault_description_0,
        R.string.fault_description_1,
        R.string.fault_description_2,
        R.string.fault_description_3,
        R.string.fault_description_4,
        R.string.fault_description_5,
        R.string.fault_description_6,
        R.string.fault_description_7,
        R.string.fault_description_8,
        R.string.fault_description_9,
        R.string.fault_description_10,
        R.string.fault_description_11,
        R.string.fault_description_12,
        R.string.fault_description_13,
        R.string.fault_description_14,
        R.string.fault_description_15,
        R.string.fault_description_16
    )
    fun description(context: Context, id: Int): String = context.getString(descriptions[id])
}
