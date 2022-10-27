package nl.joozd.logbookapp.data.calendar

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.importing.matchesFlightnumberTimeOrigAndDestWith
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.core.MessageCenter
import nl.joozd.logbookapp.core.messages.UserMessage
import java.time.Instant

object CalendarControl {
    /**
     * true if changing one flight to another will make changes that calendar sync will overwrite
     */
    fun checkIfCalendarSyncOK(oldFlight: ModelFlight?, newFlight: ModelFlight) =
        oldFlight?.toFlight()?.matchesFlightnumberTimeOrigAndDestWith(newFlight.toFlight()) ?: false

    /**
     * If a flight has been manually saved, this checks if CalendarSync causes a conflict.
     * If so, it postpones calendarSync for a flight that already departed, or puts a UserMessage in [MessageCenter]
     */
    suspend fun handleManualFlightSave(flight: Flight){
        if (flight.causesConflictWithCalendar())
            handleCalendarConflict(flight)
    }

    suspend fun handleManualDelete(flight: Flight) {
        if (flight.causesConflictWithCalendar())
            makeCalendarDeleteConflictMessage(flight)
    }

    private suspend fun Flight.causesConflictWithCalendar(): Boolean =
        isPlanned
                && Prefs.useCalendarSync()
                && timeIn > Prefs.calendarDisabledUntil()
                && timeIn > Instant.now().epochSecond

    private fun makeCalendarDeleteConflictMessage(flight: Flight) =
        makeCalendarConflictMessage(flight.timeIn, R.string.delete_calendar_flight)

    // This should only receive a conflicting flight.
    private fun handleCalendarConflict(flight: Flight) = with (flight) {
        if (timeOut <= Instant.now().epochSecond) // If this flight already departed
            Prefs.calendarDisabledUntil(timeIn + 1) // disable calendarSync until after this flight
        else
            makeCalendarConflictMessage(flight.timeIn, R.string.calendar_sync_edited_flight)
    }

    private fun makeCalendarConflictMessage(time: Long, msg: Int) {
        MessageCenter.pushMessage(
            UserMessage.Builder().apply {
                titleResource = R.string.calendar_sync_conflict
                descriptionResource = msg
                setPositiveButton(android.R.string.ok) {
                    Prefs.calendarDisabledUntil(time + 1)
                }
                setNegativeButton(R.string.ignore) { /* intentionally left blank */ }
            }.build()
        )
    }
}