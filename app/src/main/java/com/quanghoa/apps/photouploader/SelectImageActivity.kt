package com.quanghoa.apps.photouploader

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class SelectImageActivity : AppCompatActivity() {

    private val TAG by lazy { SelectImageActivity::class.java.simpleName }

    private val REQUEST_CODE_IMAGE = 100
    private val REQUEST_CODE_PERMISSIONS = 101

    private val KEY_PERMISSIONS_REQUEST_COUNT = "KEY_PERMISSIONS_REQUEST_COUNT"
    private val MAX_NUMBER_REQUEST_PERMISSIONS = 2

    private val permissions = Arrays.asList(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var permissionsRequestCount: Int = 0
    private lateinit var selectImageButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        selectImageButton = findViewById(R.id.selectImage)

        savedInstanceState?.let {
            permissionsRequestCount = it.getInt(KEY_PERMISSIONS_REQUEST_COUNT, 0)
        }

        // Make sure the app has correct permissions to run
        requestPermissionsIfNecessary()

        // Create request to get image from filesystem when button clicked
        selectImageButton.setOnClickListener {
            val chooseIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(chooseIntent, REQUEST_CODE_IMAGE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(KEY_PERMISSIONS_REQUEST_COUNT, permissionsRequestCount)
    }

    /**
     * Permission checking
     */
    fun requestPermissionsIfNecessary() {
        if (!checkAllPermission()) {
            if (permissionsRequestCount < MAX_NUMBER_REQUEST_PERMISSIONS) {
                permissionsRequestCount += 1
                ActivityCompat.requestPermissions(
                    this, permissions.toTypedArray(),
                    REQUEST_CODE_PERMISSIONS
                )
            } else {
                Toast.makeText(this, R.string.set_permissions_in_settings, Toast.LENGTH_LONG).show()
                selectImageButton.isEnabled = true
            }
        }
    }

    private fun checkAllPermission(): Boolean {
        var hasPermissions = true
        for (permission in permissions) {
            hasPermissions = isPermissionGranted(permission)
        }
        return hasPermissions
    }

    private fun isPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            requestPermissionsIfNecessary()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_IMAGE -> data?.let {
                    handleImageRequestResult(it)
                }
                else -> Log.d(TAG, "Unknown request code")
            }
        } else {
            Log.d(TAG, String.format("Unexpected Result code %s", resultCode))
        }
    }

    private fun handleImageRequestResult(intent: Intent) {
        // If clipdata is available , we use it, otherwise we use data
        val imageUri: Uri? = intent.clipData?.let {
            it.getItemAt(0).uri
        } ?: intent.data

        if (imageUri == null) {
            Log.e(TAG, "Invalid input image uri")
            return
        }

        val filterIntent = Intent(this, BlurActivity::class.java).apply {
            putExtra(KEY_IMAGE_URI, imageUri.toString())
        }
        startActivity(filterIntent)
    }
}

