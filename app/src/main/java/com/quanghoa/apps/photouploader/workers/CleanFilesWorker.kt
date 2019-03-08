package com.quanghoa.apps.photouploader.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.quanghoa.apps.photouploader.ImageUtils

private const val LOG_TAG = "CleanFilesWorker"

class CleanFilesWorker(cxt: Context, parameters: WorkerParameters) : Worker(cxt, parameters) {

    override fun doWork(): Result = try {
        // Sleep for debugging purpose

        Thread.sleep(3000)
        Log.d(LOG_TAG, "Cleaning files")

        ImageUtils.clearFiles(applicationContext)

        Log.d(LOG_TAG, "Success")
        Result.success()
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work:${e.message}", e)
        Result.failure()
    }

}