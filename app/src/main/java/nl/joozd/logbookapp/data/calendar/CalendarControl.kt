package nl.joozd.logbookapp.data.calendar

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.importing.matchesFlightnumberTimeOrigAndDestWith
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.ui.messageCenter.MessageCenter
import nl.joozd.logbookapp.utils.UserMessage
import java.time.Instant

object CalendarControl {
    private fun setMessage(m: UserMessage) {
        MessageCenter.pushMessage(m)
    }


    /**
     * true if changing one flight to another will make changes that calendar sync will overwrite
     */
    fun checkIfCalendarSyncOK(oldFlight: ModelFlight?, newFlight: ModelFlight) =
        oldFlight?.toFlight()?.matchesFlightnumberTimeOrigAndDestWith(newFlight.toFlight()) ?: false

    /**
     * If a flight has been manually saved, this checks if CalendarSync causes a conflict.
     * If so, it puts a UserMessage in [MessageCenter]
     */
    fun handleManualFlightSave(flight: Flight){
        if (flight.causesConflictWithCalendar())
            setMessage(makeCalendarConflictMessage(flight, CalendarConflict.EDITING_FLIGHT_IN_PERIOD))
    }

    fun handleManualDelete(flight: Flight) {
        if (flight.causesConflictWithCalendar())
            setMessage(makeCalendarConflictMessage(flight, CalendarConflict.DELETING_FLIGHT_IN_PERIOD))
    }

    private fun Flight.causesConflictWithCalendar(): Boolean =
        isPlanned
                && Prefs.useCalendarSync
                && timeIn > Prefs.calendarDisabledUntil
                && timeIn > Instant.now().epochSecond


    private fun makeCalendarConflictMessage(flight: Flight, reason: CalendarConflict): UserMessage{
        val time = flight.timeIn
        return when (reason){
            CalendarConflict.EDITING_FLIGHT_IN_PERIOD ->
                makeCalendarConflictMessage(time, R.string.calendar_sync_edited_flight)
            CalendarConflict.DELETING_FLIGHT_IN_PERIOD ->
                makeCalendarConflictMessage(time, R.string.delete_calendar_flight)
        }
    }


    private fun makeCalendarConflictMessage(time: Long, msg: Int): UserMessage =
        UserMessage.Builder().apply{
            titleResource = R.string.calendar_sync_conflict
            descriptionResource = msg
            setPositiveButton(android.R.string.ok){
                Prefs.calendarDisabledUntil = time + 1
            }
            setNegativeButton(R.string.ignore){ /* intentionally left blank */ }
        }.build()

    private enum class CalendarConflict{
        EDITING_FLIGHT_IN_PERIOD,
        DELETING_FLIGHT_IN_PERIOD
    }


}