/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

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
        OPEN_SEARCH_FIELD,
        CLOSE_SEARCH_FIELD,
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

    enum class FlightEditorOpenOrClosed: Event{
        OPENED,
        CLOSED
    }

    enum class GenericEvents: Event{
        EVENT_1,
        EVENT_2,
        EVENT_3
    }


}