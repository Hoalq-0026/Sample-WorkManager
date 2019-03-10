package com.quanghoa.apps.photouploader

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import androidx.work.*
import com.quanghoa.apps.photouploader.workers.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val GALLERY_REQUEST_CODE = 300
        private const val PERMISSION_REQUEST_CODE = 301

        private const val MAX_NUMBER_REQUEST_PERMISSION = 2

        private const val UNIQUE_WORK_NAME = "UNIQUE_WORK_NAME"
        private const val WORK_TAG = "WORK_TAG"

        private const val IMAGE_TYPE = "image/*"
        private const val IMAGE_CHOOSER_TITLE = "Select Picture"

        private val PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private var permissionRequestCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUi()

        requestPermissionsIfNecessary()

        observeWork()
    }

    private fun initUi() {
        uploadingGroup.visibility = View.GONE

        pickPhotosButton.setOnClickListener { showPhotoPicker() }
        cancelButton.setOnClickListener { WorkManager.getInstance().cancelUniqueWork(UNIQUE_WORK_NAME) }
    }

    private fun showPhotoPicker() {
        val intent = Intent().apply {
            type = IMAGE_TYPE
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            action = Intent.ACTION_OPEN_DOCUMENT
        }
        startActivityForResult(Intent.createChooser(intent, IMAGE_CHOOSER_TITLE), GALLERY_REQUEST_CODE)
    }

    private fun observeWork() {
        val statuses = WorkManager.getInstance().getWorkInfosByTagLiveData(WORK_TAG)
        statuses.observe(this, Observer<List<WorkInfo>> { listOfWorkInfo ->
            if (listOfWorkInfo.isNullOrEmpty()) {
                return@Observer
            }

            val workInfo = listOfWorkInfo[0]
            val isWorkActive = workInfo.state?.isFinished == false
            val uploadVisibility = if (isWorkActive) View.VISIBLE else View.GONE
            uploadingGroup.visibility = uploadVisibility
        })
    }

    private fun requestPermissionsIfNecessary() {
        if (!hasRequiredPermissions()) {
            askForPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissionResults = PERMISSIONS.map { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED
        }

        return permissionResults.all { isGranted -> isGranted }
    }

    private fun askForPermissions() {
        if (permissionRequestCount < MAX_NUMBER_REQUEST_PERMISSION) {
            permissionRequestCount += 1
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        } else {
            pickPhotosButton.isEnabled = false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            requestPermissionsIfNecessary()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {

            val applySepiaFilter = buildSepiaFilterRequests(data)

            val zipFiles = OneTimeWorkRequest.Builder(CompressWorker::class.java).build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadZip = OneTimeWorkRequest.Builder(UploadWorker::class.java)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build()

            val cleanFiles = OneTimeWorkRequest.Builder(CleanFilesWorker::class.java).build()

            val workManager = WorkManager.getInstance()

            workManager.beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, cleanFiles)
                .then(applySepiaFilter)
                .then(zipFiles)
                .then(uploadZip)
                .enqueue()
        }
    }

    private fun buildSepiaFilterRequests(intent: Intent): List<OneTimeWorkRequest> {
        val filterRequests = mutableListOf<OneTimeWorkRequest>()

        intent.clipData?.run {
            for (i in 0 until itemCount) {
                val imageUri = getItemAt(i).uri

                val filterRequest = OneTimeWorkRequest.Builder(FilterWorker::class.java)
                    .setInputData(buildInputDataForFilter(imageUri, i))
                    .build()
                filterRequests.add(filterRequest)
            }
        }

        intent.data?.run {
            val filterWorkRequest = OneTimeWorkRequest.Builder(FilterWorker::class.java)
                .setInputData(buildInputDataForFilter(this, 0))
                .build()

            filterRequests.add(filterWorkRequest)
        }
        return filterRequests
    }

    private fun buildInputDataForFilter(imageUri: Uri?, index: Int): Data {
        val builder = Data.Builder()
        if (imageUri != null) {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
            builder.putInt(KEY_IMAGE_INDEX, index)
        }
        return builder.build()
    }
}
