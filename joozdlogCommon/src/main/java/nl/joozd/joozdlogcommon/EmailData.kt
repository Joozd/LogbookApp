package nl.joozd.joozdlogcommon

import nl.joozd.serializing.*

data class EmailData(val emailID: Long, val emailAddress: String, val attachment: ByteArray = ByteArray(0)): JoozdSerializable{
    override fun serialize(): ByteArray =
        wrap(component1()) +
        wrap(component2()) +
        wrap(component3())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmailData

        if (emailID != other.emailID) return false
        if (emailAddress != other.emailAddress) return false
        if (!attachment.contentEquals(other.attachment)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = emailID.hashCode()
        result = 31 * result + emailAddress.hashCode()
        result = 31 * result + attachment.contentHashCode()
        return result
    }

    companion object: JoozdSerializable.Deserializer<EmailData> {
        const val EMAIL_ID_NOT_SET = -1L

        override fun deserialize(source: ByteArray): EmailData {
            val wraps = EmailData.serializedToWraps(source)
            return EmailData(
                unwrap(wraps[0]),
                unwrap(wraps[1]),
                unwrap(wraps[2])
            )
        }
    }
}
