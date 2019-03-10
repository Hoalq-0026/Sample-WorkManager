package com.quanghoa.apps.photouploader.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quanghoa.apps.photouploader.KEY_IMAGE_URI
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * Saves the image to a permanent file
 */
class SaveImageToFileWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private val TAG by lazy { SaveImageToFileWorker::class.java.simpleName }
    private val Title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z",
            Locale.getDefault())

    override fun doWork(): Result {
        makeStatusNotification("Saving image", applicationContext)
        sleep()

        val resolver = applicationContext.contentResolver
        return try {
            val sourceUri = inputData.getString(KEY_IMAGE_URI)
            val bitmap = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(sourceUri)))
            val imageUrl = MediaStore.Images.Media.insertImage(
                    resolver, bitmap, Title, dateFormatter.format(Date()))
            if (!imageUrl.isNullOrEmpty()) {
                val outputData = workDataOf(KEY_IMAGE_URI to imageUrl)
                Result.success(outputData)
            } else {
                Log.e(TAG, "Writing to MediaStore failed.")
                Result.failure()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to save image to Gallery", exception)
            Result.failure()
        }

    }

}