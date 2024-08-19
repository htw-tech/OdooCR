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

/*! \class EnterInvoiceActivity
    \brief Activity for entering invoice details either by capturing an image, choosing from the gallery, or manually entering the data.
 */
class EnterInvoiceActivity : AppCompatActivity() {

    private val TAG = "EnterInvoiceActivity"
    private val CAMERA_PERMISSION_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002
    private val GALLERY_REQUEST_CODE = 1003

    private lateinit var invoiceProcessor: InvoiceProcessor

    /*! \brief Called when the activity is starting.
        \param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
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

    /*! \brief Copies an invoice template to the gallery. */
    private fun copyInvoiceTemplateToGallery() {
        // Implementation as before
    }

    /*! \brief Checks if the camera permission is granted.
        \return True if the camera permission is granted, false otherwise.
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*! \brief Requests the camera permission. */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    /*! \brief Opens the camera to capture an image. */
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

    /*! \brief Opens the gallery to choose an image. */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    /*! \brief Handles the result of a permission request.
        \param requestCode The request code passed in requestPermissions(android.app.Activity, String[], int).
        \param permissions The requested permissions. Never null.
        \param grantResults The grant results for the corresponding permissions which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
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

    /*! \brief Handles the result of an activity result.
        \param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
        \param resultCode The integer result code returned by the child activity through its setResult().
        \param data An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
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

    /*! \brief Processes the captured or selected image.
        \param bitmap The bitmap of the image to process.
     */
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
