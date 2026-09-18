package br.gov.sp.sme.salaleitura.feature.dashboard

/** Put the task screen under its directed scanner to preserve the operation on return. */
object HomeRoutes {
    fun destinations(action: String): List<String> = when (action) {
        "checkout/scan" -> listOf("checkout", "scanner/person")
        "return/scan" -> listOf("return", "scanner/return_copy")
        "book/scan" -> listOf("book/new", "scanner/book")
        // Inventory must have a persisted session before it is allowed to scan copies.
        "inventory/scan" -> listOf("inventory")
        else -> listOf(action)
    }
}
