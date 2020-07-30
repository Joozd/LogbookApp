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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import com.caverock.androidsvg.SVG
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class SignatureDialogViewModel: JoozdlogDialogViewModel() {
    private var _signature: String = workingFlight?.signature ?: ""
    val signature: String
        get() = _signature

    val signatureSvg: SVG
        get() = SVG.getFromString(signature)


    fun updateSignature(newSignature: String){
        _signature = newSignature
    }

    fun signatureCleared(){
        _signature = ""
    }

    fun saveSignature(){
        workingFlight?.let{
            workingFlight = it.copy(signature = signature)
        }
    }
}