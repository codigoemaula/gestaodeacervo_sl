package br.gov.sp.sme.salaleitura.feature

import br.gov.sp.sme.salaleitura.feature.people.*
import org.junit.Assert.*
import org.junit.Test

class CsvRoundTripTest {
    @Test fun acceptsSemicolonBomMultilineAndQuotedFields() {
        val result = CsvImporter.parse("\uFEFFnome;tipo;identificador;turma;ano;funcao;codigo_interno\n\"Ana; Maria\";ESTUDANTE;123;7A;2026;;P-12345678")
        assertTrue(result.errors.toString(), result.errors.isEmpty())
        assertEquals("Ana; Maria", result.rows.single().name)
        assertEquals(2026, result.rows.single().schoolYear)
        val multiline = CsvImporter.parse("nome,tipo,funcao\n\"José\nLima\",PROFISSIONAL,\"Mediação \"\"de leitura\"\"\"")
        assertTrue(multiline.errors.toString(), multiline.errors.isEmpty())
        assertEquals("José\nLima", multiline.rows.single().name)
        assertEquals("Mediação \"de leitura\"", multiline.rows.single().role)
    }

    @Test fun exportCanReimportStableCodeAndNeutralizesFormula() {
        val row = PersonImportRow(2, "=SUM(1+2)", "ESTUDANTE", "+123", schoolYear = 2026, internalCode = "P-12345678")
        val text = CsvExporter.export(listOf(row))
        assertTrue(text.contains("\"\t=SUM(1+2)\""))
        assertTrue(text.contains("\"\t+123\""))
        val parsed = CsvImporter.parse(text)
        assertTrue(parsed.errors.toString(), parsed.errors.isEmpty())
        assertEquals(row.name, parsed.rows.single().name)
        assertEquals(row.institutionalId, parsed.rows.single().institutionalId)
        assertEquals(row.internalCode, parsed.rows.single().internalCode)
    }

    @Test fun invalidNumberOfColumnsReportsOriginalLineAndDoesNotImportIt() {
        val result = CsvImporter.parse("nome,tipo\nAna,ESTUDANTE,EXTRA\nJoão,PROFISSIONAL")
        assertEquals(1, result.rows.size)
        assertTrue(result.errors.single().contains("Linha 2"))
    }

    @Test fun reconcilePreservesExistingIdentityRejectsCollisionsAndChecksSchoolYear() {
        val reader = ExistingReader(42L, "P-12345678", "ESTUDANTE", "123")
        val classGroup = ExistingClass(6L, "7A", 2026)
        val row = PersonImportRow(2, "Ana", "ESTUDANTE", "123", "7A", internalCode = "P-12345678", schoolYear = 2026)
        val plan = CsvReconciler.plan(listOf(row), listOf(reader), listOf(classGroup), 2026)
        assertTrue(plan.errors.toString(), plan.errors.isEmpty())
        assertTrue(plan.inserts.isEmpty())
        assertEquals(42L, plan.updates.single().personId)
        assertEquals(6L, plan.updates.single().classGroupId)
        assertTrue(CsvReconciler.plan(listOf(row), emptyList(), listOf(classGroup), 2026).errors.any { it.contains("desconhecido") })
        assertTrue(CsvReconciler.plan(listOf(row, row.copy(line = 3)), listOf(reader), listOf(classGroup), 2026).errors.any { it.contains("duplicado") })
        assertTrue(CsvReconciler.plan(listOf(row.copy(schoolYear = 2027)), listOf(reader), listOf(classGroup), 2026).errors.any { it.contains("Turma") })
    }
}
