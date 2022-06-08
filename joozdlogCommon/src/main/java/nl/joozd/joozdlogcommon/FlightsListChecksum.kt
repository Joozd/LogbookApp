package nl.joozd.joozdlogcommon

import nl.joozd.serializing.JoozdSerializable
import nl.joozd.serializing.unwrap
import nl.joozd.serializing.wrap

data class FlightsListChecksum(val flightIdHash: Long, val timestampsHash: Long): JoozdSerializable {
    constructor(flights: List<BasicFlight>): this(getIdChecksum(flights), getTimestampsChecksum(flights))

    override fun serialize(): ByteArray {
        var serialized = ByteArray(0)
        serialized += wrap(component1())
        serialized += wrap(component2())

        return serialized
    }

    companion object: JoozdSerializable.Deserializer<FlightsListChecksum> {
        override fun deserialize(source: ByteArray): FlightsListChecksum {
            val wraps = LoginData.serializedToWraps(source)
            return FlightsListChecksum(
                unwrap(wraps[0]),
                unwrap(wraps[1])
            )
        }

        fun checksum(longs: List<Long>): Long {
            var a = 0L
            var b = 0L
            longs.forEach {
                a = (a + it.abs()) % Int.MAX_VALUE
                b = (b + a.abs()) % Int.MAX_VALUE
            }
            return a.shl(31) * b % Int.MAX_VALUE
        }

        private fun Long.abs() = if (this > 0) this else this * -1


    private fun getIdChecksum(flights: List<BasicFlight>): Long =
        checksum(
            flights.map { it.flightID.toLong() }
        )

    private fun getTimestampsChecksum(flights: List<BasicFlight>): Long =
        checksum(
            flights.map { it.timeStamp }
        )
    }
}