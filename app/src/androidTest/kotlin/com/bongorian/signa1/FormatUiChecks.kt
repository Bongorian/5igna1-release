package com.bongorian.signa1

internal object FormatUiChecks {
    fun run(test: DeviceChecks) {
        val a = test.activity!!
        fun waitReady(label: String) = test.await(label, { a.ready }, 20000)
        test.runOnMainSync {
            a.tapMode = false; a.videoMode = false
            a.applySettings(CaptureSettings(a.settings).apply { photoFormat = 0; rawVideoEnabled = false; rawVideo = false })
        }
        waitReady("JPG")
        android.os.SystemClock.sleep(500)
        test.runOnMainSync {
            val modes = a.photoTab.parent as android.view.View
            val toolbar = a.formatButton.parent as CaptureToolbar
            check(a.formatButton.right <= modes.left && modes.right <= toolbar.live.left)
            val footer = a.capture.parent as android.view.View
            check(kotlin.math.abs(a.capture.left+a.capture.width/2-footer.width/2)<=1)
        }
        test.languageScreenshot("gpu-defaults-toolbar")
        val effects = a.effectState.encode()
        if (a.cameraOptions!!.raws.isNotEmpty()) {
            test.runOnMainSync { a.formatButton.performClick(); check(a.capturePhotoFormat == 2) }
            waitReady("RAW photo")
            check(a.effectState.encode() == effects)
            test.runOnMainSync { a.formatButton.performClick(); check(a.capturePhotoFormat == 0) }
            waitReady("JPG restored")
        } else check(!a.canCycleFormat())
        test.runOnMainSync {
            a.recording = true
            a.cycleFormat()
            check(a.capturePhotoFormat == 0)
            a.recording = false
            a.videoMode = true
            a.applySettings(CaptureSettings(a.settings))
        }
        waitReady("MP4 without RAW opt-in")
        test.runOnMainSync {
            check(!a.formatButton.isEnabled)
            a.cycleFormat()
            check(!a.captureRawVideo && !a.settings.rawVideoEnabled)
        }
        if (a.cameraOptions!!.rawVideoAvailable()) {
            test.runOnMainSync {
                val q = QualityDialog(a); q.show()
                q.rawMode!!.performClick()
                check(a.settings.rawVideoEnabled && !a.captureRawVideo) { "Enabling RAW switched capture format" }
                q.sheet!!.dismiss()
            }
            waitReady("MP4 with RAW opt-in")
            test.runOnMainSync { check(a.formatButton.isEnabled); a.formatButton.performClick() }
            waitReady("RAW video")
            check(a.captureRawVideo)
            test.runOnMainSync { a.formatButton.performClick() }
            waitReady("MP4 retaining opt-in")
            check(!a.captureRawVideo && a.settings.rawVideoEnabled)
            val saved = CaptureSettings.load(a.getSharedPreferences("signal", 0))
            check(saved.rawVideoEnabled && !saved.rawVideo)
            test.runOnMainSync { a.formatButton.performClick() }
            waitReady("RAW before opt-out")
            test.runOnMainSync {
                val q = QualityDialog(a); q.show()
                q.rawMode!!.performClick()
                check(!a.settings.rawVideoEnabled && !a.captureRawVideo)
                q.sheet!!.dismiss()
            }
            waitReady("MP4 after opt-out")
            check(!a.formatButton.isEnabled)
        }
        test.runOnMainSync {
            a.tapMode = true; a.videoMode = false; a.renderFormat()
            check(a.formatButton.text.toString() == "JPG" && !a.formatButton.isEnabled)
            a.cycleFormat()
            a.videoMode = true; a.renderFormat()
            check(a.formatButton.text.toString() == "MP4" && !a.formatButton.isEnabled)
            a.tapMode = false; a.renderFormat()
        }
        val fixture = test.targetContext.getSharedPreferences("formatMigrationTest", 0)
        try {
            fixture.edit().clear().putInt("photoFormat", 1).putBoolean("rawVideo", true).commit()
            val migrated = CaptureSettings.load(fixture)
            check(migrated.photoFormat == 2 && migrated.rawVideoEnabled && migrated.rawVideo)
            migrated.rawVideoEnabled = false
            check(!CaptureSettings(migrated).rawVideo)
            migrated.save(fixture)
            check(!CaptureSettings.load(fixture).rawVideo)
        } finally { fixture.edit().clear().commit() }
    }
}
