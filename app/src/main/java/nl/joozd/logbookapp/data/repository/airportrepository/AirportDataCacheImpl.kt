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

package nl.joozd.logbookapp.data.repository.airportrepository

import nl.joozd.logbookapp.data.dataclasses.Airport

/**
 * Keeps a list of Airports and provides access to it.
 */
class AirportDataCacheImpl(private var airportsList: List<Airport>): AirportDataCache {
    private val icaoToIatamap: Map<String, String> by lazy { buildIcaoToIataMap() }
    private val iataToIcaoMap: Map<String, String> by lazy { buildIataToIcaoMap() }


    override fun getAirports(): List<Airport> = airportsList

    override fun getAirportByIcaoIdentOrNull(icaoIdent: String): Airport? =
        airportsList.firstOrNull {
            it.ident.equals(icaoIdent, ignoreCase = true)
        }

    override fun icaoToIata(icaoIdent: String): String? =
        icaoToIatamap[icaoIdent]

    override fun iataToIcao(iataIdent: String): String? =
        iataToIcaoMap[iataIdent]

    private fun buildIcaoToIataMap() =
        airportsList.map { it.ident to it.iata_code }.toMap()

    private fun buildIataToIcaoMap() =
        airportsList.map { it.iata_code to it.ident }.toMap()


}