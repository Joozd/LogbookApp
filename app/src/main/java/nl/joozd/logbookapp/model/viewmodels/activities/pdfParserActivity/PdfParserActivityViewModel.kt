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

package nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel

class PdfParserActivityViewModel(
    airportRepository: AirportRepository = AirportRepository.instance,
    aircraftRepository: AircraftRepository = AircraftRepository.instance
): JoozdlogActivityViewModel() {
    val repositoriesReadyFlow = combine(aircraftRepository.dataLoaded, airportRepository.dataLoaded){ aircraftReady, airportsReady ->
        aircraftReady && airportsReady
    }
    private val handler = SingleUseImportIntentHandler()
    val handlerStatus get() = handler.statusFlow

    // This gets called from
    private var alreadyHandlingIntent = false
    fun handleUri(uri: Uri?, contentResolver: ContentResolver){
        if (!alreadyHandlingIntent) {
            alreadyHandlingIntent = true
            handler.handleUri(uri, contentResolver)
        }
    }


}