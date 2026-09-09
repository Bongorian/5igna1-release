package com.bongorian.signa1

/** A mode-tab shortcut edits that mode's resolution without taking a picture or changing modes. */
internal object ResolutionPicker {
    fun show(a: MainActivity, video: Boolean) {
        if (!a.ready || a.recording || a.engine.photoBusy) return
        val options = a.cameraOptions ?: return
        val next = CaptureSettings(a.settings)
        val keys: List<String>
        val labels: Array<String>
        val current: String
        val title: Int
        if (video && next.rawVideo) {
            keys = options.rawVideos.map { it.size.toString() }
            labels = options.rawVideos.map { it.label(a) }.toTypedArray()
            current = next.rawVideoSize
            title = R.string.ui_raw_video_resolution
        } else if (video) {
            val choices = options.videosFor(next.codec)
            keys = listOf("recommended") + choices.map { it.key() }
            labels = (listOf(a.getString(R.string.load_recommended)) + choices.map { it.label(a) }).toTypedArray()
            current = next.videoKey
            title = R.string.ui_video_resolution_fps
        } else {
            val raw = next.photoFormat != 0
            val choices = if (raw) options.raws else options.photos
            keys = listOf("recommended", "auto", "max") + choices.map { it.key() }
            labels = (listOf(a.getString(R.string.load_recommended),
                a.getString(if (raw) R.string.ui_standard_prefer_12_mp_or_less else R.string.fault_live_resolution),
                a.getString(if (raw) R.string.ui_maximum_camera_resolution else R.string.fault_live_max)) +
                choices.map { it.label(a) }).toTypedArray()
            current = next.photoSize
            title = R.string.ui_photo_resolution
        }
        if (keys.isEmpty()) return
        SignalSheet.pick(a, a.getString(title), labels, keys.indexOf(current).coerceAtLeast(0)) { index ->
            if (video && next.rawVideo) next.rawVideoSize = keys[index]
            else if (video) next.videoKey = keys[index]
            else next.photoSize = keys[index]
            a.applySettings(next)
        }
    }
}
