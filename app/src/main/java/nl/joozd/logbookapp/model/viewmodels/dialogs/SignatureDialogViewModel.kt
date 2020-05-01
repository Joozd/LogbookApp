package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.Transformations
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class SignatureDialogViewModel: JoozdlogDialogViewModel() {
    val signature = Transformations.map(flightRepository.workingFlight) { it.signature }

    fun updateSignature(newSignature: String){
        workingFlight?.let{flight ->
            flightRepository.updateWorkingFlight(flight.copy(signature = newSignature))
        }
    }
}