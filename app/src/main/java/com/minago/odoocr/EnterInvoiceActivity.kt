package com.minago.odoocr

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class EnterInvoiceActivity : AppCompatActivity() {

    private val TAG = "EnterInvoiceActivity"
    private val CAMERA_PERMISSION_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002
    private val GALLERY_REQUEST_CODE = 1003

    private lateinit var invoiceProcessor: InvoiceProcessor
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_invoice)

        invoiceProcessor = InvoiceProcessor(this)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        findViewById<Button>(R.id.btnCaptureImage).setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        findViewById<Button>(R.id.btnChooseFromGallery).setOnClickListener {
            showAssetImageList()
        }

        findViewById<Button>(R.id.btnEnterDataManually).setOnClickListener {
            startActivity(Intent(this, ManualEntryActivity::class.java))
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

    private fun showAssetImageList() {
        lifecycleScope.launch(Dispatchers.Default) {
            val assetImages = assets.list("") ?: emptyArray()
            val imageList = assetImages.filter { it.endsWith(".jpg") || it.endsWith(".png") }

            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@EnterInvoiceActivity)
                    .setTitle("Select an image")
                    .setItems(imageList.toTypedArray()) { _, which ->
                        val selectedImage = imageList[which]
                        loadImageFromAssets(selectedImage)
                    }
                    .show()
            }
        }
    }

    private fun loadImageFromAssets(imageName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = assets.open(imageName)
                val imageBitmap = BitmapFactory.decodeStream(inputStream)
                processImage(imageBitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image from assets", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EnterInvoiceActivity, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                showProgress()
                updateProgress(0)

                val result = withContext(Dispatchers.Default) {
                    var progress = 0
                    val progressJob = launch {
                        while (isActive && progress < 95) {
                            delay(100)
                            progress += 1
                            updateProgress(progress)
                        }
                    }

                    val processingResult = invoiceProcessor.processInvoice(bitmap)
                    progressJob.cancel()
                    processingResult
                }

                // Ensure smooth progress to 100%
                for (i in progressBar.progress..99) {
                    updateProgress(i)
                    delay(20)
                }
                updateProgress(100)
                delay(200) // Show 100% for a moment

                hideProgress()
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@EnterInvoiceActivity, ResultActivity::class.java).apply {
                        putExtra("INVOICE_RESULT", result)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
                hideProgress()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EnterInvoiceActivity, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun showProgress() {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE
            progressBar.progress = 0
            progressText.text = "0%"
        }
    }

    private suspend fun hideProgress() {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
        }
    }

    private suspend fun updateProgress(progress: Int) {
        withContext(Dispatchers.Main) {
            progressBar.progress = progress
            progressText.text = "$progress%"
        }
    }
}