package com.quanghoa.apps.photouploader.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.quanghoa.apps.photouploader.ImageUtils

private const val LOG_TAG = "UploadWorker"
private const val KEY_ZIP_PATH = "ZIP_PATH"

class UploadWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result = try {
        // Sleep for debugging purpose
        Thread.sleep(3000)
        Log.d(LOG_TAG, "Uploading file!")

        val zipPath = inputData.getString(KEY_ZIP_PATH)

        ImageUtils.uploadFile(Uri.parse(zipPath))

        Log.d(LOG_TAG, "Success!")
        Result.success()
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work: ${e.message}", e)
        Result.failure()
    }
}