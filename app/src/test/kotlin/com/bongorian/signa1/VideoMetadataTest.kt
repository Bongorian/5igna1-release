package com.bongorian.signa1

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class VideoMetadataTest {
    @Test fun keepsMediaBytesAndReadsUnicodeAcrossExtendedBoxes() {
        val file=File.createTempFile("video-metadata", ".mp4")
        try {
            val original=ByteBuffer.allocate(28).putInt(12).putInt(0x66747970).putInt(0)
                .putInt(1).putInt(0x6d646174).putLong(16).array()
            file.writeBytes(original)
            RandomAccessFile(file,"rw").use { f ->
                assertNull(VideoMetadata.read(f.channel))
                val description="5igna1 1.6.2 | CLEAN | LEVEL=0.42 | 録画開始時 → 初期値"
                VideoMetadata.append(f.channel,description)
                assertEquals(description,VideoMetadata.read(f.channel))
                assertArrayEquals(original,file.readBytes().copyOf(original.size))
            }
        } finally { file.delete() }
    }
    @Test fun rejectsTruncatedAndOpenEndedFilesWithoutChangingThem() {
        val file=File.createTempFile("video-metadata", ".mp4")
        try {
            for (size in listOf(0,4,100)) {
                val original=ByteBuffer.allocate(8).putInt(size).putInt(0x6d646174).array()
                file.writeBytes(original)
                RandomAccessFile(file,"rw").use { f ->
                    assertTrue(runCatching {VideoMetadata.append(f.channel,"signal")}.isFailure)
                }
                assertArrayEquals(original,file.readBytes())
            }
        } finally {file.delete()}
    }
}
