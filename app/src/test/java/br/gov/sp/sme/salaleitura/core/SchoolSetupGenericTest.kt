package br.gov.sp.sme.salaleitura.core

import br.gov.sp.sme.salaleitura.core.logic.SchoolSetupData
import br.gov.sp.sme.salaleitura.core.logic.SchoolSetupValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolSetupGenericTest {
    @Test fun `school can be registered without network-specific identifiers`() {
        val errors = SchoolSetupValidator.validate(
            SchoolSetupData("Escola comunitária", "", "", 2026, "Biblioteca", 7)
        )
        assertTrue("Identificação e região devem ser opcionais: $errors", errors.isEmpty())
    }
}
