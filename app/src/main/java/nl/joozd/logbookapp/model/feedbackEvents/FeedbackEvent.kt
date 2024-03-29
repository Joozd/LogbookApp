/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

package nl.joozd.logbookapp.model.feedbackEvents

import android.os.Bundle

/**
 * Gives feedback from viewModel to UI class
 * ie. invalid data was received and nothing was saved
 * will work only once on "getEvent()", will return null every next time this is checked
 * inspiration from "https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150"
 */
class FeedbackEvent(val type: FeedbackEvents.Event) {
    var consumed = false
    fun getEvent(): FeedbackEvents.Event? {
        if (consumed)
            return null
        else {
            consumed = true
            return type
        }
    }
    val extraData = Bundle()


    /**
     * Functions to put an anonymous value into FeedbackEvent
     */
    fun putInt(value: Int): FeedbackEvent{
        extraData.putInt(INTEGER, value)
        return this
    }
    fun getInt(): Int = extraData.getInt(INTEGER)

    fun putString(value: String?): FeedbackEvent{
        extraData.putString(STRING, value)
        return this
    }
    fun getString(): String? = extraData.getString(STRING)

    fun putLong(value: Long): FeedbackEvent{
        extraData.putLong(LONG, value)
        return this
    }
    fun getLong(): Long = extraData.getLong(LONG)

    fun putBoolean(value: Boolean): FeedbackEvent{
        extraData.putBoolean(BOOLEAN, value)
        return this
    }
    fun getBoolean(): Boolean = extraData.getBoolean(BOOLEAN)

    companion object{
        const val INTEGER = "INTEGER_EXTRA_DATA_FOR_FEEDBACK"
        const val LONG = "LONG_EXTRA_DATA_FOR_FEEDBACK"
        const val STRING = "STRING_EXTRA_DATA_FOR_FEEDBACK"
        const val BOOLEAN = "BOOLEAN_EXTRA_DATA_FOR_FEEDBACK"
    }
}