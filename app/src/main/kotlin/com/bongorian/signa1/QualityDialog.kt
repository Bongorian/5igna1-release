package com.bongorian.signa1

import android.app.Dialog
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.min

internal class QualityDialog(a: MainActivity) {
    val activity: MainActivity
    val draft: CaptureSettings
    val options: CameraOptions?
    var advanced: Boolean
    var language: String = AppLanguage.current()
    var languageField: TextView? = null
    var loadNote: TextView? = null
    var sheet: Dialog? = null
    var format: TextView? = null
    var size: TextView? = null
    var jpeg: TextView? = null
    var codec: TextView? = null
    var video: TextView? = null
    var bitrate: TextView? = null
    var rawSize: TextView? = null
    var rawRate: TextView? = null
    var rawInfo: TextView? = null
    var rawMode: SignalToggle? = null
    var content: LinearLayout? = null
    private var syncing = false

    init {
        activity = a
        advanced = a.advancedMode
        draft = CaptureSettings(a.settings)
        options = a.cameraOptions
    }

    fun heading(label: String?) {
        val t = activity!!.text(label, 13, MainActivity.LIME)
        activity.typography(t, MainActivity.TEXT_LABEL, true)
        val p = LinearLayout.LayoutParams(-1, activity.dp(32f))
        p.topMargin = if (content!!.childCount == 0) 0 else activity.dp(20f)
        p.bottomMargin = activity.dp(6f)
        content!!.addView(t, p)
    }

    fun field(): TextView {
        val t = activity!!.text("", MainActivity.TEXT_LABEL, MainActivity.WHITE)
        t.setPadding(activity.dp(12f), activity.dp(8f), activity.dp(12f), activity.dp(8f))
        t.setBackground(activity.bg(MainActivity.PANEL, 0))
        t.setMinHeight(activity.dp(56f))
        val p = LinearLayout.LayoutParams(-1, -2)
        p.bottomMargin = activity.dp(7f)
        content!!.addView(t, p)
        return t
    }

    fun pick(title: String?, labels: Array<String>, selected: Int, chosen: (Int) -> Unit) {
        SignalSheet.pick(
            activity,
            title,
            labels,
            selected,
            IntConsumer@{ n: Int ->
                chosen(n)
                persist()
            },
        )
    }

    fun show() {
        val scroll = ScrollView(activity)
        content = LinearLayout(activity)
        content!!.setOrientation(LinearLayout.VERTICAL)
        content!!.setPadding(0, 0, 0, activity!!.dp(8f))
        content!!.setBackgroundColor(MainActivity.BG)
        scroll.addView(content)
        note(activity.getString(R.string.settings_saved_immediately))
        heading(activity.getString(R.string.settings_modes))
        mode(
            R.string.ui_advanced_mode,
            R.string.ui_advanced_settings_hint,
            advanced,
            "advanced-mode",
            Consumer@{ checked: Boolean? -> advanced = checked!!; saveAdvanced() },
        )
        mode(
            R.string.expert_mode,
            R.string.expert_hint,
            draft.expertMode,
            "expert-mode",
            Consumer@{ checked: Boolean? ->
                draft.expertMode = checked!!
                if (!syncing) persist()
            },
        )
        if (options == null) {
            note(activity.getString(R.string.camera_settings_unavailable))
            experimentalSection()
            appSection()
            refresh()
            scroll.removeView(content)
            sheet =
                SignalSheet.content(activity, activity.getString(R.string.ui_settings), content, 0, null, .87f)
            return
        }
        heading(activity.getString(R.string.load_title))
        loadNote = note("")
        val recommend = field()
        recommend.setTag("load-recommend")
        recommend.setText(activity.getString(R.string.load_use_recommended))
        recommend.setOnClickListener(
            OnClickListener@{ v: View? ->
                syncing = true
                draft.expertMode = false
                content?.findViewWithTag<SignalToggle>("expert-mode")?.isChecked = false
                draft.photoSize = "recommended"
                draft.videoKey = "recommended"
                draft.videoQuality = 1
                syncing = false
                persist()
            }
        )
        heading(activity.getString(R.string.settings_photo))
        note(activity.getString(R.string.fault_signal_photo_note))
        format = field()
        format!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                val labels: Array<String> =
                    if (options.raws.isEmpty())
                        arrayOf<String>(activity.getString(R.string.ui_processed_jpeg))
                    else
                        arrayOf<String>(
                            activity.getString(R.string.ui_processed_jpeg),
                            activity.getString(R.string.ui_raw_dng_sensor_processing),
                        )
                pick(
                    activity.getString(R.string.ui_save_format),
                    labels,
                    if (draft.photoFormat == 0) 0 else 1,
                    IntConsumer@{ n: Int ->
                        draft.photoFormat = if (n == 0) 0 else 2
                        draft.photoSize = "recommended"
                    },
                )
            }
        )
        size = field()
        size!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                val choices = if (draft.photoFormat == 0) options.photos else options.raws
                val labels = Array(choices.size + 3) { "" }
                labels[0] = activity.getString(R.string.load_recommended)
                labels[1] =
                    activity.getString(
                        if (draft.photoFormat == 0) R.string.fault_live_resolution
                        else R.string.ui_standard_prefer_12_mp_or_less
                    )
                labels[2] =
                    activity.getString(
                        if (draft.photoFormat == 0) R.string.fault_live_max
                        else R.string.ui_maximum_camera_resolution
                    )
                var selected =
                    if ("max" == draft.photoSize) 2 else if ("auto" == draft.photoSize) 1 else 0
                for (i in choices.indices) {
                    labels[i + 3] = choices.get(i)!!.label(activity)
                    if (choices.get(i)!!.key() == draft.photoSize) selected = i + 3
                }
                pick(
                    activity.getString(R.string.ui_photo_resolution),
                    labels,
                    selected,
                    IntConsumer@{ n: Int ->
                        draft.photoSize =
                            if (n == 0) "recommended"
                            else if (n == 1) "auto"
                            else if (n == 2) "max" else choices.get(n - 3)!!.key()
                    },
                )
            }
        )
        jpeg = field()
        jpeg!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                if (draft.photoFormat != 0) return@OnClickListener
                val qualities = intArrayOf(85, 95, 100)
                val selected =
                    if (draft.jpegQuality == 85) 0 else if (draft.jpegQuality == 100) 2 else 1
                pick(
                    activity.getString(R.string.ui_jpeg_quality),
                    arrayOf<String>(
                        activity.getString(R.string.ui_85_smaller_files),
                        activity.getString(R.string.ui_95_high_quality),
                        activity.getString(R.string.ui_100_best_quality),
                    ),
                    selected,
                    IntConsumer@{ n: Int -> draft.jpegQuality = qualities[n] },
                )
            }
        )
        heading(activity.getString(R.string.settings_video))
        codec = field()
        codec!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                val keys: MutableList<String> = ArrayList<String>()
                val labels: MutableList<String> = ArrayList<String>()
                for (key in arrayOf<String>("video/hevc", "video/avc")) if (
                    !options.videosFor(key).isEmpty()
                ) {
                    keys.add(key)
                    labels.add(
                        if (key == "video/hevc")
                            activity.getString(R.string.ui_hevc_h_265_efficient)
                        else activity.getString(R.string.ui_avc_h_264_compatible)
                    )
                }
                pick(
                    activity.getString(R.string.ui_video_codec),
                    labels.toTypedArray<String>(),
                    keys.indexOf(draft.codec),
                    IntConsumer@{ n: Int ->
                        draft.codec = keys.get(n)
                        draft.videoKey = ""
                    },
                )
            }
        )
        video = field()
        video!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                val choices = options.videosFor(draft.codec)
                val labels = Array(choices.size + 1) { "" }
                labels[0] = activity.getString(R.string.load_recommended)
                var selected = 0
                for (i in choices.indices) {
                    labels[i + 1] = choices.get(i)!!.label(activity)
                    if (choices.get(i)!!.key() == draft.videoKey) selected = i + 1
                }
                pick(
                    activity.getString(R.string.ui_video_resolution_fps),
                    labels,
                    selected,
                    IntConsumer@{ n: Int ->
                        draft.videoKey = if (n == 0) "recommended" else choices.get(n - 1)!!.key()
                    },
                )
            }
        )
        bitrate = field()
        bitrate!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                val names =
                    arrayOf<String>(
                        activity.getString(R.string.ui_smaller_files),
                        activity.getString(R.string.ui_high_quality),
                        activity.getString(R.string.ui_best_quality),
                        activity.getString(R.string.ui_encoder_maximum),
                    )
                val labels = Array(4) { "" }
                for (i in 0..3) {
                    val s = CaptureSettings(draft)
                    s.videoQuality = i
                    val rate = options.bitrate(requireNotNull(options.video(s)), s)
                    labels[i] =
                        names[i] +
                            String.format(
                                Locale.US,
                                activity.getString(R.string.ui_1f_mbps_approx_0f_mb_min),
                                rate / 1e6,
                                rate * 60 / 8e6,
                            )
                }
                pick(
                    activity.getString(R.string.ui_video_quality),
                    labels,
                    draft.videoQuality,
                    IntConsumer@{ n: Int -> draft.videoQuality = n },
                )
            }
        )
        heading(activity.getString(R.string.settings_raw_video))
        rawMode =
            SignalToggle(
                activity,
                activity.getString(R.string.settings_raw_enable),
                draft.rawVideo && options.rawVideoAvailable(),
            )
        rawMode!!.setEnabled(options.rawVideoAvailable())
        content!!.addView(rawMode, LinearLayout.LayoutParams(-1, activity.dp(48f)))
        rawMode!!.setOnCheckedChangeListener(
            OnCheckedChangeListener@{ button: CompoundButton?, checked: Boolean ->
                draft.rawVideo = checked
                persist()
            }
        )
        rawInfo =
            note(
                options.rawVideoReason(activity) +
                    activity.getString(R.string.ui_saves_original_silent_dng_frames_as_a_zip)
            )
        rawSize = field()
        rawSize!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                val labels = options.rawVideos.map { it.label(activity) }.toTypedArray()
                var selected = 0
                for (n in options.rawVideos.indices) if (
                    options.rawVideos.get(n) == options.rawVideo(draft)
                )
                    selected = n
                pick(
                    activity.getString(R.string.ui_raw_video_resolution),
                    labels,
                    selected,
                    IntConsumer@{ n: Int ->
                        draft.rawVideoSize = options.rawVideos.get(n).size.toString()
                    },
                )
            }
        )
        rawRate = field()
        rawRate!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                val choice = options.rawVideo(draft)
                if (choice == null) return@OnClickListener
                val labels = Array(choice.maxFps) { "" }
                for (n in 1..choice.maxFps) labels[n - 1] =
                    n.toString() + activity.getString(R.string.ui_fps_target)
                pick(
                    activity.getString(R.string.ui_raw_video_target_frame_rate),
                    labels,
                    min(draft.rawVideoFps, choice.maxFps) - 1,
                    IntConsumer@{ n: Int -> draft.rawVideoFps = n + 1 },
                )
            }
        )
        heading(activity.getString(R.string.settings_metadata))
        val gps =
            SignalToggle(
                activity,
                activity.getString(R.string.ui_save_capture_location),
                draft.location,
            )
        gps.setOnCheckedChangeListener(
            OnCheckedChangeListener@{ b: CompoundButton?, checked: Boolean ->
                draft.location = checked
                persist()
            }
        )
        content!!.addView(gps, LinearLayout.LayoutParams(-1, activity.dp(48f)))
        note(activity.getString(R.string.settings_metadata_hint))
        experimentalSection()
        appSection()
        refresh()
        scroll.removeView(content)
        sheet =
            SignalSheet.content(activity, activity.getString(R.string.ui_settings), content, 0, null, .87f)
    }

    fun note(label: String?): TextView {
        val note = activity!!.text(label, MainActivity.TEXT_BODY, MainActivity.MUTED)
        note.setLineSpacing(activity.dp(3f).toFloat(), 1f)
        val p = LinearLayout.LayoutParams(-1, -2)
        p.topMargin = activity.dp(5f)
        p.bottomMargin = activity.dp(12f)
        content!!.addView(note, p)
        return note
    }

    fun mode(label: Int, hint: Int, value: Boolean, tag: String?, changed: (Boolean) -> Unit) {
        val toggle = SignalToggle(activity, activity!!.getString(label), value)
        toggle.setTag(tag)
        content!!.addView(toggle, LinearLayout.LayoutParams(-1, activity.dp(48f)))
        note(activity.getString(hint))
        toggle.setOnCheckedChangeListener(
            OnCheckedChangeListener@{ view: CompoundButton?, checked: Boolean ->
                changed(checked)
            }
        )
    }

    fun experimentalSection() {
        heading(activity.getString(R.string.settings_experiments))
        mode(R.string.experimental_signals, R.string.experimental_signals_hint,
            draft.experimentalSignals, "experimental-signals") { checked ->
            draft.experimentalSignals = checked
            persist()
        }
        mode(R.string.resolution_audio, R.string.resolution_audio_hint,
            draft.resolutionAudio, "resolution-audio") { checked ->
            draft.resolutionAudio = checked
            persist()
        }
    }

    fun appSection() {
        heading(activity!!.getString(R.string.settings_app))
        val tutorial = field()
        tutorial.setTag("settings-tutorial")
        tutorial.setText(activity.getString(R.string.tutorial_title))
        tutorial.setOnClickListener(OnClickListener@{ v: View? -> activity.showTutorial(sheet) })
        languageField = field()
        languageField!!.setOnClickListener(
            OnClickListener@{ v: View? ->
                SignalSheet.pick(
                    activity,
                    activity.getString(R.string.language_title),
                    AppLanguage.labels(activity),
                    AppLanguage.index(language),
                    IntConsumer@{ n: Int ->
                        language = AppLanguage.TAGS[n]
                        sheet?.dismiss()
                        AppLanguage.select(language)
                    },
                )
            }
        )
        val feedback = field()
        feedback.tag = "settings-feedback"
        feedback.text = activity.getString(R.string.feedback_title)
        feedback.setOnClickListener { FeedbackDialog.show(activity) }
        val privacy = field()
        privacy.setText(
            BuildConfig.APP_NAME +
                " " +
                BuildConfig.VERSION_NAME +
                "\n" +
                activity.getString(R.string.ui_privacy_policy_support)
        )
        privacy.setOnClickListener(OnClickListener@{ v: View? -> AboutDialog.show(activity) })
    }

    private fun persist() {
        activity.applySettings(draft)
        refresh()
    }

    fun saveAdvanced() {
        draft.advancedMode = advanced
        persist()
    }

    fun refresh() {
        if (loadNote != null)
            loadNote!!.setText(
                activity!!.getString(
                    if (draft.expertMode) R.string.expert_load_hint else R.string.load_hint
                )
            )
        if (languageField != null)
            languageField!!.setText(
                activity!!.getString(R.string.language_summary) +
                    "\n" +
                    AppLanguage.labels(activity)[AppLanguage.index(language)]
            )
        if (options == null) return
        val raw = options.rawVideo(draft)
        val active = draft.rawVideo && options.rawVideoAvailable()
        rawInfo!!.setVisibility(
            if (active || !options.rawVideoAvailable()) View.VISIBLE else View.GONE
        )
        rawSize!!.setVisibility(if (active) View.VISIBLE else View.GONE)
        rawRate!!.setVisibility(if (active) View.VISIBLE else View.GONE)
        rawSize!!.setEnabled(active)
        rawRate!!.setEnabled(active)
        if (raw != null) {
            val fps = min(draft.rawVideoFps, raw.maxFps)
            rawSize!!.setText(
                activity!!.getString(R.string.ui_raw_video_resolution_39) + raw.label(activity)
            )
            rawRate!!.setText(
                String.format(
                    Locale.US,
                    activity.getString(R.string.ui_raw_video_target_frame_rate_d_fps_approx),
                    fps,
                    CameraOptions.area(raw.size) * 2 * fps / 1e6,
                )
            )
        }
        val p = options.photo(draft)
        val v = options.video(draft)
        format!!.setText(
            activity!!.getString(R.string.ui_photo_format) +
                (if (draft.photoFormat == 0) activity.getString(R.string.ui_processed_jpeg)
                else activity.getString(R.string.ui_raw_dng_sensor_processing))
        )
        size!!.setText(
            activity.getString(R.string.ui_photo_resolution_42) +
                (if ("recommended" == draft.photoSize) activity.getString(R.string.load_auto_prefix)
                else "") +
                (if (p == null) activity.getString(R.string.ui_unavailable_on_this_camera)
                else p.label(activity))
        )
        jpeg!!.setText(
            if (draft.photoFormat == 0)
                activity.getString(R.string.ui_jpeg_quality_44) + draft.jpegQuality + " / 100"
            else activity.getString(R.string.ui_raw_quality_uncompressed_sensor_data)
        )
        jpeg!!.setAlpha(if (draft.photoFormat == 0) 1f else .55f)
        codec!!.setText(
            activity.getString(R.string.ui_video_codec_46) +
                (if (draft.codec == "video/hevc") "HEVC / H.265" else "AVC / H.264")
        )
        video!!.setText(
            activity.getString(R.string.ui_video_resolution_fps_47) +
                (if ("recommended" == draft.videoKey) activity.getString(R.string.load_auto_prefix)
                else "") +
                (if (v == null) activity.getString(R.string.ui_unavailable) else v.label(activity))
        )
        codec!!.setEnabled(v != null && !active)
        video!!.setEnabled(v != null && !active)
        bitrate!!.setEnabled(v != null && !active)
        for (field in
            arrayOf<TextView>(
                codec!!,
                video!!,
                bitrate!!,
            )) field.setAlpha(if (active) .35f else 1f)
        val rate = if (v == null) 0 else options.bitrate(v, draft)
        bitrate!!.setText(
            String.format(
                Locale.US,
                activity.getString(R.string.ui_video_quality_1f_mbps_approx_0f_mb_min),
                rate / 1e6,
                rate * 60 / 8e6,
            )
        )
    }
}
