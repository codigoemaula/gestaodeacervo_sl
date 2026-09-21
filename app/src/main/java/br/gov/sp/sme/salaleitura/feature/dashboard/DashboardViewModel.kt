package br.gov.sp.sme.salaleitura.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.repository.MetadataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val _state = MutableStateFlow(DashboardStats())
    val state: StateFlow<DashboardStats> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch { MetadataRepository(db).refreshIsbnRanges() }
    }

    fun refresh() = viewModelScope.launch {
        val catalog = db.catalogDao(); val loans = db.loanDao(); val inventory = db.inventoryDao()
        val school = db.schoolDao().get()
        val zone = ZoneId.of("America/Sao_Paulo"); val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        _state.value = DashboardStats(
            schoolName = school?.name.orEmpty(), readingRoomName = school?.readingRoomName?.ifBlank { "Sala de Leitura" } ?: "Sala de Leitura",
            dueToday = loans.dueBetween(start, end),
            editions = catalog.editionCount(), copies = catalog.copyCount(), available = catalog.countByStatus(CopyStatus.AVAILABLE),
            activeLoans = loans.activeCount(), overdueLoans = loans.overdueCount(System.currentTimeMillis()),
            damagedOrLost = catalog.countByStatus(CopyStatus.DAMAGED) + catalog.countByStatus(CopyStatus.LOST),
            returnedToday = loans.returnedBetween(start, end), lastInventoryAt = inventory.latestClosed()?.closedAt
        )
    }
}
