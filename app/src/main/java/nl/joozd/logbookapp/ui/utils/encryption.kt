package nl.joozd.logbookapp.ui.utils

import java.util.*


fun base64Encode(ba: ByteArray): String = Base64.getEncoder().encodeToString(ba)

fun base64Decode(s: String): ByteArray = Base64.getDecoder().decode(s)