package nl.joozd.joozdlogcommon

import org.json.JSONObject
import org.json.JSONTokener

data class DataFilesMetaData(
    var aircraftTypesVersion: Int,
    var aircraftTypesLocation: String,
    var aircraftForcedTypesVersion: Int,
    var aircraftForcedTypesLocation: String,
    var airportsVersion: Int,
    var airportsLocation: String
) {
    override fun toString() = toJSONString()

    private fun toJSONString(): String =
        "{\n${buildJsonBlock().prependIndent("    ")}\n}"

    private fun buildJsonBlock(): String =
        "\"$AIRCRAFT_TYPES_VERSION\": $aircraftTypesVersion,\n" +
        "\"$AIRCRAFT_TYPES_LOCATION\": \"$aircraftTypesLocation\",\n" +
                "\n" +
        "\"$AIRCRAFT_FORCED_TYPES_VERSION\": $aircraftForcedTypesVersion,\n" +
        "\"$AIRCRAFT_FORCED_TYPES_LOCATION\": \"$aircraftForcedTypesLocation\",\n" +
                "\n" +
        "\"$AIRPORTS_VERSION\": $airportsVersion,\n" +
        "\"$AIRPORTS_LOCATION\": \"$airportsLocation\""


    companion object{
        fun fromJSON(json: String): DataFilesMetaData =
            with(JSONTokener(json).nextValue() as JSONObject){
                DataFilesMetaData(
                    getInt(AIRCRAFT_TYPES_VERSION),
                    getString(AIRCRAFT_TYPES_LOCATION),
                    getInt(AIRCRAFT_FORCED_TYPES_VERSION),
                    getString(AIRCRAFT_FORCED_TYPES_LOCATION),
                    getInt(AIRPORTS_VERSION),
                    getString(AIRPORTS_LOCATION)
                )
            }

        private const val AIRCRAFT_TYPES_VERSION = "aircraftTypesVersion"
        private const val AIRCRAFT_TYPES_LOCATION = "aircraftTypesLocation"
        private const val AIRCRAFT_FORCED_TYPES_VERSION = "aircraftForcedTypesVersion"
        private const val AIRCRAFT_FORCED_TYPES_LOCATION = "aircraftForcedTypesLocation"
        private const val AIRPORTS_VERSION = "airportsVersion"
        private const val AIRPORTS_LOCATION = "airportsLocation"

        fun blank() = DataFilesMetaData(0, "", 0, "",0, "")
    }
}