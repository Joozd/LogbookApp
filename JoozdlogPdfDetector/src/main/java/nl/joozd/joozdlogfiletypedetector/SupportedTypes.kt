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

package nl.joozd.joozdlogfiletypedetector

/**
 * Supported types
 */
object SupportedTypes {
    val KLC_ROSTER = PlannedFlights()
    val KLC_CHECKIN_SHEET = PlannedFlights()
    val KLM_ICA_ROSTER = PlannedFlights()

    val KLC_MONTHLY = CompletedFlights()
    val KLM_ICA_MONTHLY = CompletedFlights()

    val MCC_PILOT_LOG_LOGBOOK = CompleteLogbook()
    val LOGTEN_PRO_LOGBOOK = CompleteLogbook()
    val JOOZDLOG_CSV_BACKUP = CompleteLogbook()

    val UNSUPPORTED_PDF = Unsupported()
    val UNSUPPORTED_CSV = Unsupported()

    interface SupportedType
    class PlannedFlights: SupportedType
    class CompletedFlights: SupportedType
    class CompleteLogbook: SupportedType
    class Unsupported: SupportedType
}