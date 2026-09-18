package br.gov.sp.sme.salaleitura.feature

import br.gov.sp.sme.salaleitura.feature.dashboard.HomeRoutes
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRouteTest {
    @Test fun checkoutBeginsWithReaderSelectionAndOtherCameraFlowsRemainBookOnly() {
        assertEquals(listOf("checkout"), HomeRoutes.destinations("checkout/scan"))
        assertEquals(listOf("return", "scanner/return_copy"), HomeRoutes.destinations("return/scan"))
        assertEquals(listOf("book/new", "scanner/book"), HomeRoutes.destinations("book/scan"))
    }
    @Test fun universalCameraAndInventoryHavePredictableDestinations() {
        assertEquals(listOf("scanner/universal"), HomeRoutes.destinations("scanner/universal"))
        assertEquals(listOf("inventory"), HomeRoutes.destinations("inventory/scan"))
        assertEquals(listOf("people"), HomeRoutes.destinations("people"))
    }
}
