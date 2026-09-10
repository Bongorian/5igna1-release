package com.bongorian.signa1

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.SystemClock
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.floor

/** Finalizes imported audio off the GL thread. The recorded video is never re-encoded. */
internal object TapAudio {
    class Session(val source: Uri, val quality: RecordingAudio.Quality?) {
        // Accessed only by the engine's serial file executor, including segmented recordings.
        var consumedUs = 0L
    }
    data class Job(val session: Session, val spans: List<TapAudioTimeline.Span>)

    private fun extractor(context: Context, uri: Uri): MediaExtractor = MediaExtractor().also {
        try { it.setDataSource(context,uri,null) } catch (error: Exception) { it.release(); throw error }
    }
    private fun track(extractor: MediaExtractor, prefix: String): Int =
        (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith(prefix) == true }
            ?: error("Missing $prefix track")

    fun remux(context: Context, video: Uri, job: Job): File {
        val source = extractor(context,job.session.source)
        val picture = try { extractor(context,video) } catch(error: Exception) { source.release();throw error }
        val output = File.createTempFile("tap-audio-", ".mp4",context.cacheDir)
        val pcm = File.createTempFile("tap-pcm-", ".pcm",context.cacheDir)
        val packets = File.createTempFile("tap-aac-", ".packets",context.cacheDir)
        var mux: MediaMuxer? = null
        var succeeded = false
        try {
            val videoIndex = track(picture,"video/")
            val audioIndex = track(source,"audio/")
            val videoFormat = picture.getTrackFormat(videoIndex)
            val audioFormat = source.getTrackFormat(audioIndex)
            val duration = videoFormat.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1)
            val start = job.session.consumedUs
            job.session.consumedUs += duration
            val spans = job.spans.mapNotNull { it.clipped(start,start+duration) }
            val writer = MediaMuxer(output.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            mux = writer
            MediaMetadataRetriever().use { metadata ->
                metadata.setDataSource(context,video)
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()?.let { writer.setOrientationHint(it) }
                val location = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                Regex("([+-][0-9.]+)([+-][0-9.]+)").find(location.orEmpty())?.let {
                    writer.setLocation(it.groupValues[1].toFloat(),it.groupValues[2].toFloat())
                }
            }
            val videoTrack = writer.addTrack(videoFormat)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            val copyable = mime in setOf("audio/mp4a-latm","audio/3gpp","audio/amr-wb","audio/opus")
            // Codec passthrough preserves source samples. Pauses produce gaps in the audio timeline.
            if (job.session.quality == null && copyable && spans.isNotEmpty()) {
                val audioTrack = writer.addTrack(audioFormat)
                writer.start()
                source.selectTrack(audioIndex)
                var lastUs = -1L
                for (span in spans) {
                    source.seekTo(span.sourceStartUs,MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    val end = span.sourceStartUs+span.outputEndUs-span.outputStartUs
                    val info = MediaCodec.BufferInfo()
                    var buffer = ByteBuffer.allocateDirect(65536)
                    while (source.sampleTrackIndex >= 0 && source.sampleTime < end) {
                        val sourceUs = source.sampleTime
                        if (sourceUs >= span.sourceStartUs) {
                            buffer = capacity(buffer,source.sampleSize)
                            val count = source.readSampleData(buffer,0)
                            if (count < 0) break
                            val time = span.outputStartUs+sourceUs-span.sourceStartUs
                            if (time > lastUs) {
                                info.set(0,count,time,if (source.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                                writer.writeSampleData(audioTrack,buffer,info)
                                lastUs = time
                            }
                        }
                        source.advance()
                    }
                }
                // If a selected interval is shorter than one packet, keep a valid silent audio track.
                check(lastUs >= 0) { "No complete source audio packet in recording" }
            } else {
                val quality = job.session.quality ?: RecordingAudio.Quality(
                    audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtMost(48000),192000)
                val channels = if (job.session.quality != null) 1 else audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1,2)
                RandomAccessFile(pcm,"rw").use { samples ->
                    val frames = ceil(duration*quality.sampleRate/1_000_000.0).toLong()
                    val bytes = Math.multiplyExact(frames,channels*2L)
                    check(bytes < context.cacheDir.usableSpace-16L*1024*1024) { "Insufficient space for audio conversion" }
                    samples.setLength(bytes) // New file: unwritten playback pauses read as silence.
                    for (span in spans) decodeSpan(context,job.session.source,span,samples,quality.sampleRate,channels)
                    encode(samples,frames,quality,channels,writer,packets,duration)
                }
            }
            picture.selectTrack(videoIndex)
            copyVideo(picture,writer,videoTrack)
            writer.stop()
            succeeded = true
            return output
        } finally {
            runCatching { mux?.release() }
            picture.release();source.release();pcm.delete();packets.delete()
            if (!succeeded) output.delete()
        }
    }

    private fun capacity(buffer: ByteBuffer, size: Long): ByteBuffer {
        require(size in 0..(64L*1024*1024)) { "Oversized media sample" }
        return if (buffer.capacity() >= size) buffer.apply { clear() } else ByteBuffer.allocateDirect(size.toInt())
    }
    private fun copyVideo(source: MediaExtractor, mux: MediaMuxer, target: Int) {
        val first = source.sampleTime.coerceAtLeast(0)
        var buffer = ByteBuffer.allocateDirect(1024*1024)
        val info = MediaCodec.BufferInfo()
        while (source.sampleTime >= 0) {
            buffer = capacity(buffer,source.sampleSize)
            val size = source.readSampleData(buffer,0)
            if (size < 0) break
            info.set(0,size,(source.sampleTime-first).coerceAtLeast(0),if (source.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
            mux.writeSampleData(target,buffer,info)
            source.advance()
        }
    }

    private fun decodeSpan(context: Context, uri: Uri, span: TapAudioTimeline.Span, output: RandomAccessFile, rate: Int, channels: Int) {
        val source = extractor(context,uri)
        var decoder: MediaCodec? = null
        try {
            val index = track(source,"audio/")
            val inputFormat = source.getTrackFormat(index)
            val codec = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
            decoder = codec
            codec.configure(inputFormat,null,null,0);codec.start()
            source.selectTrack(index);source.seekTo(span.sourceStartUs,MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val end = span.sourceStartUs+span.outputEndUs-span.outputStartUs
            var inputEnded=false
            var format=inputFormat
            var nextFrame=ceil(span.outputStartUs*rate/1_000_000.0).toLong()
            val info=MediaCodec.BufferInfo()
            var progress=SystemClock.elapsedRealtime()
            while (true) {
                if (!inputEnded) {
                    val i=codec.dequeueInputBuffer(0)
                    if(i>=0) {
                        val time=source.sampleTime
                        if(source.sampleTrackIndex<0 || time>=end) {
                            codec.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnded=true
                        } else {
                            val input=codec.getInputBuffer(i)!!
                            check(source.sampleSize<=input.capacity()) { "Audio packet exceeds decoder buffer" }
                            val size=source.readSampleData(input,0)
                            codec.queueInputBuffer(i,0,size.coerceAtLeast(0),time,if(size<0) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                            if(size<0) inputEnded=true else source.advance()
                        }
                        progress=SystemClock.elapsedRealtime()
                    }
                }
                when(val i=codec.dequeueOutputBuffer(info,10000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { format=codec.outputFormat;progress=SystemClock.elapsedRealtime() }
                    else -> if(i>=0) {
                        try {
                            if(info.size>0) nextFrame = writePcm(codec.getOutputBuffer(i)!!,info,format,span,output,rate,channels,nextFrame)
                        } finally { codec.releaseOutputBuffer(i,false) }
                        progress=SystemClock.elapsedRealtime()
                        if(info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
                check(SystemClock.elapsedRealtime()-progress<10000) { "Audio decoder stalled" }
            }
        } finally { runCatching { decoder?.stop() };decoder?.release();source.release() }
    }

    private fun writePcm(buffer: ByteBuffer, info: MediaCodec.BufferInfo, format: MediaFormat,
                         span: TapAudioTimeline.Span, output: RandomAccessFile, rate: Int, channels: Int, next: Long): Long {
        val inputRate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val inputChannels=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val encoding=if(format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
        val bytes=when(encoding) { AudioFormat.ENCODING_PCM_16BIT->2;AudioFormat.ENCODING_PCM_FLOAT->4;else->error("Unsupported decoder PCM format") }
        val count=info.size/(bytes*inputChannels)
        if(count==0) return next
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val from=maxOf(next,ceil((span.outputStartUs+info.presentationTimeUs-span.sourceStartUs)*rate/1_000_000.0).toLong())
        val until=minOf(ceil(span.outputEndUs*rate/1_000_000.0).toLong(),
            floor((span.outputStartUs+info.presentationTimeUs-span.sourceStartUs+count*1_000_000.0/inputRate)*rate/1_000_000.0).toLong())
        if(until<=from) return next
        val packed=ByteBuffer.allocate(Math.toIntExact((until-from)*channels*2)).order(ByteOrder.LITTLE_ENDIAN)
        fun sample(frame: Int, channel: Int): Double {
            val offset=info.offset+(frame*inputChannels+channel)*bytes
            return if(bytes==2) buffer.getShort(offset)/32768.0 else buffer.getFloat(offset).toDouble().let { if(it.isFinite()) it else 0.0 }
        }
        for(frame in from until until) {
            val position=(span.sourceStartUs+frame*1_000_000.0/rate-span.outputStartUs-info.presentationTimeUs)*inputRate/1_000_000.0
            // Box-average when downsampling to avoid point-sampling aliasing at the low tiers.
            val first=floor(position).toInt().coerceIn(0,count-1)
            val last=(ceil(position+inputRate.toDouble()/rate).toInt()-1).coerceIn(first,count-1)
            for(channel in 0 until channels) {
                var sum=0.0;var n=0
                for(f in first..last) for(c in 0 until inputChannels) {
                    if(channels==1 || inputChannels==1 || c%channels==channel) {sum+=sample(f,c);n++}
                }
                packed.putShort((sum/n.coerceAtLeast(1)*32767).toInt().coerceIn(-32768,32767).toShort())
            }
        }
        output.seek(from*channels*2);output.write(packed.array())
        return until
    }

    private fun encode(pcm: RandomAccessFile, frames: Long, quality: RecordingAudio.Quality, channels: Int, mux: MediaMuxer, packets: File, durationUs: Long) {
        val codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        var started=false
        val packetOutput = DataOutputStream(packets.outputStream().buffered())
        try {
            val format=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,quality.sampleRate,channels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE,quality.bitRate)
            }
            codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);codec.start()
            pcm.seek(0)
            var fed=0L;var ended=false;var track=-1;var packetCount=0L
            var reportedDelay: Int? = null
            val info=MediaCodec.BufferInfo();var progress=SystemClock.elapsedRealtime()
            while(true) {
                if(!ended) {
                    val i=codec.dequeueInputBuffer(0)
                    if(i>=0) {
                        val input=codec.getInputBuffer(i)!!
                        val count=minOf((frames-fed)*channels*2,input.capacity().toLong()).toInt()/(channels*2)*(channels*2)
                        val bytes=ByteArray(count);if(count>0) {pcm.readFully(bytes);input.put(bytes)}
                        codec.queueInputBuffer(i,0,count,fed*1_000_000/quality.sampleRate,if(count==0) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        fed+=count/(channels*2);ended=count==0;progress=SystemClock.elapsedRealtime()
                    }
                }
                when(val i=codec.dequeueOutputBuffer(info,10000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!started)
                        val actual=codec.outputFormat
                        if(actual.containsKey(MediaFormat.KEY_ENCODER_DELAY)) reportedDelay=actual.getInteger(MediaFormat.KEY_ENCODER_DELAY)
                        track=mux.addTrack(actual);mux.start();started=true;progress=SystemClock.elapsedRealtime()
                    }
                    else -> if(i>=0) {
                        try {
                            if(info.size>0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                check(started)
                                val buffer=codec.getOutputBuffer(i)!!
                                buffer.position(info.offset);buffer.limit(info.offset+info.size)
                                val bytes=ByteArray(info.size);buffer.get(bytes)
                                packetOutput.writeLong(info.presentationTimeUs)
                                packetOutput.writeInt(info.size);packetOutput.write(bytes)
                                packetCount++
                            }
                        } finally {codec.releaseOutputBuffer(i,false)}
                        progress=SystemClock.elapsedRealtime()
                        if(info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
                check(SystemClock.elapsedRealtime()-progress<10000) { "Audio encoder stalled" }
            }
            packetOutput.close()
            // AAC-LC uses 1024 samples per packet. Some encoders omit their priming delay;
            // excess complete packets over the input length provide its whole-frame component.
            val priming = (reportedDelay?.toLong() ?: ((packetCount-(frames+1023)/1024).coerceAtLeast(0)*1024))
                .coerceIn(0,8192)
            val delayUs=priming*1_000_000/quality.sampleRate
            var last=-1L
            DataInputStream(packets.inputStream().buffered()).use { input ->
                repeat(Math.toIntExact(packetCount)) {
                    val time=input.readLong()-delayUs
                    val size=input.readInt();val bytes=ByteArray(size);input.readFully(bytes)
                    if(time>=0 && time<durationUs) {
                        info.set(0,size,time,MediaCodec.BUFFER_FLAG_KEY_FRAME)
                        mux.writeSampleData(track,ByteBuffer.wrap(bytes),info);last=time
                    }
                }
            }
            check(last>=0) { "No encoded audio" }
            if(durationUs>last) {
                info.set(0,0,durationUs,MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                mux.writeSampleData(track,ByteBuffer.allocate(0),info)
            }
        } finally {runCatching {packetOutput.close()};runCatching {codec.stop()};codec.release()}
    }
}
