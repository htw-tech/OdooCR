package com.minago.odoocr

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.ProgressDialog
import android.os.Handler
import android.os.Looper


class EnterInvoiceActivity : AppCompatActivity() {

    private val TAG = "EnterInvoiceActivity"
    private val CAMERA_PERMISSION_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002
    private val GALLERY_REQUEST_CODE = 1003

    private lateinit var invoiceProcessor: InvoiceProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_invoice)

        invoiceProcessor = InvoiceProcessor(this)

        copyInvoiceTemplateToGallery()

        findViewById<Button>(R.id.btnCaptureImage).setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        findViewById<Button>(R.id.btnChooseFromGallery).setOnClickListener {
            openGallery()
        }

        findViewById<Button>(R.id.btnEnterDataManually).setOnClickListener {
            startActivity(Intent(this, ManualEntryActivity::class.java))
        }
    }

    private fun copyInvoiceTemplateToGallery() {
        // Implementation as before
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun openCamera() {
        Log.d(TAG, "Attempting to open camera")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        processImage(imageBitmap)
                    } else {
                        Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    val selectedImage = data?.data
                    if (selectedImage != null) {
                        val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage)
                        processImage(imageBitmap)
                    } else {
                        Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Image processing in progress...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val result = invoiceProcessor.processInvoice(bitmap)
                progressDialog.dismiss()
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("INVOICE_RESULT", result)
                }
                startActivity(intent)
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e(TAG, "Error processing image", e)
                Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, 1000) // Simulating processing time
    }
}