package yandex.cloud.toolkit.util

import com.intellij.util.io.decodeBase64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Key
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


object CryptUtils {

    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES", BouncyCastleProvider())
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun writeKey(key: Key): String {
        return Base64.getEncoder().encodeToString(key.encoded)
    }

    fun readKey(rawKey: String): Key {
        val decodedKey: ByteArray = Base64.getDecoder().decode(rawKey)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
    }

    fun encrypt(key: Key, toEncrypt: String): String {
        val cipher: Cipher = Cipher.getInstance("AES", BouncyCastleProvider())
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encrypted: ByteArray = cipher.doFinal(toEncrypt.toByteArray())
        return String(Base64.getEncoder().encode(encrypted))
    }

    fun decrypt(key: Key, encrypted: String): String {
        val cipher: Cipher = Cipher.getInstance("AES", BouncyCastleProvider())
        cipher.init(Cipher.DECRYPT_MODE, key)

        val decrypted: ByteArray = cipher.doFinal(encrypted.decodeBase64())
        return String(decrypted)
    }
}