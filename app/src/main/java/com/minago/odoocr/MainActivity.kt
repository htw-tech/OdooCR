package com.minago.odoocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
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
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.util.Log
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    //var extractedText by remember { mutableStateOf("") }
    var extractedText by remember { mutableStateOf<Map<String, String>>(emptyMap()) }


    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable("enterInvoice") {
            EnterInvoiceScreen(navController)
        }
        composable("captureImage") {
            CameraScreen(navController) { image, extractedFields ->
                capturedImage = image
                extractedText = extractedFields
                navController.navigate("result")
            }
        }
        composable("result") {
            ResultScreen(navController, capturedImage, extractedText as Map<String, String>)
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
fun CameraScreen(navController: NavController, onImageCaptured: (Bitmap?, Map<String, String>) -> Unit) {
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                coroutineScope.launch {
                    val result = InvoiceProcessor.processInvoice(context, "invoice_template.jpg")
                    isProcessing = false
                    onImageCaptured(null, result) // Note: We're not capturing a real image here
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            Text(if (isProcessing) "Processing..." else "Capture & Extract")
        }
    }
}

@Composable
fun ResultScreen(navController: NavController, capturedImage: Bitmap?, extractedFields: Map<String, String>) {
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
            text = "Extracted Fields:",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        extractedFields.forEach { (key, value) ->
            Text(
                text = "$key: $value",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }
    }
}