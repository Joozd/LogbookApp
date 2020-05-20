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

import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import javax.crypto.Cipher

class RSAHandler {
    private val keyPair: KeyPair
    private val cipher: Cipher
    private val kpg: KeyPairGenerator

    val publicKey: PublicKey
        get() = keyPair.public

    init {
        kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        keyPair = kpg.genKeyPair()
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    }
    fun encrypt(data: ByteArray, publicKey: PublicKey): ByteArray {
        val encryptedBytes: ByteArray
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            encryptedBytes = cipher.doFinal(data)
        }catch (keyError: InvalidKeyException) {
            return ByteArray(0)
        }
        return encryptedBytes
    }
   fun decrypt(data: ByteArray): ByteArray {
        val decryptedBytes: ByteArray
        try {
            cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
            decryptedBytes = cipher.doFinal(data)
        } catch (keyError: InvalidKeyException) {
            return ByteArray(0)
        }
       return decryptedBytes
    }
}


