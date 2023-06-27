package nl.joozd.textscanner.analyzer

import com.google.mlkit.vision.text.Text

/**
 * Use a ScannedTextProcessor to process scanned Text and produce a result from it.
 * A ScannedTextProcessor will keep on receiving scanned texts until [isDone] is true.
 */
interface ScannedTextProcessor {
    // False when more input is needed, true when it is done.
    val isDone: Boolean

    /**
     * The progress of the process, 0 to 100, used for progress bar.
     */
    val progress: Int

    /**
     * The result of the scan. Should only be read after isDone is set to true.
     * It is the responsibility of the user to not get more results than available
     * (ie if this only gives a single list of strings, only the first iteration will be requested)
     */
    val results: Iterable<List<String>>

    fun addScan(scannedText: Text) //
}