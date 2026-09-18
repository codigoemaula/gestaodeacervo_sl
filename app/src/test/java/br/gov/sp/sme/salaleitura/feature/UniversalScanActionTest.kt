package br.gov.sp.sme.salaleitura.feature

import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.feature.scanner.UniversalScanAction
import br.gov.sp.sme.salaleitura.feature.scanner.UniversalScanPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class UniversalScanActionTest {
    @Test fun copyActionsFollowPhysicalAvailability() {
        assertEquals(listOf(UniversalScanAction.CHECKOUT_COPY, UniversalScanAction.CATALOG), UniversalScanPolicy.copyActions(CopyStatus.AVAILABLE))
        assertEquals(listOf(UniversalScanAction.RETURN_COPY, UniversalScanAction.CATALOG), UniversalScanPolicy.copyActions(CopyStatus.LOANED))
        assertEquals(listOf(UniversalScanAction.CATALOG), UniversalScanPolicy.copyActions(CopyStatus.DAMAGED))
    }
    @Test fun unknownCodeCannotCauseCirculation() {
        assertEquals(listOf(UniversalScanAction.MANUAL_SEARCH), UniversalScanPolicy.unknownActions())
    }
}
