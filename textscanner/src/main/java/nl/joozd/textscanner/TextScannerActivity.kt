package nl.joozd.textscanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import nl.joozd.textscanner.analyzer.CrewNamesCollector
import nl.joozd.textscanner.analyzer.ImageAnalyzer
import nl.joozd.textscanner.analyzer.ScannedTextProcessor
import nl.joozd.textscanner.databinding.ActivityTextScannerBinding
import java.util.concurrent.Executors

/**
 * Launch this activity for result to receive scanned texts.
 * best create the launch Intent with [createLaunchIntent]
 * It needs to know what type of text to scan (currently supports only KLM FlightDeck app) and what the tags of the data to be returned should be.
 * The amount of tags provided will also determine the amount of data expected, if these do not match this will cause an error.
 */
class TextScannerActivity : AppCompatActivity() {
    //TODO add a one-time popup stating that this is an experimental function, what is supported and what isn't.

    // The identifiers of the data that is expected in the result
    // Initialized lazy because intent not ready when construction this
    private val expectedStringArrayListResults by lazy { intent.extras?.getStringArrayList(EXPECTED_STRING_ARRAY_LISTS) ?: listOf(DEFAULT_ID) }
    private val scannedTextProcessor: ScannedTextProcessor by lazy {
        when (intent.extras?.getString(TYPE_OF_PROCESSOR)) {
            CREW_NAMES_FROM_KLM_FLIGHTDECK -> CrewNamesCollector()
            else -> error("No valid processor provided: ${intent.extras?.getString(TYPE_OF_PROCESSOR)}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityTextScannerBinding.inflate(layoutInflater).apply{
            checkPermissionAndRun {
                startPreview(this)
            }


            startScanButton.setOnClickListener {
                checkPermissionAndRun {
                    startScanButton.visibility = View.GONE
                    scanningProgress.visibility = View.VISIBLE
                    startScanning(this)
                }
            }

            setContentView(root)
        }
    }

    private fun checkPermissionAndRun(block: () -> Any) {
        if(allPermissionsGranted())
            block()

        else requestPermissions()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        permissionsLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun startPreview(binding: ActivityTextScannerBinding) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun startScanning(binding: ActivityTextScannerBinding) {
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        // Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //analyze
            val analyze = ImageAnalysis.Builder()
                //.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(executor, ImageAnalyzer{ text ->
                        runBlocking { scannedTextProcessor.addScan(text) } // running this blocking to prevent race conditions in hashmap. Only takes like 2 millis.
                        binding.scanningProgress.progress = scannedTextProcessor.progress
                        if (scannedTextProcessor.isDone) {
                            clearAnalyzer()
                            setResult(RESULT_OK, makeResult(scannedTextProcessor))
                            finish()
                        }
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, analyze)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Puts all results from [scannedTextProcessor] into an Intent which can be set as a result for this activity
     */
    private fun makeResult(scannedTextProcessor: ScannedTextProcessor): Intent = Intent().apply{
        val results = scannedTextProcessor.results.iterator()
        expectedStringArrayListResults.forEach {id ->
            putStringArrayListExtra(id, ArrayList(results.next()))
        }
    }



    private val permissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            }
        }

    companion object{
        private const val EXPECTED_STRING_ARRAY_LISTS = "EXPECTED_STRING_ARRAY_LISTS"
        private const val TYPE_OF_PROCESSOR = "TYPE_OF_PROCESSOR"

        const val DEFAULT_ID = "DEFAULT_ID"



        // Supported ScannedTextProcessors:
        const val CREW_NAMES_FROM_KLM_FLIGHTDECK = "CREW_NAMES_FROM_KLM_FLIGHTDECK"

        private const val TAG = "TextScannerActivity"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).toTypedArray()


        fun createLaunchIntent(context: Context, wantedType: String, expectedResults: List<String>): Intent =
            Intent(context, TextScannerActivity::class.java).apply{
                putExtra(TYPE_OF_PROCESSOR, wantedType)
                putStringArrayListExtra(EXPECTED_STRING_ARRAY_LISTS, ArrayList(expectedResults))
            }
    }
}