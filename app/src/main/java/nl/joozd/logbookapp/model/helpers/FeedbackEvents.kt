package nl.joozd.logbookapp.model.helpers

object FeedbackEvents {
    interface Event
    enum class MainActivityEvents: Event{
        NOT_IMPLEMENTED,
        SHOW_FLIGHT,
        FLIGHT_SAVED,
        FLIGHT_NOT_FOUND,
        TRYING_TO_DELETE_COMPLETED_FLIGHT,
        DELETED_FLIGHT
    }

    enum class EditFlightFragmentEvents: Event{
        NOT_IMPLEMENTED,
        INVALID_REG_TYPE_STRING,
        AIRPORT_NOT_FOUND_FOR_LANDINGS
    }

    enum class AirportPickerEvents: Event{
        NOT_IMPLEMENTED,
        ORIG_OR_DEST_NOT_SELECTED
    }

    enum class NamesDialogEvents: Event{
        NOT_IMPLEMENTED,
        NAME1_OR_NAME2_NOT_SELECTED
    }

    enum class TimePickerEvents: Event{
        NOT_IMPLEMENTED,
        FLIGHT_IS_NULL,
        INVALID_NIGHT_TIME,
        NIGHT_TIME_GREATER_THAN_DURATION,
        INVALID_IFR_TIME,
        IFR_TIME_GREATER_THAN_DURATION
    }


}