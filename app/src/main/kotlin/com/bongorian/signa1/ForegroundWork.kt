package com.bongorian.signa1

import java.io.FilterOutputStream
import java.io.OutputStream

/** Cooperative pause for image processing and chunked writes while the app is hidden. */
internal class ForegroundWork {
    private val monitor = Object()
    private var paused = false
    private var closed = false
    fun pause() = synchronized(monitor) { paused = true }
    fun resume() = synchronized(monitor) { paused = false; monitor.notifyAll() }
    fun close() = synchronized(monitor) { closed = true; monitor.notifyAll() }
    fun await() = synchronized(monitor) {
        while (paused && !closed) monitor.wait()
        if (closed || Thread.currentThread().isInterrupted) throw InterruptedException("Capture work stopped")
    }
    fun output(stream: OutputStream) = object : FilterOutputStream(stream) {
        override fun write(value: Int) { await(); out.write(value) }
        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            await()
            out.write(bytes, offset, length)
        }
    }
}
