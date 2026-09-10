package com.bongorian.signa1

import android.graphics.ImageDecoder
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.SystemClock
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.util.concurrent.Future
import kotlin.math.sqrt

internal data class TapInput(val uri: Uri, val video: Boolean)

/** Owned imported RGB source. Output recording follows preview transport only in one direction. */
internal class TapSource(private val engine: GlitchEngine, val input: TapInput) {
    var texture = 0
        private set
    val external get() = input.video
    val matrix = GlitchEngine.IDENTITY.clone()
    @Volatile var ready = false
        private set
    @Volatile var playing = false
        private set
    @Volatile var hasAudio = false
        private set
    @Volatile var ended = false
        private set
    private var audioTimeline: TapAudioTimeline? = null
    var audioSession: TapAudio.Session? = null
        private set
    var width = 0
        private set
    var height = 0
        private set
    @Volatile private var closed = false
    private var available = false
    private var player: MediaPlayer? = null
    private var stream: SurfaceTexture? = null
    private var surface: Surface? = null
    private var load: Future<*>? = null
    private val playback = TapPlayback()

    private fun createTexture() {
        val id = IntArray(1)
        GLES20.glGenTextures(1, id, 0)
        texture = id[0]
        val target = if (external) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D
        GLES20.glBindTexture(target, texture)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    fun open() {
        createTexture()
        if (!input.video) {
            load = engine.files.submit {
                try {
                    engine.foregroundWork.await()
                    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(engine.context.contentResolver, input.uri)) {
                            decoder, info, _ ->
                        val scale = minOf(1.0, sqrt(engine.deviceProfile.photoPixels().toDouble() /
                            (info.size.width.toDouble() * info.size.height)))
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()),
                            maxOf(1, (info.size.height * scale).toInt()))
                    }
                    engine.gl.post {
                        try {
                            if (!closed) {
                                engine.current(engine.window)
                                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
                                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                                check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "TAP texture allocation" }
                                width = bitmap.width
                                height = bitmap.height
                                matrix[5] = -1f
                                matrix[13] = 1f
                                ready = true
                                engine.tapPrepared(this)
                            }
                        } catch (error: Exception) { fail(error) }
                        finally { bitmap.recycle() }
                    }
                } catch (error: Exception) { engine.gl.post { if (!closed) fail(error) } }
                catch (error: OutOfMemoryError) {
                    engine.gl.post { if (!closed) fail(IllegalStateException("TAP image memory", error)) }
                }
            }
            return
        }
        stream = SurfaceTexture(texture).also {
            it.setOnFrameAvailableListener({ if (!closed) available = true }, engine.gl)
        }
        surface = Surface(stream)
        player = MediaPlayer().also { media ->
            media.setDataSource(engine.context, input.uri)
            media.setSurface(surface)
            // TAP is an image signal at the readout/data boundary. It does not import sound.
            media.setVolume(0f, 0f)
            media.isLooping = false
            media.setOnCompletionListener {
                if (!closed && engine.tapSource === this) {
                    audioTimeline?.playback(false,media.currentPosition.toLong()*1000,SystemClock.elapsedRealtimeNanos()/1000)
                    playing = false
                    ended = true
                    // Flush the last available video frame before finalizing the recorder.
                    if (engine.recording) { engine.frame(); engine.stopVideo() }
                }
            }
            media.setOnVideoSizeChangedListener { _, w, h ->
                if (!closed && w > 0 && h > 0) { width = w; height = h }
            }
            media.setOnPreparedListener {
                if (!closed) {
                    width = media.videoWidth
                    height = media.videoHeight
                    hasAudio = media.trackInfo.any { it.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO }
                    media.seekTo(0L, MediaPlayer.SEEK_CLOSEST_SYNC)
                }
            }
            media.setOnSeekCompleteListener {
                if (!closed && !ready && width > 0 && height > 0) {
                    ready = true
                    engine.tapPrepared(this)
                }
            }
            media.setOnErrorListener { _, what, extra ->
                if (!closed) fail(IllegalStateException("TAP video $what/$extra"))
                true
            }
            media.prepareAsync()
        }
    }

    fun update() {
        if (external && available) {
            stream!!.updateTexImage()
            stream!!.getTransformMatrix(matrix)
            available = false
        }
    }

    fun toggle() {
        if (!ready || !input.video || closed) return
        val next = !playing
        playback.manual(next)
        play(next)
    }

    fun recordingStarted() {
        if (!ready || !input.video || closed) return
        if (hasAudio) {
            audioTimeline = TapAudioTimeline()
            audioSession = TapAudio.Session(input.uri,
                if (engine.settings.resolutionAudio) RecordingAudio.supported(engine.outW,engine.outH,true) else null)
            audioTimeline!!.playback(playing,positionUs(),SystemClock.elapsedRealtimeNanos()/1000)
        }
        play(playback.recordingStarted())
    }

    fun recordingFrame(timestampNs: Long) { audioTimeline?.firstFrame(timestampNs/1000) }
    fun recordedAudio(): TapAudio.Job? = audioSession?.let {
        TapAudio.Job(it,audioTimeline!!.snapshot(SystemClock.elapsedRealtimeNanos()/1000))
    }
    private fun positionUs() = if (ended) 0L else (player?.currentPosition ?: 0).toLong()*1000

    fun recordingStopped() {
        playback.recordingStopped()
        if (ready && input.video && !closed) play(false)
    }

    private fun play(value: Boolean) {
        if (playing == value) return
        try {
            val position = positionUs()
            if (value) {
                player!!.start() // PlaybackCompleted restarts at the beginning.
                ended = false
            } else player!!.pause()
            playing = value
            audioTimeline?.playback(value,position,SystemClock.elapsedRealtimeNanos()/1000)
        } catch (error: Exception) { fail(error) }
    }

    private fun fail(error: Exception) {
        if (engine.recording) engine.stopVideo()
        close()
        engine.error(engine.context.getString(R.string.tap_failed), error)
        engine.ready(false)
    }

    fun close() {
        if (closed) return
        closed = true
        load?.cancel(true)
        player?.release()
        player = null
        surface?.release()
        surface = null
        stream?.setOnFrameAvailableListener(null)
        stream?.release()
        stream = null
        if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
        texture = 0
        ready = false
        playing = false
    }
}
