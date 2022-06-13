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

package nl.joozd.logbookapp.model.viewmodels.fragments

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.calendar.CalendarControl
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.checkIfValidCoordinates
import nl.joozd.logbookapp.extensions.toLocalDate
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.enumclasses.DualInstructorFlag
import nl.joozd.logbookapp.model.enumclasses.PicPicusFlag
import nl.joozd.logbookapp.model.helpers.makeNamesList
import nl.joozd.logbookapp.model.viewmodels.JoozdlogViewModel
import nl.joozd.logbookapp.model.workingFlight.FlightEditor
import nl.joozd.logbookapp.model.workingFlight.FlightEditorDataParser
import java.time.LocalDate


class NewEditFlightFragmentViewModel: JoozdlogViewModel() {
    private val flightEditor = FlightEditor.instance!! // this Fragment should not have launched if flightEditor is null
    private val uneditedFlight = flightEditor.snapshot()
    private val aircraftRepository = AircraftRepository.instance
    private val flightRepository = FlightRepository.instance

    //use this for any data that needs parsing
    private val flightEditorDataParser = FlightEditorDataParser(flightEditor)

    private val flightFlow get() = flightEditor.flightFlow

    val isNewFlight = flightEditor.isNewFlight

    val localDate: LocalDate get() = flightEditor.timeOut.toLocalDate()

    val isSim: Boolean get() = flightEditor.isSim

    val dateFlow = flightFlow.map { it.date() }
    val flightNumberFlow= flightFlow.map { it.flightNumber }
    val timeOutFlow = flightFlow.map { it.timeOut }
    val timeInFlow = flightFlow.map { it.timeIn }

    private val origFlow = flightFlow.map { it.orig }
    val origTextFlow = combine(origFlow, Prefs.useIataAirports.flow){ ap, useIata -> if(useIata && ap.iata_code.isNotBlank() ) ap.iata_code else ap.ident }
    val origValidFlow = origFlow.map { it.checkIfValidCoordinates() }

    private val destFlow = flightFlow.map { it.dest }
    val destTextFlow = combine(destFlow, Prefs.useIataAirports.flow){ ap, useIata -> if(useIata && ap.iata_code.isNotBlank() ) ap.iata_code else ap.ident }
    val destValidFlow = destFlow.map { it.checkIfValidCoordinates() }

    val aircraftFlow = flightFlow.map { it.aircraft }
    val takeoffLandingsFlow = flightFlow.map{ it.takeoffLandings }
    val nameFlow = flightFlow.map { it.name }
    val name2Flow = flightFlow.map { it.name2 }
    val remarksFlow = flightFlow.map { it.remarks }

    val isSimFlow = flightFlow.map { it.isSim }
    val isSignedFlow = flightFlow.map { it.signature.isNotBlank() }
    val dualInstructorFlow = flightFlow.map { makeDualInstructorFlag(it) }
    val isMultiPilotFlow = flightFlow.map { it.multiPilotTime > 0 }
    val isIfrFlow = flightFlow.map { it.ifrTime >= 0 } // IFR time of -1 means VFR
    val picPicusFlow = flightFlow.map { makePicPicusFlag(it) }
    val isPfFlow = flightFlow.map { it.isPF }
    val isAutoValuesFlow = flightFlow.map { it.autoFill }

    val simTimeFlow = flightFlow.map { it.simTime }

    val sortedRegistrationsFlow = aircraftRepository.aircraftMapFlow().map { makeSortedRegistrationsList(it) }


    val namesFlow = flightRepository.allFlightsFlow().map{ it.makeNamesList() }

    private fun makeSortedRegistrationsList(regMap: Map<String, Aircraft>) =
        regMap.keys.toList()

    private fun makeDualInstructorFlag(it: ModelFlight) = when {
        it.isDual -> DualInstructorFlag.DUAL
        it.isInstructor -> DualInstructorFlag.INSTRUCTOR
        else -> DualInstructorFlag.NONE
    }

    private fun makePicPicusFlag(it: ModelFlight) = when {
        it.isPIC -> PicPicusFlag.PIC
        it.isPICUS -> PicPicusFlag.PICUS
        else -> PicPicusFlag.NONE
    }




    fun toggleSim(){
         flightEditor.isSim = !flightEditor.isSim
    }

    fun toggleDualInstructorNone(){
        flightEditor.toggleDualInstructorNeither()
    }

    fun togglePicusPicNone(){
        flightEditor.togglePicusPicNeither()
    }

    fun toggleMultiPilot(){
        flightEditor.multiPilotTime =
        if (flightEditor.multiPilotTime == 0)
             flightEditor.totalFlightTime
        else 0
    }

    fun toggleIFR(){
        flightEditor.ifrTime =
            if(flightEditor.ifrTime >= 0) Flight.FLIGHT_IS_VFR
        else flightEditor.totalFlightTime
    }

    fun togglePF(){
        flightEditor.isPF = !flightEditor.isPF
    }

    fun toggleAutoValues(){
        flightEditor.autoFill = !flightEditor.autoFill
    }

    fun setFlightNumber(flightNumber: String?){
        flightNumber?.let{
            flightEditor.flightNumber = it
        }
    }

    fun setOrig(orig: String?){
        orig?.let {
            flightEditorDataParser.setOrig(it)
        } ?: Log.w(this::class.simpleName, "setOrig() received null param")
    }

    fun setDest(dest: String?){
        dest?.let {
            flightEditorDataParser.setDest(it)
        } ?: Log.w(this::class.simpleName, "setDest() received null param")
    }

    fun setTimeOut(timeOut: String?){
        timeOut?.let {
            flightEditorDataParser.setTimeOut(it)
        }
    }

    fun setTimeIn(timeIn: String?){
        timeIn?.let {
            flightEditorDataParser.setTimeIn(it)
        }
    }

    fun setRegAndType(regAndType: String?){
        regAndType?.let{ flightEditorDataParser.setAircraft(it) }
    }

    fun setTakeoffLandings(toLandingData: String?){
        toLandingData?.let{
            flightEditorDataParser.setTakeoffLandings(it)
        }
    }


    fun setName(name: String?){
        name?.let{
            flightEditor.name = it
        }
    }

    fun setName2(names: String?){
        names?.let{
            flightEditor.name2 = splitAndTrimNames(it)
        }
    }

    fun setRemarks(remarks: String?){
        remarks?.let{
            flightEditor.remarks = it
        }
    }

    fun setSimTime(simTime: String?){
        flightEditorDataParser.setSimTimeFromString(simTime)
    }

    fun setSimAircraft(aircraft: String?){
        aircraft?.let {
            flightEditorDataParser.setSimAircraftType(it)
        }
    }


    private fun splitAndTrimNames(namesString: String) =
        namesString.split(";").map { it.trim() }



    fun saveAndClose(){
        viewModelScope.launch {
            val f = flightEditorDataParser.saveAndClose()

            with(CalendarControl){
                if (!checkIfCalendarSyncOK(flightEditor.snapshot(), uneditedFlight))
                    handleManualFlightSave(f)
            }
        }
    }

    fun closeWithoutSaving(){
        flightEditorDataParser.close()
    }
}