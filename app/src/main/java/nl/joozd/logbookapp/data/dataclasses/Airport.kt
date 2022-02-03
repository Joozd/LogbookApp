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

package nl.joozd.logbookapp.data.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.joozd.joozdlogcommon.BasicAirport

@Entity
data class Airport(
    @PrimaryKey val id: Int = 0,
    val ident: String = "", // ICAO ident
    val type: String = "",
    val name: String = "",
    val latitude_deg: Double = 0.0,
    val longitude_deg: Double = 0.0,
    val elevation_ft: Int = 0,
//    val continent: String = "",
//    val iso_country: String = "",
//    val iso_region: String = "",
    val municipality: String = "",
//    val scheduled_service: String = "",
//    val gps_code: String = "",
    val iata_code: String = ""
//    val local_code: String = "",
//    val home_link: String = "",
//    val wikipedia_link: String = "",
//    val keywords: String = ""
){
    constructor(a: BasicAirport): this(a.id, a.ident, a.type, a.name, a.latitude_deg, a.longitude_deg, a.elevation_ft, a.municipality, a.iata_code)

    infix fun identMatches(query: String): Boolean{
        val q = query.uppercase()
        return q in ident.uppercase() || q in iata_code.uppercase()
    }

    infix fun matches(query: String): Boolean{
        val q = query.uppercase()
        return q in ident.uppercase()
                || q in iata_code.uppercase()
                || q in name.uppercase()
                || q in municipality.uppercase()
    }

    fun toBasicAirport() = BasicAirport(id, ident, type, name, latitude_deg, longitude_deg, elevation_ft, municipality, iata_code)

    companion object{
        fun placeholderWithIdentOnly(ident: String) = Airport(-1, ident)
    }
}
