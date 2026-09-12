package com.bongorian.signa1

import android.widget.LinearLayout

internal fun MainActivity.renderCameraLens() {
    val selected = engine.activeLens
    lensRail.removeAllViews()
    for (lens in engine.lenses.filter { it.front == (selected?.front ?: engine.front) }.sortedBy { it.equivalentMm ?: Double.MAX_VALUE }) {
        val focals = lens.opticalFocals.takeIf { it.size > 1 } ?: listOfNotNull(lens.focalMm)
        for (focal in focals.ifEmpty { listOf(0f) }) {
            val equivalent = lens.equivalentMm?.let { it * focal / (lens.focalMm ?: focal) }
            val label = if (equivalent != null) String.format(java.util.Locale.US, "%.0f mm", equivalent)
                else if (focal > 0) String.format(java.util.Locale.US, "%.1f mm", focal) else "—"
            val active = lens.key == engine.cameraSelection && (focals.size <= 1 || focal == engine.opticalFocal)
            val choice = button(label).apply {
                textSize = 12f
                setPadding(dp(12f), 0, dp(12f), 0)
                isSingleLine = true
                isSelected = active
                isEnabled = !recording && !engine.photoBusy
                setTextColor(if (active) MainActivity.BG else MainActivity.WHITE)
                background = bg(if (active) MainActivity.LIME else 0xCC101410.toInt(), 0)
                contentDescription = if (equivalent != null) getString(R.string.lens_equivalent, equivalent) else label
                tooltipText = lens.detail(this@renderCameraLens)
                setOnClickListener {
                    if (!recording && !engine.photoBusy && !tapMode) {
                        cancelEffectPreview()
                        if (lens.key == engine.cameraSelection) {
                            engine.setOpticalFocal(focal)
                            handler.postDelayed({ renderCameraLens() }, 100)
                        } else {
                            if (lens.opticalFocals.size > 1) getSharedPreferences("signal", 0).edit()
                                .putFloat("opticalFocal:" + lens.key, focal).apply()
                            engine.selectCamera(lens.key)
                        }
                    }
                }
            }
            lensRail.addView(choice, LinearLayout.LayoutParams(-2, dp(ControlSize.COMPACT)).apply { leftMargin = dp(4f); rightMargin = dp(4f) })
        }
    }
    flipButton.contentDescription = getString(R.string.camera_flip_facing)
    renderTorch()
}

internal fun MainActivity.showCameras() {
    if (recording || engine.photoBusy || tapMode || engine.lenses.isEmpty()) return
    lensDialog?.dismiss()
    val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    val note=text(getString(R.string.lens_hint),12,MainActivity.MUTED)
    body.addView(note,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(14f)})
    engine.activeLens?.opticalFocals?.takeIf { it.size>1 }?.let { focalLengths ->
        val label=text("",13,MainActivity.LIME)
        fun describe(value: Float) {label.text=getString(R.string.lens_optical_focal,value)}
        val selected=OpticalFocal.supported(focalLengths,engine.opticalFocal ?: focalLengths.first())!!
        describe(selected)
        body.addView(label,LinearLayout.LayoutParams(-1,-2))
        val slider=android.widget.SeekBar(this).apply {max=focalLengths.lastIndex;progress=focalLengths.indexOf(selected)}
        slider.contentDescription=getString(R.string.lens_optical)
        slider.setOnSeekBarChangeListener(object: android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: android.widget.SeekBar?,progress: Int,fromUser: Boolean) {
                if(fromUser) {val value=focalLengths[progress];describe(value);engine.setOpticalFocal(value)}
            }
            override fun onStartTrackingTouch(bar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(bar: android.widget.SeekBar?) {}
        })
        body.addView(slider,LinearLayout.LayoutParams(-1,dp(48f)))
    }
    for (lens in engine.lenses.filter { it.front == (engine.activeLens?.front ?: engine.front) }) {
        val choice=button(lens.label(this)+"\n"+lens.detail(this))
        choice.tag="camera-lens:"+lens.key
        choice.isSingleLine=false
        choice.setTextSize(13f)
        choice.setPadding(dp(12f),dp(10f),dp(12f),dp(10f))
        choice.setTextColor(if (lens.key==engine.cameraSelection) MainActivity.BG else MainActivity.WHITE)
        choice.background=bg(if (lens.key==engine.cameraSelection) MainActivity.LIME else MainActivity.PANEL,0)
        body.addView(choice,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(8f)})
        choice.setOnClickListener {
            if (!recording && !engine.photoBusy && !tapMode) {
                cancelEffectPreview()
                engine.selectCamera(lens.key)
                lensDialog?.dismiss()
            }
        }
    }
    lensDialog=SignalSheet.content(this,getString(R.string.lens_select),body,0,null,.72f)
}
