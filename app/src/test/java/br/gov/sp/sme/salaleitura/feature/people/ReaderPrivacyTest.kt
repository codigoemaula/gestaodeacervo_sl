package br.gov.sp.sme.salaleitura.feature.people

import br.gov.sp.sme.salaleitura.feature.scanner.ScanResult
import br.gov.sp.sme.salaleitura.feature.scanner.ScanRouter
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPrivacyTest {
    @Test fun codesOfPeopleAreNeverRecognizedAsCameraTargets() {
        assertTrue(ScanRouter.route("PERSON:P-AABBCCDD") is ScanResult.Unknown)
        assertTrue(ScanRouter.route("P-AABBCCDD") is ScanResult.Unknown)
    }
}
