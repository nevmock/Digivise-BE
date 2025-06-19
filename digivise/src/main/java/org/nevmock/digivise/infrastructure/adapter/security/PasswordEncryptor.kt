package org.nevmock.digivise.infrastructure.adapter.security

import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


object PasswordEncryptor {
    private const val SECRET_KEY = "digivisehebat123"
    @Throws(Exception::class)
    fun encrypt(plainText: String): String {
        val secretKey: SecretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
        val cipher: Cipher = Cipher.getInstance("AES")

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes: ByteArray = cipher.doFinal(plainText.toByteArray())

        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    @Throws(Exception::class)
    fun decrypt(encryptedText: String?): String {
        val secretKey: SecretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
        val cipher: Cipher = Cipher.getInstance("AES")

        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes: ByteArray = cipher.doFinal(Base64.getDecoder().decode(encryptedText))

        return String(decryptedBytes)
    }
}