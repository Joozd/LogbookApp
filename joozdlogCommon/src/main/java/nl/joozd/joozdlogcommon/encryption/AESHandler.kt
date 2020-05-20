/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.joozdlogcommon.encryption

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


// usage: create an instance of AESHandler with a key, that will be used to handle de/encryption of RSA encrypted data
class AESHandler (keyBytes: ByteArray) {
    val key: SecretKey = SecretKeySpec(keyBytes, "AES")

    fun encrypt (data: ByteArray): AESMessage {
        var nonce = ByteArray(16)
        val r = SecureRandom()

        r.nextBytes(nonce)
        val myParams = GCMParameterSpec(128, nonce)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, myParams)
        val encryptedDataPlusTag = cipher.doFinal(data)
        val tag = encryptedDataPlusTag.takeLast(16).toByteArray()
        val encryptedData = encryptedDataPlusTag.dropLast(16).toByteArray()
        return AESMessage(nonce+tag+encryptedData)
    }

    fun decrypt (message: AESMessage): ByteArray {
        val decryptedData: ByteArray
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        with (message) {
            val myParams = GCMParameterSpec(128, nonce)
            // AEADBadTagException
            try {
                cipher.init(Cipher.DECRYPT_MODE, key, myParams);
            } catch (keyError: IllegalArgumentException) {
                //TODO handle exception
                return ByteArray(0)
            }
            try {
                decryptedData = cipher.doFinal(data + tag)
            } catch (bte: AEADBadTagException) {
                //TODO handle exception
                return ByteArray(0)
            }
        }
        return decryptedData
    }

}