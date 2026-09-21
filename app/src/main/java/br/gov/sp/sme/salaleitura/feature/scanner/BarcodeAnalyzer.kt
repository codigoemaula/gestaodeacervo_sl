package br.gov.sp.sme.salaleitura.feature.scanner

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(private val onDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E, Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128
        ).build()
    )
    private var lastValue: String? = null
    private var lastAt: Long = 0

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstNotNullOfOrNull { it.rawValue }
                val now = SystemClock.elapsedRealtime()
                if (value != null && (value != lastValue || now - lastAt > 1800)) {
                    lastValue = value; lastAt = now; onDetected(value)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() = scanner.close()
}
