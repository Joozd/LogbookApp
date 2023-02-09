package nl.joozd.logbookapp.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.data.importing.merging.mergeFlightsLists
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.databinding.ActivitySharingBinding
import nl.joozd.logbookapp.exceptions.CloudException
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.sharing.P2PSessionMetaData
import nl.joozd.logbookapp.sharing.makeQRCodeFromStringAndPutInImageView
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.QR.QRFunctions
import nl.joozd.serializing.packSerializable
import nl.joozd.serializing.unpackSerialized


class SharingActivity : JoozdlogActivity() {
    // using a property because I don't know how to pass a parameter to [qrCodeReaderLauncher] and am too lazy to find out.
    // It's not too complicated so it's fine for now.
    private var replace: Boolean? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivitySharingBinding.inflate(layoutInflater).apply{
            setSupportActionBarWithReturn(sharingToolbar)?.apply {
                setDisplayShowHomeEnabled(true)
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.transfer_data)
            }
            initializeSendButton()
            initializeReceiveButton()
        }.root)
    }

    private fun ActivitySharingBinding.initializeSendButton() {
        sendButton.setOnClickListener {
            sendButtonClicked()
        }
    }

    private fun ActivitySharingBinding.initializeReceiveButton() {
        receiveButton.setOnClickListener {
            askMergeOrReplaceThenLaunchScanner()
            hideButtons()
        }
    }

    private fun launchQRCodeLauncher() {
        qrCodeReaderLauncher.launch(ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("SCANNEN MET JE HOOFD aub")
            setBeepEnabled(true)
            setOrientationLocked(false)
        })
    }

    private fun ActivitySharingBinding.sendButtonClicked() {
        hideButtons()
        loadingCircle.visibility = View.VISIBLE
        lifecycleScope.launch {
            val data = withContext(DispatcherProvider.default()) { packSerializable(FlightRepository.instance.getAllFlights().map { it.toBasicFlight() }) }
            val key = "NO KEY".toByteArray()
            //TODO encrypt data with key
            val id: Long = try{
                Cloud().sendP2PData(data)
            } catch (e: CloudException){
                Log.w("SendP2P", "Error: $e")
                //TODO feedback reason of failure or something to user
                return@launch
            }
            val p2pData = P2PSessionMetaData(id, key)
            loadingCircle.visibility = View.GONE
            makeQRCodeFromStringAndPutInImageView(
                qrCodeImageView,
                QRFunctions.makeJsonStringWithAction(ACTION_IDENTIFIER_XFER, *p2pData.keyDataPairs)
            )
            qrCodeImageView.visibility = View.VISIBLE
        }
    }

    private fun ActivitySharingBinding.hideButtons() {
        sendButton.visibility = View.GONE
        receiveButton.visibility = View.GONE
    }

    private fun ActivitySharingBinding.showButtons() {
        sendButton.visibility = View.VISIBLE
        receiveButton.visibility = View.VISIBLE
    }

    private val qrCodeReaderLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()
            MainScope().launch { // this scope can live longer than activity, needs to keep working in background if activity left
                val amount = handleResult(result.contents)
                Toast.makeText(App.instance, "KLAAR IS KEES ($amount) ", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun handleResult(result: String): Int{
        val p2pData = P2PSessionMetaData.ofJsonString(result)
        val receivedData = Cloud().receiveP2PData(p2pData.sessionID)

        val receivedFlights = unpackFlights(receivedData)
        FlightRepositoryWithUndo.instance.singleUndoableOperation {
            if (replace == true) {
                clear()
                save(receivedFlights)
            } else {
                val allFlights = getAllFlights()
                clear()
                save(mergeFlightsLists(allFlights, receivedFlights))
            }
        }
        return receivedFlights.size
    }

    private suspend fun unpackFlights(receivedData: ByteArray) =
        withContext(DispatcherProvider.default()) {
            unpackSerialized(receivedData)
                .map {
                    Flight(BasicFlight.deserialize(it))
                }
        }

    private fun ActivitySharingBinding.askMergeOrReplaceThenLaunchScanner(){
        AlertDialog.Builder(activity).apply{
            setTitle(R.string.replace_or_merge)
            setMessage(R.string.replace_or_merge_long)
            setPositiveButton(R.string.replace){ _, _ ->
                showAreYouSureYouWantToReplaceDialog()
                launchQRCodeLauncher()
            }
            setNegativeButton(R.string.merge){ _, _ ->
                replace = false
                launchQRCodeLauncher()
            }
            setNeutralButton(android.R.string.cancel){ _, _ ->
                showButtons()
            }
        }.create().show()
    }

    private fun ActivitySharingBinding.showAreYouSureYouWantToReplaceDialog(){
        AlertDialog.Builder(activity).apply{
            setTitle(R.string.are_you_sure_qmk)
            setMessage(R.string.this_will_replace_your_entire_logbook)
            setPositiveButton(android.R.string.ok){ _, _ ->
                replace = true
                launchQRCodeLauncher()
            }
            setNegativeButton(android.R.string.cancel){ _, _ ->
                showButtons()
            }
        }
    }

    companion object{
        private const val ACTION_IDENTIFIER_XFER = "Xfer"
    }
}