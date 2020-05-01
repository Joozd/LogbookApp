package nl.joozd.logbookapp.utils

import nl.joozd.logbookapp.model.dataclasses.Flight

/**
 * Saves only the first time this is updated.
 * Useful for saving a backup from a fragment which might get recreated
 */
class InitialSetFlight {
    var alreadySet = false
    var flight: Flight? = null
        set(f) {
            if (f != null && !alreadySet){
                field = f
                alreadySet = true
            }
        }
}