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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.comms.Client
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithSpecializedFunctions
import nl.joozd.logbookapp.databinding.ActivitySharingBinding
import nl.joozd.logbookapp.exceptions.CloudException
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.sharing.P2PSessionMetaData
import nl.joozd.logbookapp.sharing.makeQRCodeFromStringAndPutInImageView
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.logbookapp.utils.QR.QRFunctions
import nl.joozd.serializing.packSerializable
import nl.joozd.serializing.unpackSerialized


class SharingActivity : JoozdlogActivity() {
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
            hideButtons()
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
        lifecycleScope.launch {
            println("GETTING DATA")
            val data = withContext(DispatcherProvider.default()) { packSerializable(FlightRepository.instance.getAllFlights().map { it.toBasicFlight() }) }
            println("DATA PACKED (${data.size} bytes)")
            val key = "NO KEY".toByteArray()
            //TODO encrypt data with key
            val id: Long = try{
                Cloud().sendP2PData(data)
            } catch (e: CloudException){
                Log.w("SendP2P", "Error: $e")
                //TODO feedback reason of failure or something to user
                return@launch
            }
            println("ID: $id")
            val p2pData = P2PSessionMetaData(id, key)
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
            lifecycleScope.launch {
                handleResult(result.contents)
            }
        }
    }

    private suspend fun handleResult(result: String){
        println("HandleResult")
        val p2pData = P2PSessionMetaData.ofJsonString(result)
        println("HandleResult - ${p2pData.sessionID}")
        val receivedData = Cloud().receiveP2PData(p2pData.sessionID)

        if (replace == true) {
            FlightRepositoryWithDirectAccess.instance.apply {
                clear()
                save(unpackFlights(receivedData))
            }
        }
        else {
            toast("Not supported yet")
        }

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
                replace = true
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

    companion object{
        private const val ACTION_IDENTIFIER_XFER = "Xfer"
    }
}