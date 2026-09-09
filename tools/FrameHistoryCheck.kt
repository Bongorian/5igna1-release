package com.bongorian.signa1

/* Independent schedule fixtures for the exact displayed-frame capture contract. */
object FrameHistoryCheck {
    internal fun check(ok: Boolean, label: String) {
        if (!ok) throw AssertionError(label)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val history = FrameHistory<IntArray>(3, { IntArray(1) })
        val first = requireNotNull(history.acquire())
        first.value[0] = 10
        history.publish(first, 100)
        check(history.reserve() == null, "submission is not display acknowledgement")
        check(history.acknowledge(100), "first displayed frame")
        val shutter = requireNotNull(history.reserve(100))
        val second = requireNotNull(history.acquire())
        second.value[0] = 20
        history.publish(second, 200)
        history.acknowledge(200)
        val third = requireNotNull(history.acquire())
        third.value[0] = 30
        history.publish(third, 300)
        check(history.acquire() == null, "bounded backpressure while UI and shutter retain images")
        check(
            shutter.value[0] == 10 && shutter.timestamp == 100L,
            "queued camera frames cannot replace shutter image",
        )
        check(!history.acknowledge(99), "stale UI acknowledgement rejected")
        check(history.valid(shutter), "old displayed frame is still pinned")
        history.release(shutter)
        check(history.acquire() == first, "released older buffer is reusable")
        history.abandon(first)
        val last = requireNotNull(history.reserve(200))
        history.clear()
        check(
            !history.valid(last) && history.reserve() == null,
            "camera lifecycle invalidates captures",
        )
        history.release(last)
        check(history.acquire() != null, "camera restart recovers buffers")
        println(
            "PASS UI-acknowledged frames, synchronous shutter pin, bounded backpressure and lifecycle invalidation"
        )
    }
}
