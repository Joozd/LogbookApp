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

package nl.joozd.logbookapp.ui.dialogs.namesDialog

import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.model.viewmodels.dialogs.namesDialog.Name1DialogViewModel

class Name1Dialog: NamesDialog() {
    /**
     * Set to true if working on PIC, or false if working on other names (Flight.name2)
     */
    override val workingOnName1 = true

    /**
     * ViewModel to use for this dialog
     */
    override val viewModel: Name1DialogViewModel by viewModels()
}