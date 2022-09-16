package nl.joozd.joozdlogcommon.comms

enum class JoozdlogCommsResponses(val keyword: String) {
    //responses
    OK("OK"),
    CONNECTION_ERROR("CONNECTION_ERROR"),
    SERVER_ERROR("SERVER_ERROR"),
    BAD_DATA_RECEIVED("BAD_DATA_RECEIVED"),
    ID_NOT_FOUND("P2P_SESSION_NOT_FOUND"),
    EMAIL_NOT_KNOWN_OR_VERIFIED("EMAIL_NOT_KNOWN_OR_VERIFIED"),
    NOT_A_VALID_EMAIL_ADDRESS("NOT_A_VALID_EMAIL_ADDRESS"),

    UNKNOWN_KEYWORD("UNKNOWN_KEYWORD");

    companion object {
        private val keyWords by lazy { values().associateBy { it.keyword } }
        fun from(s: String): JoozdlogCommsResponses = keyWords[s] ?: UNKNOWN_KEYWORD
        fun from(b: ByteArray) = from(b.toString(Charsets.UTF_8))
    }
}