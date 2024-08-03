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

class EnterInvoiceActivity : AppCompatActivity() {

    private val TAG = "EnterInvoiceActivity"
    private val CAMERA_PERMISSION_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002
    private val GALLERY_REQUEST_CODE = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_invoice)

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
            // We'll implement this later
        }
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

    private fun copyInvoiceTemplateToGallery() {
        try {
            val inputStream = assets.open("invoice_template.jpg")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val fileName = "invoice_template_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/OdooCR")
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        Log.d(TAG, "Invoice template copied to gallery: $uri")
                        Toast.makeText(this, "Invoice template added to gallery", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Failed to compress and save bitmap")
                    }
                } ?: Log.e(TAG, "Failed to open output stream")
            } else {
                Log.e(TAG, "Failed to create new MediaStore record.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying invoice template to gallery", e)
            Toast.makeText(this, "Error adding invoice template to gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
        // Here you would use your InvoiceProcessor to process the image
        // For now, we'll just show a toast
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show()

        // TODO: Implement image processing using InvoiceProcessor
        // val result = invoiceProcessor.processInvoice(bitmap)
        // Display or use the result as needed
    }
}