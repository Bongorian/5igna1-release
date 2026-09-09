package com.bongorian.signa1

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.util.Size
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/** Compare the two DNG input adapters without saving photographs or retaining camera images. */
internal object DngBufferChecks {
    private class Sink : OutputStream() {
        val digest = MessageDigest.getInstance("SHA-256")
        var bytes = 0L
        override fun write(value: Int) { digest.update(value.toByte()); bytes++ }
        override fun write(data: ByteArray, offset: Int, count: Int) {
            digest.update(data, offset, count)
            bytes += count
        }
    }

    fun run(engine: GlitchEngine): String {
        val characteristics = arrayOfNulls<CameraCharacteristics>(1)
        val metadata = arrayOfNulls<TotalCaptureResult>(1)
        val dimensions = arrayOfNulls<Size>(1)
        val ready = CountDownLatch(1)
        engine.gl.post {
            characteristics[0] = engine.characteristics
            metadata[0] = engine.signalMetadata.values.lastOrNull()
            dimensions[0] = engine.options?.raws?.firstOrNull { !it.maximumPixelMode }?.size
            ready.countDown()
        }
        check(ready.await(10, TimeUnit.SECONDS)) { "DNG metadata timeout" }
        val size = requireNotNull(dimensions[0]) { "This device has no default-mode RAW size" }
        val input = ByteArray(size.width * size.height * 2)
        for (index in 0..<input.size / 2) RawGlitch.write(input,index,256+(index*71)%3500)
        val inputHash = MessageDigest.getInstance("SHA-256").digest(input)
        val direct = ByteBuffer.allocateDirect(input.size).order(ByteOrder.nativeOrder())
        direct.put(input).flip()
        val wrapped = ByteBuffer.wrap(input)
        check(wrapped.array() === input)
        val before = Sink()
        val after = Sink()
        DngCreator(requireNotNull(characteristics[0]),requireNotNull(metadata[0])).use { dng ->
            dng.setDescription("Synthetic DNG buffer equivalence check")
            dng.setOrientation(1)
            dng.writeByteBuffer(before,size,direct,0)
            dng.writeByteBuffer(after,size,wrapped,0)
        }
        val expected = before.digest.digest()
        check(before.bytes == after.bytes && expected.contentEquals(after.digest.digest())) {
            "DNG byte-buffer adapters changed output"
        }
        check(inputHash.contentEquals(MessageDigest.getInstance("SHA-256").digest(input))) {
            "DNG writer modified source bytes"
        }
        return JSONObject().put("width",size.width).put("height",size.height)
            .put("inputBytes",input.size).put("dngBytes",before.bytes)
            .put("sha256",expected.joinToString("") { "%02x".format(it) }).toString()
    }
}
