package com.bongorian.signa1

/** Explicit preview transport can override the next recording start; stopping always pauses. */
internal class TapPlayback {
    private var nextStart: Boolean? = null
    fun manual(playing: Boolean) { nextStart = playing }
    fun recordingStarted(): Boolean = (nextStart ?: true).also { nextStart = null }
    fun recordingStopped() { nextStart = null }
}
