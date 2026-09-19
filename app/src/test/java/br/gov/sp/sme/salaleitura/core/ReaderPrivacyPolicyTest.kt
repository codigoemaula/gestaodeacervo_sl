package br.gov.sp.sme.salaleitura.core

import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import br.gov.sp.sme.salaleitura.feature.people.ReaderPrivacyPolicy
import org.junit.Assert.*
import org.junit.Test

class ReaderPrivacyPolicyTest {
    private val reader = PersonEntity(id=7, internalCode="P-ABCDEF12", name="Estudante Identificado", type=PersonType.STUDENT,
        institutionalId="12345", classGroupId=22, grade="7", role="Monitor", photoFilename="11111111-1111-1111-1111-111111111111.jpg")

    @Test fun `active loans prevent irreversible privacy removal until returned`() {
        assertThrows(IllegalArgumentException::class.java) { ReaderPrivacyPolicy.anonymize(reader, true) }
    }

    @Test fun `anonymization preserves loan foreign key but erases identifying profile`() {
        val redacted = ReaderPrivacyPolicy.anonymize(reader, false)
        assertEquals(reader.id, redacted.id)
        assertEquals("Leitor anonimizado", redacted.name)
        assertTrue(redacted.internalCode.startsWith("ANON-"))
        assertNotEquals(reader.internalCode, redacted.internalCode)
        assertNull(redacted.institutionalId)
        assertNull(redacted.classGroupId)
        assertNull(redacted.grade)
        assertNull(redacted.shift)
        assertNull(redacted.role)
        assertNull(redacted.photoFilename)
        assertFalse(redacted.active)
    }
}
