package com.bongorian.signa1

/** Display-only budget. Camera input and saved output sizes are never changed here. */
internal object PreviewSizing {
    data class Size(val width: Int, val height: Int)

    fun choose(sourceW: Int, sourceH: Int, viewW: Int, viewH: Int,
               light: Boolean, recordingRgb: Boolean, retainedNetworkFrame: Boolean): Size {
        require(sourceW > 0 && sourceH > 0)
        if (!light || recordingRgb || retainedNetworkFrame || viewW <= 0 || viewH <= 0)
            return Size(sourceW, sourceH)
        val scale = minOf(1.0, viewW.toDouble() / sourceW, viewH.toDouble() / sourceH)
        return Size(maxOf(1, (sourceW * scale).toInt()), maxOf(1, (sourceH * scale).toInt()))
    }
}
