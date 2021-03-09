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

package nl.joozd.logbookapp.ui.dialogs.airportPicker

import androidx.fragment.app.viewModels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker.OrigPickerViewmodel

/**
 * Airport picker that works on Origin
 */
class OrigPicker: AirportPicker() {
    override val workingOnOrig = true
    override val viewModel: OrigPickerViewmodel by viewModels()
}