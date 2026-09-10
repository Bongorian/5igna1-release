package com.bongorian.signa1.cameraclient

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File
import java.security.MessageDigest

/** Test client intentionally runs as a different package/UID and requests no media permission. */
class CameraClient : Activity() {
    companion object {
        const val TARGET = "com.bongorian.signa1.debug"
        const val RESULT = "com.bongorian.signa1.cameraclient.RESULT"
        val OUTPUT: Uri = Uri.parse("content://com.bongorian.signa1.cameraclient.output/shot")
    }
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        if (saved != null) return
        File(filesDir,"shot").writeText("untouched")
        val video = intent.getBooleanExtra("video", false)
        val action = if (video) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE
        val camera = Intent(action).setPackage(TARGET)
        val canResolve = packageManager.resolveActivity(camera, PackageManager.MATCH_DEFAULT_ONLY) != null
        check(canResolve) { "Explicit package camera not discoverable" }
        if (intent.getBooleanExtra("output", true)) {
            val uri = if (intent.getBooleanExtra("invalid",false)) Uri.parse("https://invalid.example/not-a-content-uri") else if (intent.getBooleanExtra("writeFail",false)) Uri.parse("content://com.bongorian.signa1.cameraclient.output/fail") else OUTPUT
            camera.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            camera.clipData = ClipData.newRawUri("destination", uri)
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!intent.getBooleanExtra("readonly",false)) camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (video) camera.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3)
        if (intent.hasExtra("limit")) camera.putExtra(MediaStore.EXTRA_SIZE_LIMIT,intent.getLongExtra("limit",0))
        startActivityForResult(camera, 41)
    }
    @Suppress("DEPRECATION")
    override fun onActivityResult(request: Int, code: Int, data: Intent?) {
        super.onActivityResult(request, code, data)
        val report = Intent(RESULT).setPackage(TARGET).putExtra("case",intent.getStringExtra("case")).putExtra("code",code)
        try {
            val generic=packageManager.queryIntentActivities(Intent(MediaStore.ACTION_IMAGE_CAPTURE),PackageManager.MATCH_DEFAULT_ONLY)
            report.putExtra("genericIncludesTarget",generic.any { it.activityInfo.packageName==TARGET })
            val video = intent.getBooleanExtra("video",false)
            val output = intent.getBooleanExtra("output",true)
            if (code == RESULT_OK && !output && !video) {
                val thumbnail = requireNotNull(data?.getParcelableExtra<Bitmap>("data"))
                check(thumbnail.width in 1..256 && thumbnail.height in 1..256)
                report.putExtra("thumbnail",true)
            } else if (code == RESULT_OK) {
                val uri = if (output) OUTPUT else requireNotNull(data?.data)
                val bytes = contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                report.putExtra("size",bytes.size)
                report.putExtra("sha256",MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
                if (video) MediaMetadataRetriever().use {
                    it.setDataSource(this, uri)
                    check(it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)=="yes")
                    report.putExtra("duration",it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong())
                    report.putExtra("gps",it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
                } else {
                    contentResolver.openInputStream(uri)!!.use { input ->
                        val exif = android.media.ExifInterface(input)
                        report.putExtra("gps",exif.getAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE))
                    }
                    check(bytes[0]==0xff.toByte() && bytes[1]==0xd8.toByte())
                }
                if (!output) report.putExtra("returnedUri",uri.toString())
            } else report.putExtra("untouched", File(filesDir,"shot").readText()=="untouched")
            report.putExtra("verified",true)
        } catch (error: Exception) { report.putExtra("error",error.toString()) }
        sendBroadcast(report)
        finish()
    }
}

class OutputProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "application/octet-stream"
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (uri.path=="/fail") throw java.io.FileNotFoundException("Deliberately unavailable destination")
        require(uri.path=="/shot")
        return ParcelFileDescriptor.open(File(requireNotNull(context).filesDir,"shot"),ParcelFileDescriptor.parseMode(mode))
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
}
