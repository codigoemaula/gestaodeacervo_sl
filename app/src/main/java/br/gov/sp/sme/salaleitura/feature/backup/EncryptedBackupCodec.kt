package br.gov.sp.sme.salaleitura.feature.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** SLB2 = magic (4), random salt (16), random IV (12), AES-256-GCM ciphertext and tag. */
object EncryptedBackupCodec {
    private val magic = byteArrayOf(0x53, 0x4c, 0x42, 0x32)
    private const val ITERATIONS = 310_000
    private val random = SecureRandom()

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        require(password.size >= 10) { "Use uma senha de backup com pelo menos 10 caracteres." }
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(input: InputStream, output: OutputStream, password: CharArray) {
        require(password.size >= 10) { "Use uma senha de backup com pelo menos 10 caracteres." }
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        output.write(magic)
        output.write(salt)
        output.write(iv)
        // Caller owns output: closing CipherOutputStream finalizes the authenticated tag.
        CipherOutputStream(output, cipher).use { encrypted -> input.copyTo(encrypted) }
    }

    /** Decrypt into a PRIVATE TEMPORARY file first: authentication is verified only at EOF. */
    fun decrypt(input: InputStream, output: OutputStream, password: CharArray) {
        val header = ByteArray(32)
        var pos = 0
        while (pos < header.size) {
            val count = input.read(header, pos, header.size - pos)
            require(count > 0) { "Formato de backup inválido ou incompleto." }
            pos += count
        }
        require(header.copyOfRange(0, 4).contentEquals(magic)) { "Formato de backup criptografado inválido." }
        val salt = header.copyOfRange(4, 20)
        val iv = header.copyOfRange(20, 32)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        CipherInputStream(input, cipher).use { decrypted -> decrypted.copyTo(output) }
    }
}
