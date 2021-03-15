/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2021 Joost Welle
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

package nl.joozd.logbookapp.ui.utils

import androidx.appcompat.app.AppCompatDelegate
import nl.joozd.logbookapp.data.sharedPrefs.Preferences

object DarkModeHub {
    fun setDarkMode(darkMode: Int){
        Preferences.darkMode = if (darkMode in SUPPORTED_MODES) darkMode else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        AppCompatDelegate.setDefaultNightMode(Preferences.darkMode)
    }

    fun getDarkMode() = Preferences.darkMode

    fun setDarkMode(){
        AppCompatDelegate.setDefaultNightMode(Preferences.darkMode)
    }

    private val SUPPORTED_MODES = listOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_NO)
}