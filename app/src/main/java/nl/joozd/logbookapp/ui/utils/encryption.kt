package nl.joozd.logbookapp.ui.utils

import android.util.Base64
import java.security.MessageDigest

fun passwordToKeyString(password: String): String =
    base64Encode(md5Hash(password))

fun md5Hash(input: String): ByteArray =
    with (MessageDigest.getInstance("MD5")){
        update(input.toByteArray())
        digest()
    }

fun base64Encode(ba: ByteArray): String = Base64.encodeToString(ba, Base64.DEFAULT)

fun base64Decode(s: String): ByteArray = Base64.decode(s, Base64.DEFAULT)