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

package nl.joozd.logbookapp.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KProperty

/**
 * cast a Flow item to a MutableStateFlow.
 * For items that you want to expose as an immutable StateFlow
 * but want to call as a variable internally.
 * Use:
 *  val dataFlow: Flow<SomeType> = MutableStateFlow<SomeType>(makeInitialValue())
 *  private var data: SomeType by CastFlowToMutableFlowShortcut(dataFlow)
 */
class CastFlowToMutableFlowShortcut<T>(private val flowToCast: Flow<T>){
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        (flowToCast as MutableStateFlow).value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        (flowToCast as MutableStateFlow).value = newValue
    }
}