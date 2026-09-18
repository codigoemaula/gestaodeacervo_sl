package br.gov.sp.sme.salaleitura.feature.dashboard

data class DashboardStats(
    val editions: Int = 0,
    val copies: Int = 0,
    val available: Int = 0,
    val activeLoans: Int = 0,
    val overdueLoans: Int = 0,
    val damagedOrLost: Int = 0,
    val returnedToday: Int = 0,
    val lastInventoryAt: Long? = null
)
