package nl.joozd.logbookapp.utils.QR

object QRFunctions {
    const val ACTION_KEY = "A"

    fun makeJsonStringWithAction(action: String, vararg keyValuePairs: Pair<String, String>) =
        makeJsonString(*(arrayOf(ACTION_KEY to action) + keyValuePairs))

    fun makeJsonString(vararg keyValuePairs: Pair<String, String>): String =
        "{\n${makeJsonData(*keyValuePairs).prependIndent("  ")}\n}"

    private fun makeJsonData(vararg keyValuePairs: Pair<String, String>): String =
        keyValuePairs.joinToString(",\n"){ "\"${it.first}\": \"${it.second}\""}
}