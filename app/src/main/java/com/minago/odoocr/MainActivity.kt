package com.minago.odoocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var extractedText by remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable("enterInvoice") {
            EnterInvoiceScreen(navController)
        }
        composable("captureImage") {
            CameraScreen(navController) { image, text ->
                capturedImage = image
                extractedText = text
                navController.navigate("result")
            }
        }
        composable("result") {
            ResultScreen(navController, capturedImage, extractedText)
        }
    }
}


@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            "Please choose one of these options",
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { navController.navigate("enterInvoice") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("ENTER INVOICE")
        }

        Button(
            onClick = { /* TODO: Implement later */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SCANNED INVOICES")
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "designed by minagoTECH Group",
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun EnterInvoiceScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        ) {
            Text("Previous")
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "Please choose one of these options",
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { navController.navigate("captureImage") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("CAPTURE IMAGE")
        }

        Button(
            onClick = { /* TODO: Implement later */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CHOOSE FROM GALLERY")
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "designed by minagoTECH Group",
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
@Composable
fun CameraScreen(navController: NavController, onImageCaptured: (Bitmap?, String) -> Unit) {
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        ) {
            Text("Previous")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Text("CAMERA PREVIEW", modifier = Modifier.align(Alignment.Center))
        }

        Button(
            onClick = {
                isProcessing = true
                captureAndProcessImage(context) { image, text ->
                    isProcessing = false
                    onImageCaptured(image, text)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            Text(if (isProcessing) "Processing..." else "Capture&Extract")
        }
    }
}

@Composable
fun ResultScreen(navController: NavController, capturedImage: Bitmap?, extractedText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        ) {
            Text("Previous")
        }

        capturedImage?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Captured image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 16.dp)
            )
        }

        Text(
            text = extractedText,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        )
    }
}

private fun captureAndProcessImage(
    context: android.content.Context,
    onResult: (Bitmap?, String) -> Unit
) {
    try {
        val capturedImage = loadImageFromAssets(context, "invoice_template.jpg")
        if (capturedImage != null) {
            performOCR(capturedImage) { text ->
                onResult(capturedImage, text)
            }
        } else {
            onResult(null, "Failed to load image")
        }
    } catch (e: Exception) {
        val errorMessage = "Failed to load or process image: ${e.message}"
        Log.e(TAG, errorMessage, e)
        onResult(null, errorMessage)
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
    Log.d(TAG, "Starting OCR process")

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            Log.d(TAG, "OCR successful: ${visionText.text}")
            onTextExtracted(visionText.text)
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "OCR failed", e)
            onTextExtracted("OCR failed: ${e.message}")
        }
}