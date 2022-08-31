package nl.joozd.logbookapp.sharing

import nl.joozd.logbookapp.ui.utils.base64Decode
import nl.joozd.logbookapp.ui.utils.base64Encode
import nl.joozd.logbookapp.utils.generateKey
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.random.Random

class P2PSessionMetaData(val sessionID: Long, val encryptionKey: ByteArray) {
    private constructor(sessionID: String, encryptionKeyString: String): this(sessionID.toLong(16), base64Decode(encryptionKeyString))
    val keyDataPairs: Array<Pair<String, String>> get() =
        listOf(
            ID_KEY to sessionID.toString(16),
            KEY_KEY to base64Encode(encryptionKey).trim()
        ).toTypedArray()


    companion object{
        fun ofJsonString(json: String): P2PSessionMetaData =
            with(JSONTokener(json).nextValue() as JSONObject){
                P2PSessionMetaData(
                    getString(ID_KEY),
                    getString(KEY_KEY)
                )
            }

        private const val ID_KEY = "ID"
        private const val KEY_KEY = "KEY"
    }
}