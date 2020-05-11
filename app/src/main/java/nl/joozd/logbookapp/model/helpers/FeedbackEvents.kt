package nl.joozd.logbookapp.model.helpers

object FeedbackEvents {
    interface Event

    enum class PdfParserActivityEvents: Event{
        ROSTER_SUCCESSFULLY_ADDED,
        FILE_NOT_FOUND,
        NOT_A_KNOWN_ROSTER,
        ERROR
    }

    enum class MainActivityEvents: Event{
        NOT_IMPLEMENTED,
        SHOW_FLIGHT,
        FLIGHT_SAVED,
        FLIGHT_NOT_FOUND,
        TRYING_TO_DELETE_COMPLETED_FLIGHT,
        DELETED_FLIGHT,
        DONE,
        ERROR
    }

    enum class EditFlightFragmentEvents: Event{
        NOT_IMPLEMENTED,
        INVALID_REG_TYPE_STRING,
        AIRPORT_NOT_FOUND_FOR_LANDINGS,
        INVALID_TIME_STRING,                // flight not updated due bad time string
        INVALID_SIM_TIME_STRING,            // Flight updated with 0 sim time due bad string format
        REGISTRATION_NOT_FOUND,             // registration has no match in local DB or consensus
        REGISTRATION_HAS_MULTIPLE_CONSENSUS,// registration has multiple hits in consensus
                                            // extraData = List of found registrations in consensus
        AIRPORT_NOT_FOUND,
        ERROR

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