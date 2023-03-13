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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.repository.BalanceForwardRepositoryImpl
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut


/**
 * Viewmodel for AddBalanceForwardDialog Fragment
 * As this is only for inputting data, and not for retrieving, only limited feedback is required
 */
//TODO switch from LiveData to Flow
class AddBalanceForwardDialogViewmodel: JoozdlogDialogViewModel() {
    private val balanceForwardFlow: Flow<BalanceForward> = MutableStateFlow(BalanceForward.EMPTY)
    var balanceForward: BalanceForward by CastFlowToMutableFlowShortcut(balanceForwardFlow)


    val logbookNameFlow = balanceForwardFlow.map { it.logbookName }

    val multiPilotFlow = balanceForwardFlow.map { it.multiPilotTime.minutesToHoursAndMinutesString()}

    val totalTimeOfFlightFlow = balanceForwardFlow.map { it.aircraftTime.minutesToHoursAndMinutesString()}

    val landingDayFlow = balanceForwardFlow.map { it.landingDay.toString()}

    val landingNightFlow = balanceForwardFlow.map { it.landingNight.toString()}

    val nightTimeFlow = balanceForwardFlow.map { it.nightTime.minutesToHoursAndMinutesString()}

    val ifrTimeFlow = balanceForwardFlow.map { it.ifrTime.minutesToHoursAndMinutesString()}

    val picTimeFlow = balanceForwardFlow.map { it.picTime.minutesToHoursAndMinutesString()}

    val copilotTimeFlow = balanceForwardFlow.map { it.copilotTime.minutesToHoursAndMinutesString()}

    val dualTimeFlow = balanceForwardFlow.map { it.dualTime.minutesToHoursAndMinutesString()}

    val instructorTimeFlow = balanceForwardFlow.map { it.instructortime.minutesToHoursAndMinutesString()}

    val simTimeFlow = balanceForwardFlow.map { it.simTime.minutesToHoursAndMinutesString()}

    /**********************************************************************************************
     * Private parts
     **********************************************************************************************/

    var logbookName: String
        get() = balanceForward.logbookName
        set(it) {
            balanceForward = balanceForward.copy(logbookName = it)
        }

    var multipilotTime: Int
        get() = balanceForward.multiPilotTime
        set(it){
            balanceForward = balanceForward.copy(multiPilotTime = it)
        }
    var aircraftTime: Int
        get() = balanceForward.aircraftTime
        set(it){
            balanceForward = balanceForward.copy(aircraftTime = it)
        }

    var landingDay: Int
        get() = balanceForward.landingDay
        set(it){
            balanceForward = balanceForward.copy(landingDay = it)
        }

    var landingNight: Int
        get() = balanceForward.landingNight
        set(it){
            balanceForward = balanceForward.copy(landingNight = it)
        }

    var nightTime: Int
        get() = balanceForward.nightTime
        set(it){
            balanceForward = balanceForward.copy(nightTime = it)
        }

    var ifrTime: Int
        get() = balanceForward.ifrTime
        set(it){
            balanceForward = balanceForward.copy(ifrTime = it)
        }

    var picTime: Int
        get() = balanceForward.picTime
        set(it){
            balanceForward = balanceForward.copy(picTime = it)
        }

    var copilotTime: Int
        get() = balanceForward.copilotTime
        set(it){
            balanceForward = balanceForward.copy(copilotTime = it)
        }

    var dualTime: Int
        get() = balanceForward.dualTime
        set(it){
            balanceForward = balanceForward.copy(dualTime = it)
        }

    var instructortime: Int
        get() = balanceForward.instructortime
        set(it){
            balanceForward = balanceForward.copy(instructortime = it)
        }

    var simTime: Int
        get() = balanceForward.simTime
        set(it){
            balanceForward = balanceForward.copy(simTime = it)
        }

    suspend fun saveBalanceForward() = withContext(Dispatchers.IO + NonCancellable){
        BalanceForwardRepositoryImpl.instance.save(balanceForward)
    }
}
