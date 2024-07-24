package com.minago.odoocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        setContent {
            MaterialTheme {
                CameraScreen()
            }
        }
    }

    @Composable
    fun CameraScreen() {
        var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
        var extractedText by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var debugInfo by remember { mutableStateOf("") }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text("Camera Preview (Simulated)", modifier = Modifier.align(Alignment.Center))
            }

            Button(
                onClick = {
                    try {
                        debugInfo = "Attempting to load image...\n"
                        debugInfo += "Assets found: ${context.assets.list("")?.joinToString(", ") ?: "None"}\n"
                        capturedImage = loadImageFromAssets(context, "invoice_template.jpg")
                        if (capturedImage != null) {
                            errorMessage = null
                            debugInfo += "Image loaded successfully"
                        } else {
                            errorMessage = "Failed to load image: Bitmap is null"
                            debugInfo += "Failed to load image: Bitmap is null"
                        }

                        // Try loading a test text file
                        try {
                            val textContent = context.assets.open("test.txt").bufferedReader().use { it.readText() }
                            debugInfo += "\nTest file content: $textContent"
                        } catch (e: Exception) {
                            debugInfo += "\nFailed to read test file: ${e.message}"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Failed to load image: ${e.message}"
                        debugInfo += "\nException: ${e.message}"
                        Log.e(TAG, "Failed to load image", e)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Capture Image")
            }

            Spacer(modifier = Modifier.height(16.dp))

            capturedImage?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Captured image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { performOCR(it) { text -> extractedText = text } },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Extract Text")
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Text(
                text = "Debug Info:\n$debugInfo",
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (extractedText.isNotEmpty()) {
                Text(
                    text = "Extracted Text:",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = extractedText)
            }
        }
    }


    private fun loadImageFromAssets(context: android.content.Context, fileName: String): Bitmap? {
    return try {
        Log.d(TAG, "Attempting to load image: $fileName")
        context.assets.open(fileName).use { inputStream ->
            Log.d(TAG, "Input stream opened successfully")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                Log.d(TAG, "Image loaded successfully. Dimensions: ${bitmap.width}x${bitmap.height}")
            } else {
                Log.e(TAG, "Failed to decode image from stream")
            }
            bitmap
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading image: ${e.message}", e)
        null
    }
}

    private fun performOCR(bitmap: Bitmap, onTextExtracted: (String) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                Log.d(TAG, "OCR successful: ${visionText.text}")
                onTextExtracted(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed", e)
                Toast.makeText(this, "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}