package nl.joozd.textscanner.analyzer

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ImageAnalyzer(private val onSuccessfulScanListener: OnSuccessfulScanListener): ImageAnalysis.Analyzer {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    imageProxy.close()
                    this.onSuccessfulScanListener(visionText)
                }
                .addOnFailureListener { e ->
                    Log.w("ImageAnalyzer", "Scanning error ${e.stackTrace}")
                }

        }
    }

    fun interface OnSuccessfulScanListener{
        operator fun invoke(scannedText: Text)
    }
}
