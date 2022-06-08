package nl.joozd.joozdlogcommon

import nl.joozd.serializing.JoozdSerializable
import nl.joozd.serializing.unwrap
import nl.joozd.serializing.wrap

// For use in syncing
data class IDWithTimeStamp(val ID: Int, val timeStamp: Long): JoozdSerializable {
    constructor(flight: BasicFlight): this(flight.flightID, flight.timeStamp)

    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)
        serialized += wrap(component1())
        serialized += wrap(component2())
        return serialized
    }

    companion object: JoozdSerializable.Deserializer<IDWithTimeStamp> {
        override fun deserialize(source: ByteArray): IDWithTimeStamp {
            val wraps = LoginData.serializedToWraps(source)
            return IDWithTimeStamp(
                unwrap(wraps[0]),
                unwrap(wraps[1])
            )
        }
    }
}