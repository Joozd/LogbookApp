package nl.joozd.logbookapp.data.dataclasses

import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.joozd.joozdlogcommon.BasicAirport

@Entity
data class Airport(
    @PrimaryKey val id: Int = 0,
    val ident: String = "",
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

    fun toBasicAirport() = BasicAirport(id, ident, type, name, latitude_deg, longitude_deg, elevation_ft, municipality, iata_code)
}
