package com.bongorian.signa1

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.UUID

/** One immutable recording-start description in an ISO BMFF user-extension box.
 * Appended only to a finished, unpublished recording, after any audio remux.
 * Existing media bytes and chunk offsets stay intact. No timed state is recorded.
 */
internal object VideoMetadata {
    private val identity = UUID.fromString("64be6c7d-605a-48f0-8e18-e835375738a9")
    private const val UUID_BOX = 0x75756964
    private const val LIMIT = 65536

    fun write(context: Context, uri: Uri, description: String) {
        val fd = requireNotNull(context.contentResolver.openFileDescriptor(uri,"rw"))
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { input ->
            ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.dup(fd.fileDescriptor)).use { output ->
                append(output.channel,description,input.channel)
            }
        }
    }

    fun read(context: Context, uri: Uri): SavedSignal? {
        val fd = context.contentResolver.openFileDescriptor(uri,"r") ?: return null
        return ParcelFileDescriptor.AutoCloseInputStream(fd).use { SavedSignal.read(read(it.channel)) }
    }

    internal fun append(channel: FileChannel, description: String, source: FileChannel = channel) {
        val bytes = description.toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..LIMIT)
        val length = channel.size()
        // Validate only box headers: never scan/decode the audio or video payload.
        boxes(source) { _, _, _, toEnd -> require(!toEnd) { "Open-ended MP4 box" } }
        val buffer = ByteBuffer.allocate(28+bytes.size).putInt(28+bytes.size).putInt(UUID_BOX)
            .putLong(identity.mostSignificantBits).putLong(identity.leastSignificantBits)
            .putInt(1).put(bytes).apply { flip() }
        try {
            channel.position(length)
            while (buffer.hasRemaining()) check(channel.write(buffer)>0)
            channel.force(true)
        } catch (error: Exception) {
            channel.truncate(length)
            throw error
        }
    }

    internal fun read(channel: FileChannel): String? {
        var description: String? = null
        boxes(channel) { type, payload, count, _ ->
            if (type == UUID_BOX && count in 21..(20+LIMIT).toLong()) {
                val buffer = readAt(channel,payload,count.toInt())
                if (buffer.long == identity.mostSignificantBits && buffer.long == identity.leastSignificantBits && buffer.int == 1) {
                    val bytes = ByteArray(buffer.remaining()); buffer.get(bytes)
                    description = bytes.toString(Charsets.UTF_8)
                }
            }
        }
        return description
    }

    private fun readAt(channel: FileChannel, offset: Long, size: Int): ByteBuffer {
        val buffer = ByteBuffer.allocate(size)
        while (buffer.hasRemaining()) check(channel.read(buffer,offset+buffer.position())>0) { "Truncated MP4" }
        buffer.flip()
        return buffer
    }

    private fun boxes(channel: FileChannel, visit: (Int,Long,Long,Boolean)->Unit) {
        val length = channel.size()
        require(length >= 8)
        var offset = 0L
        var count = 0
        while (offset < length) {
            require(++count <= 100000 && length-offset >= 8)
            val header = readAt(channel,offset,8)
            val shortSize = header.int.toLong() and 0xffffffffL
            val type = header.int
            val headerSize = if (shortSize == 1L) 16 else 8
            require(length-offset >= headerSize)
            val size = when (shortSize) {
                0L -> length-offset
                1L -> readAt(channel,offset+8,8).long
                else -> shortSize
            }
            require(size >= headerSize && size <= length-offset) { "Invalid MP4 box size" }
            visit(type,offset+headerSize,size-headerSize,shortSize == 0L)
            offset += size
        }
    }
}
