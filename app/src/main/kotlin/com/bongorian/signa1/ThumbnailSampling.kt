package com.bongorian.signa1

/** Power-of-two subsampling, rounding up so odd dimensions also respect the memory bound. */
internal object ThumbnailSampling {
    fun sampleSize(width: Int, height: Int, bound: Int): Int {
        require(width > 0 && height > 0 && bound > 0)
        val longest = maxOf(width, height).toLong()
        var sample = 1
        while ((longest + sample - 1) / sample > bound && sample < (1 shl 30)) sample *= 2
        return sample
    }
}
