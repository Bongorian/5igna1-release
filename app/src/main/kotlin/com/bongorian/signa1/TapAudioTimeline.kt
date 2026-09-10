package com.bongorian.signa1

/** Source playback intervals in the output recording clock. Paused intervals remain silent. */
internal class TapAudioTimeline {
    data class Span(val outputStartUs: Long, val outputEndUs: Long, val sourceStartUs: Long) {
        fun clipped(start: Long, end: Long): Span? {
            val from = maxOf(start,outputStartUs)
            val to = minOf(end,outputEndUs)
            return if (to <= from) null else Span(from-start,to-start,sourceStartUs+from-outputStartUs)
        }
    }
    private val spans = ArrayList<Span>()
    private var startUs: Long? = null
    private var sourceUs = 0L
    private var firstFrameUs: Long? = null
    fun firstFrame(nowUs: Long) { if (firstFrameUs == null) firstFrameUs = nowUs }
    fun playback(playing: Boolean, positionUs: Long, nowUs: Long) {
        val start = startUs
        if (start != null && nowUs > start) spans.add(Span(start,nowUs,sourceUs))
        startUs = if (playing) nowUs else null
        sourceUs = positionUs.coerceAtLeast(0)
    }
    fun snapshot(nowUs: Long): List<Span> {
        val first = firstFrameUs ?: return emptyList()
        val complete = spans.toMutableList()
        startUs?.let { if(nowUs > it) complete.add(Span(it,nowUs,sourceUs)) }
        return complete.mapNotNull { it.clipped(first,nowUs) }
    }
}
