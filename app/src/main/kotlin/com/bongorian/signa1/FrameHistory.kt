package com.bongorian.signa1

import java.util.Collections
import java.util.function.Supplier

/**
 * Bounded handoff from GL submission to UI acknowledgement and shutter reservation. Payloads are
 * writable only while acquired by GL. A shutter pins the acknowledged payload synchronously BEFORE
 * posting work to GL, so queued camera callbacks cannot overwrite it.
 */
internal class FrameHistory<T : Any>(capacity: Int, factory: Supplier<T>) {
    internal class Slot<T : Any>(val value: T) {
        internal var timestamp: Long = 0
        internal var readers = 0
        internal var writing = false
    }

    internal class Lease<T : Any>(internal val slot: Slot<T>, internal val epoch: Long) {
        val value: T
        val timestamp: Long
        internal var released = false

        init {
            value = slot.value
            timestamp = slot.timestamp
        }
    }

    internal val slots: MutableList<Slot<T>> = ArrayList<Slot<T>>()
    private var acknowledged: Long = 0
    private var epoch: Long = 0

    init {
        require(capacity >= 2) { "Frame capacity" }
        for (i in 0..<capacity) slots.add(Slot<T>(factory.get()))
    }

    @Synchronized
    fun acquire(): Slot<T>? {
        for (slot in slots) if (
            !slot.writing &&
                slot.readers == 0 &&
                (slot.timestamp == 0L || slot.timestamp < acknowledged)
        ) {
            slot.writing = true
            slot.timestamp = 0
            return slot
        }
        return null
    }

    @Synchronized
    fun publish(slot: Slot<T>, timestamp: Long) {
        check(!(!slot.writing || timestamp <= 0)) { "Frame publication" }
        slot.timestamp = timestamp
        slot.writing = false
    }

    @Synchronized
    fun abandon(slot: Slot<T>) {
        slot.writing = false
        slot.timestamp = 0
    }

    @Synchronized
    fun acknowledge(timestamp: Long): Boolean {
        if (timestamp <= 0) return false
        for (slot in slots) if (
            !slot.writing && slot.timestamp == timestamp && timestamp >= acknowledged
        ) {
            acknowledged = timestamp
            return true
        }
        return false
    }

    @Synchronized
    fun reserve(): Lease<T>? {
        return reserve(acknowledged)
    }

    @Synchronized
    fun reserve(timestamp: Long): Lease<T>? {
        if (timestamp <= 0) return null
        for (slot in slots) if (!slot.writing && slot.timestamp == timestamp) {
            slot.readers++
            return Lease<T>(slot, epoch)
        }
        return null
    }

    @Synchronized
    fun valid(lease: Lease<T>?): Boolean {
        return lease != null &&
            !lease.released &&
            lease.epoch == epoch &&
            lease.slot.timestamp == lease.timestamp
    }

    @Synchronized
    fun release(lease: Lease<T>?) {
        if (lease != null && !lease.released) {
            lease.released = true
            lease.slot.readers--
        }
    }

    @Synchronized
    fun acknowledged(): Long {
        return acknowledged
    }

    @Synchronized
    fun clear() {
        epoch++
        acknowledged = 0
        for (slot in slots) {
            slot.timestamp = 0
            slot.writing = false
        }
    }

    fun values(): MutableList<T> {
        val values: MutableList<T> = ArrayList<T>()
        for (slot in slots) values.add(slot.value)
        return Collections.unmodifiableList<T>(values)
    }
}
