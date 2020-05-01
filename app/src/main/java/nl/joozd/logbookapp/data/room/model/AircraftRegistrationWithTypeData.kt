package nl.joozd.logbookapp.data.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * This class matches aircraft registrations to types
 * This is what is used FIRST if looking to match an aircraft to a type
 * @param registration: Primary key, aircraft registration (can be any string, but usually something like "PH-EZE"
 * @param type: AircraftTypeData.name<String> to match with AircraftType
 * @param knownToServer: If false, let server know about this for consensus
 * @param previousType: Previous type known by server
 *      if ![knownToServer] this is to be sent to server for consensus (subtract counter)
 *      if [knownToServer], if [type] is changed, change this to previous type
 */

@Entity
data class AircraftRegistrationWithTypeData(
    @PrimaryKey val registration: String,
    var type: String = UNKNOWN,
    var knownToServer: Boolean = false,
    var previousType: String = UNKNOWN
){
    companion object {
        const val UNKNOWN = "UNKNOWN_TYPE"
    }
}