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

package nl.joozd.logbookapp.model.viewmodels

import androidx.lifecycle.*


/**
 * JoozdlogViewModel will hold data pertaining to UI components
 * ie. to tell a dialog which data it is working on
 */

class MainViewModel: ViewModel() {

    //shorter term undoFlight to be able to cancel subdialogs such as TimePicker or NamePicker

    //if true, NamePicker is working on name1, if false it is working on name2. If null, it is not set.
    var namePickerWorkingOnName1: Boolean? = null

    //if true, AirportPicker works on [orig], if false on [dest]
    var workingOnOrig: Boolean? = null













    // Get this from repository
    // val liveFlights: LiveData<List<Flight>> = repository.liveFlights


}