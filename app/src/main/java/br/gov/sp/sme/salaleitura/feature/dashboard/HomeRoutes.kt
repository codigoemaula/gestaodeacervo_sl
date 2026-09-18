package br.gov.sp.sme.salaleitura.feature.dashboard

/** Book scanning remains available; checkout begins with an explicit class/reader selection. */
object HomeRoutes {
    fun destinations(action: String): List<String> = when (action) {
        "checkout/scan" -> listOf("checkout")
        "return/scan" -> listOf("return", "scanner/return_copy")
        "book/scan" -> listOf("book/new", "scanner/book")
        "inventory/scan" -> listOf("inventory")
        else -> listOf(action)
    }
}
