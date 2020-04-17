package nl.joozd.logbookapp.data.viewmodel

import androidx.lifecycle.ViewModel
import nl.joozd.logbookapp.data.dataclasses.Flight


/**
 * Holds data to that has to stay alive as long as a dialog does. *
 */
class DialogViewmodel: ViewModel() {
    var unchangedFlight: Flight? = null
}