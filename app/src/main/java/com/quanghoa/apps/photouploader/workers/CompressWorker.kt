package com.quanghoa.apps.photouploader.workers

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.quanghoa.apps.photouploader.ImageUtils

private const val LOG_TAG = "CompressWorker"
private const val KEY_IMAGE_PATH = "IMAGE_PATH"
private const val KEY_ZIP_PATH = "ZIP_PATH"

class CompressWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result = try {
        // Sleep for debugging purpose
        Thread.sleep(3000)
        Log.d(LOG_TAG, "Compressing filtes")

        val imagePaths = inputData.keyValueMap
            .filter { it.key.startsWith(KEY_IMAGE_PATH) }
            .map { it.value as String }

        val zipFileUri = ImageUtils.createZipFile(applicationContext, imagePaths.toTypedArray())

        val    outputData = Data.Builder()
                  .putString(KEY_ZIP_PATH, zipFileUri.path)
                  .build()

        Log.d(LOG_TAG, "Success!")
        Result.success(outputData)

    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work ${e.message}", e)
        Result.failure()
    }

}