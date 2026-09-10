package com.bongorian.signa1

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

/** Headless integration checks. The caller APK uses a separate UID with no camera/media permissions. */
internal object CameraIntentChecks {
    fun run(test: DeviceChecks): String {
        val original = test.activity!!
        val context = test.targetContext
        val prefs = context.getSharedPreferences("signal",0)
        val before = CaptureSettings.load(prefs)
        val last = prefs.getString("last",null)
        val sound = prefs.getBoolean("sound",false)
        val results = AtomicReference<Intent?>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) { results.set(intent) }
        }
        context.registerReceiver(receiver,IntentFilter("com.bongorian.signa1.cameraclient.RESULT"),Context.RECEIVER_EXPORTED)
        val monitor = test.addMonitor(MainActivity::class.java.name,null,false)
        val report = StringBuilder()
        var current: MainActivity? = null
        try {
            test.runOnMainSync { original.finish() }
            test.await("initial camera closed", { original.isDestroyed }, 5000)
            val constrained = CaptureSettings(before).apply {
                photoFormat=2;rawVideoEnabled=true;rawVideo=true;location=true
                photoSize="640x480";videoKey="recommended";codec="video/hevc"
            }
            // A regular camera launch selects its mode and keeps the normal storage path.
            for ((action, video) in listOf(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA to false,
                MediaStore.INTENT_ACTION_VIDEO_CAMERA to true)) {
                val camera=test.startActivitySync(Intent(action).setClassName(context.packageName,MainActivity::class.java.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
                check(camera.videoMode==video && camera.externalCapture==null)
                if (!video) {
                    test.await("ordinary camera ready",{camera.ready},20000)
                    test.runOnMainSync {
                        camera.startActivity(Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
                            .setClassName(context.packageName,MainActivity::class.java.name)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    }
                    test.await("reused video launch",{camera.videoMode},5000)
                    check(monitor.lastActivity===camera)
                }
                test.runOnMainSync {camera.finish()}
                test.await("normal launch closed",{camera.isDestroyed},5000)
            }
            report.append("ordinary photo/video launch: PASS\n")
            constrained.save(prefs)
            prefs.edit().putBoolean("sound",false).commit()
            // Both ordinary camera actions must resolve to the public activity.
            for (action in listOf(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,MediaStore.INTENT_ACTION_VIDEO_CAMERA,
                MediaStore.ACTION_IMAGE_CAPTURE,MediaStore.ACTION_VIDEO_CAPTURE)) {
                check(context.packageManager.resolveActivity(Intent(action).setPackage(context.packageName),PackageManager.MATCH_DEFAULT_ONLY)!=null)
            }
            fun launch(name: String, video: Boolean=false, output: Boolean=true, invalid: Boolean=false, readonly: Boolean=false, writeFail: Boolean=false): MainActivity? {
                results.set(null)
                val previous = monitor.lastActivity
                test.runOnMainSync {
                    context.startActivity(Intent().setComponent(ComponentName("com.bongorian.signa1.cameraclient",
                        "com.bongorian.signa1.cameraclient.CameraClient"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("case",name).putExtra("video",video).putExtra("output",output)
                        .putExtra("invalid",invalid).putExtra("readonly",readonly).putExtra("writeFail",writeFail))
                }
                test.await("external launch $name",{ monitor.lastActivity !== previous || results.get()!=null },15000)
                if (invalid || readonly) return null
                val activity = monitor.lastActivity as MainActivity
                current = activity
                test.await("external ready $name",{activity.ready || results.get()!=null},20000)
                check(results.get()==null) { "Unexpected early result: ${results.get()?.extras}" }
                check(activity.externalCapture != null && activity.videoMode==video && !activity.tapMode)
                check(!activity.captureRawVideo && activity.capturePhotoFormat==0 && !activity.settings.location)
                check(!activity.canCycleFormat())
                check(activity.engine.settings.codec=="video/avc")
                return activity
            }
            fun result(name: String, ok: Boolean): Intent {
                test.await("caller result $name",{results.get()?.getStringExtra("case")==name},15000)
                val intent=results.get()!!
                check(intent.getIntExtra("code",99)==if(ok) Activity.RESULT_OK else Activity.RESULT_CANCELED) { "$name: ${intent.extras}" }
                check(intent.getBooleanExtra("verified",false)) { "$name: ${intent.getStringExtra("error")}" }
                if(!ok) check(intent.getBooleanExtra("untouched",false))
                check(intent.getStringExtra("gps")==null)
                report.append(name).append(": PASS; generic capture candidate=").append(intent.getBooleanExtra("genericIncludesTarget",false)).append("\n")
                return intent
            }
            fun capture(a: MainActivity): String {
                test.runOnMainSync { a.shoot() }
                test.await("private staged capture",{a.externalCapture!!.model.source!=null || results.get()!=null},25000)
                check(results.get()==null) { "Capture failed: ${results.get()?.extras}" }
                test.await("capture finalization",{!a.engine.photoBusy},5000)
                val model=a.externalCapture!!.model
                val bytes=model.session.file(model.source!!)!!.readBytes()
                return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            }

            var a=launch("jpeg")!!
            test.runOnMainSync { a.commitEffects(a.effectState.single(Effects.ROW_ERROR).amount(.7f)) }
            val adjusted=a.effectState.encode()
            capture(a)
            val old=a.externalCapture!!.model.source!!
            test.runOnMainSync {a.externalCapture!!.model.retake()}
            test.await("retake camera ready",{a.ready},15000)
            check(a.externalCapture!!.model.session.file(old)==null)
            val expected=capture(a)
            val model=a.externalCapture!!.model
            val previous=a
            test.runOnMainSync {a.recreate()}
            test.await("review recreation",{monitor.lastActivity!==previous && (monitor.lastActivity as? MainActivity)?.resumed==true},15000)
            a=monitor.lastActivity as MainActivity
            current=a
            check(a.externalCapture!!.model===model && model.source!=null)
            check(a.effectState.encode()==adjusted)
            test.runOnMainSync {model.accept()}
            check(result("jpeg",true).getStringExtra("sha256")==expected)

            a=launch("thumbnail",output=false)!!
            capture(a)
            test.runOnMainSync {a.externalCapture!!.model.accept()}
            check(result("thumbnail",true).getBooleanExtra("thumbnail",false))

            a=launch("cancel")!!
            val session=a.externalCapture!!.model.session
            test.runOnMainSync {a.externalCapture!!.cancel()}
            result("cancel",false)
            check(!session.directory.exists())

            launch("readonly",readonly=true)
            result("readonly",false)
            launch("invalid",invalid=true)
            result("invalid",false)
            a=launch("write-failure",writeFail=true)!!
            capture(a)
            test.runOnMainSync {a.externalCapture!!.model.accept()}
            result("write-failure",false)

            a=launch("video",video=true)!!
            val videoHash=capture(a) // caller's three-second limit stops recording automatically
            check(!a.engine.recording)
            val videoBytes=a.externalCapture!!.model.session.file(a.externalCapture!!.model.source!!)!!.readBytes()
            test.runOnMainSync {a.externalCapture!!.model.accept()}
            val movie=result("video",true)
            check(movie.getStringExtra("sha256")==videoHash)
            check(movie.getLongExtra("duration",0) in 1500..5000)

            a=launch("video-uri",video=true,output=false)!!
            // Exercise grant/publication independently of the already-verified recorder.
            val m=a.externalCapture!!.model
            val staged=m.session.create("mp4")
            m.session.file(staged)!!.writeBytes(videoBytes)
            test.runOnMainSync { m.saved(staged,true); m.accept() }
            val uriResult=result("video-uri",true)
            check(uriResult.getStringExtra("sha256")==videoHash)
            context.contentResolver.delete(android.net.Uri.parse(uriResult.getStringExtra("returnedUri")),null,null)

            val after=CaptureSettings.load(prefs)
            check(after.photoFormat==2 && after.rawVideo && after.location && after.codec=="video/hevc")
            check(prefs.getString("last",null)==last)
            report.append("RAW/location/codec preferences and last media preserved; caller grants and cancellation verified")
            return report.toString()
        } catch (error: Throwable) {
            val a=current
            throw AssertionError("$report\nCurrent: resumed=${a?.resumed} ready=${a?.ready} frame=${a?.engine?.frameSeen} attached=${a?.engine?.attached} status=${a?.status?.text} blocked=${a?.externalCapture?.blocked}", error)
        } finally {
            test.runOnMainSync {current?.takeUnless {it.isDestroyed || it.isFinishing}?.externalCapture?.cancel()}
            test.removeMonitor(monitor)
            context.unregisterReceiver(receiver)
            before.save(prefs)
            prefs.edit().putBoolean("sound",sound).apply()
            test.activity=original
        }
    }
}
