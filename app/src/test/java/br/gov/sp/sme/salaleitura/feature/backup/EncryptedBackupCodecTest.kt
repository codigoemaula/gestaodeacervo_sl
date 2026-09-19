package br.gov.sp.sme.salaleitura.feature.backup

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class EncryptedBackupCodecTest {
    private val password = "frase-segura-com-12-caracteres".toCharArray()

    private fun encrypted(data: ByteArray, secret: CharArray = password): ByteArray {
        val output = ByteArrayOutputStream()
        EncryptedBackupCodec.encrypt(ByteArrayInputStream(data), output, secret)
        return output.toByteArray()
    }

    private fun decrypted(data: ByteArray, secret: CharArray = password): ByteArray {
        val output = ByteArrayOutputStream()
        EncryptedBackupCodec.decrypt(ByteArrayInputStream(data), output, secret)
        return output.toByteArray()
    }

    @Test fun `database and photos round trip without plaintext appearing in archive`() {
        val plain = ("student-data-".repeat(1000)).toByteArray()
        val archive = encrypted(plain)
        assertFalse(String(archive, Charsets.ISO_8859_1).contains("student-data"))
        assertArrayEquals(plain, decrypted(archive))
    }

    @Test fun `different encryptions use independent random salt and nonce`() {
        val plain = "private".toByteArray()
        assertFalse(encrypted(plain).contentEquals(encrypted(plain)))
    }

    @Test fun `incorrect password never returns restored data`() {
        val archive = encrypted("secret".toByteArray())
        assertThrows(Exception::class.java) { decrypted(archive, "senha-errada-123".toCharArray()) }
    }

    @Test fun `tampered tag is rejected`() {
        val archive = encrypted("secret".toByteArray())
        archive[archive.lastIndex] = (archive.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { decrypted(archive) }
    }

    @Test fun `weak password and invalid format are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { encrypted(byteArrayOf(1), "123456".toCharArray()) }
        assertThrows(IllegalArgumentException::class.java) { decrypted(byteArrayOf(1, 2, 3)) }
    }
}
