package com.bongorian.signa1

/** GL-thread-only pre-FAULT history. At most 33 RGBA frames, 480 pixels on the long side
 * (under 30 MiB even for square input). Audio and the FAULT clock remain in the present. */
internal class TimeEcho {
    internal class Sample(val buffer: SignalBuffer = SignalBuffer(), var cameraNs: Long = 0)
    private val samples = Array(33) { Sample() }
    private var cursor = 0
    private var lastStored = 0L
    private var start = 0L
    private var nextAuto = 0L
    private var requested = false
    @Volatile var failed = false
        private set
    private var allocated = false
    @Volatile var ready = false
        private set
    @Volatile var replaying = false
        private set
    @Volatile var bursts = 0
        private set
    fun trigger() { requested = true }
    fun reset() {
        samples.forEach { it.cameraNs = 0 }
        cursor = 0
        lastStored = 0
        start = 0
        nextAuto = 0
        requested = false
        replaying = false
        ready = false
        failed = false
    }
    fun release() {
        if (allocated) samples.forEach { it.buffer.release() }
        allocated = false
        reset()
    }
    fun disable() { if (allocated || requested || failed) release() }
    fun sample(now: Long, config: EchoConfig, w: Int, h: Int,
               write: (SignalBuffer) -> Unit): Sample? {
        if (failed) return null
        if (lastStored == 0L || now - lastStored >= 250_000_000L) {
            val sample = samples[cursor]
            val scale = 480.0 / maxOf(w, h)
            allocated = true
            sample.buffer.allocate(maxOf(1, (w * scale).toInt()), maxOf(1, (h * scale).toInt()))
            write(sample.buffer)
            sample.cameraNs = now
            lastStored = now
            cursor = (cursor + 1) % samples.size
        }
        if (nextAuto == 0L) nextAuto = now + 12_000_000_000L
        val target = now - config.delayNs
        var past: Sample? = null
        for (sample in samples) if (sample.cameraNs > 0 && sample.cameraNs <= target &&
            (past == null || sample.cameraNs > past.cameraNs)) past = sample
        ready = past != null && target - past.cameraNs < 750_000_000L
        if (start != 0L && now - start >= 2_000_000_000L) start = 0
        if (start == 0L && (requested || now >= nextAuto) && ready) {
            start = now
            bursts++
            nextAuto = now + 12_000_000_000L
        }
        requested = false
        replaying = start != 0L && ready
        return if (replaying) past else null
    }
    fun fail() { release(); failed = true }
}
