package br.gov.sp.sme.salaleitura.feature

import br.gov.sp.sme.salaleitura.feature.backup.BackupManifest
import br.gov.sp.sme.salaleitura.feature.backup.BackupManifestCodec
import br.gov.sp.sme.salaleitura.feature.backup.Checksum
import br.gov.sp.sme.salaleitura.feature.people.CsvImporter
import br.gov.sp.sme.salaleitura.feature.scanner.ScanResult
import br.gov.sp.sme.salaleitura.feature.scanner.ScanRouter
import org.junit.Assert.*
import org.junit.Test

class ImportScanBackupTest {
    @Test fun csvUnderstandsQuotedNamesAndRejectsMissingType() {
        val good = CsvImporter.parse("""nome,tipo,identificador,turma,ano,turno,funcao
Ana Silva,ESTUDANTE,123,7A,2026,MATUTINO,
"João, Souza",PROFISSIONAL,RF99,,,,Professor""".trimIndent())
        assertTrue(good.errors.isEmpty())
        assertEquals("João, Souza", good.rows[1].name)
        assertTrue(CsvImporter.parse("nome,tipo\nSem Tipo,").errors.isNotEmpty())
    }

    @Test fun scannerRoutesIsbnAndCopiesButNeverPeople() {
        assertTrue(ScanRouter.route("9780306406157") is ScanResult.Isbn)
        assertEquals(ScanResult.Copy("SL-000123-001"), ScanRouter.route("SL:SL-000123-001"))
        assertTrue(ScanRouter.route("PERSON:P-ABCD1234") is ScanResult.Unknown)
    }

    @Test fun backupManifestRoundTripsAndChecksumIsSha256() {
        val hash = Checksum.sha256("database".toByteArray())
        val manifest = BackupManifest(1, "123456", "EMEF Exemplo", 1726570000000, hash)
        assertEquals(64, hash.length)
        assertEquals(manifest, BackupManifestCodec.decode(BackupManifestCodec.encode(manifest)))
    }
}
