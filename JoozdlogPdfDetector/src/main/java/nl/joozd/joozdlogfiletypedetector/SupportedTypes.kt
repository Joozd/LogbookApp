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

package nl.joozd.joozdlogfiletypedetector

enum class SupportedTypes {
    KLC_ROSTER,
    KLC_CHECKIN_SHEET,
    KLC_MONTHLY,
    KLM_ICA_ROSTER,
    KLM_ICA_MONTHLY,
    MCC_PILOT_LOG_LOGBOOK,
    JOOZDLOG_CSV_BACKUP,
    UNSUPPORTED_PDF,
    UNSUPPORTED_CSV
}