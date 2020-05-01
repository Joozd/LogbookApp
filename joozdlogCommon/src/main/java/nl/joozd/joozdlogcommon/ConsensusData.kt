package nl.joozd.joozdlogcommon

import nl.joozd.joozdlogcommon.serializing.JoozdlogSerializable
import nl.joozd.joozdlogcommon.serializing.unwrap
import nl.joozd.joozdlogcommon.serializing.unwrapByteArray
import nl.joozd.joozdlogcommon.serializing.wrap

/**
 * Add or remove an opinion to/from consensus
 * @param registration: Registration of aircraft this applies to
 * @param serializedType: AircraftType. Can be non-serialized thriugh secondary constructor
 * @param subtract: If true, this needs to be subtracted from consensus
 */

@Suppress("ArrayInDataClass")
data class ConsensusData(val registration: String,
                         val serializedType: ByteArray, // serialized AircraftType
                         val subtract: Boolean = false
                        ) : JoozdlogSerializable {
    constructor(registration: String, aircraftType: AircraftType, subtract: Boolean = false): this(registration, aircraftType.serialize(), subtract)
    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)

        serialized += wrap(component1())
        serialized += wrap(component2())
        serialized += wrap(component3())

        return serialized
    }
    val aircraftType: AircraftType
        get() = AircraftType.deserialize(serializedType)


    companion object: JoozdlogSerializable.Creator {

        /**
         * Unfortunately, I don't know how to do this with non-typed functions
         */
        override fun deserialize(source: ByteArray): ConsensusData {
            val wraps = ConsensusData.serializedToWraps(source)
            return ConsensusData(
                unwrap(wraps[0]),
                unwrapByteArray(wraps[1]), // needs to be forced bytearray to prevent ambiguity with secondary constructor
                unwrap(wraps[2])
            )
        }
    }
}
