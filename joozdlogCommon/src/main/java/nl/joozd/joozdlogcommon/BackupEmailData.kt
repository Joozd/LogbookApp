package nl.joozd.joozdlogcommon

import nl.joozd.serializing.*

data class BackupEmailData(val username: String, val emailAddress: String, val flightsCsvString: String): JoozdSerializable{
    override fun serialize(): ByteArray =
        wrap(component1()) +
        wrap(component2()) +
        wrap(component3())

    companion object: JoozdSerializable.Deserializer<BackupEmailData> {

        override fun deserialize(source: ByteArray): BackupEmailData {
            val wraps = BackupEmailData.serializedToWraps(source)
            return BackupEmailData(
                unwrap(wraps[0]),
                unwrap(wraps[1]),
                unwrap(wraps[2])
            )
        }
    }

}
