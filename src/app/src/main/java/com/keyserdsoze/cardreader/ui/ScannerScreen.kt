package com.keyserdsoze.cardreader.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.keyserdsoze.cardreader.model.CodeFormat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen(onScanned: (String, CodeFormat) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        if (granted) CameraPreview(onScanned)
        else Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Camera permission is needed only while scanning.", color = Color.White)
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Allow camera")
            }
        }
        Box(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Inquadra il codice", color = Color.White, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White) }
            }
            Text(
                "QR, EAN, UPC, Code 128, Data Matrix, Aztec, PDF417 e altri",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onScanned: (String, CodeFormat) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val claimed = remember { AtomicBoolean(false) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = future.get()
            provider = cameraProvider
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analyzerExecutor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null || claimed.get()) {
                    proxy.close()
                } else {
                    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val match = barcodes.firstNotNullOfOrNull { barcode ->
                                val value = barcode.rawValue?.takeIf(String::isNotBlank) ?: return@firstNotNullOfOrNull null
                                val format = BarcodeFormats.fromMlKit(barcode.format) ?: return@firstNotNullOfOrNull null
                                value to format
                            }
                            if (match != null && claimed.compareAndSet(false, true)) onScanned(match.first, match.second)
                        }
                        .addOnCompleteListener { proxy.close() }
                }
            }
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            scanner.close()
            analyzerExecutor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
