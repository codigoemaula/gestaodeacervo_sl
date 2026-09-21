package br.gov.sp.sme.salaleitura.feature.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.logic.InventorySummary
import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class InventoryUiState(
    val sessionId: Long? = null, val code: String = "", val summary: InventorySummary? = null,
    val message: String? = null, val error: String? = null, val lastScanned: String? = null, val busy: Boolean = false
)

class InventoryViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val repo = InventoryRepository(db)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(InventoryUiState())
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()
    init {
        viewModelScope.launch {
            db.inventoryDao().activeSession()?.let {
                _state.value = _state.value.copy(sessionId = it.id, summary = repo.summary(it.id))
            }
        }
    }
    fun setCode(v: String) { _state.value = _state.value.copy(code = v, error = null, message = null) }
    fun start() = viewModelScope.launch {
        runCatching { repo.start() }
            .onSuccess { _state.value = InventoryUiState(sessionId = it, summary = repo.summary(it)) }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }
    fun scan() { scanCode(_state.value.code) }
    fun scanCode(raw: String) = viewModelScope.launch {
        mutex.withLock {
            val id = _state.value.sessionId ?: return@withLock
            val code = raw.trim().removePrefix("SL:").trim()
            if (code.isBlank()) {
                _state.value = _state.value.copy(error = "Informe ou escaneie o código do exemplar")
                return@withLock
            }
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching {
                val copy = db.catalogDao().copyByCode(code)
                if (copy == null) {
                    val issue = if (Isbn.normalize(code) is IsbnResult.Valid) "ISBN identifica uma edição, não o exemplar físico. Leia a etiqueta SL ou o patrimônio vinculado." else "Código não cadastrado: $code"
                    _state.value = _state.value.copy(code = "", lastScanned = code, error = issue, message = null, busy = false)
                    return@runCatching
                }
                val recorded = repo.scan(id, copy.internalCode)
                val summary = repo.summary(id)
                _state.value = _state.value.copy(code = "", lastScanned = copy.internalCode, summary = summary,
                    message = if (recorded) "Conferido: ${copy.internalCode} (${summary.found}/${summary.registered})" else "Já conferido nesta sessão: ${copy.internalCode}",
                    error = null, busy = false)
            }.onFailure { _state.value = _state.value.copy(error = it.message ?: "Falha ao conferir exemplar", busy = false) }
        }
    }
    fun close() = viewModelScope.launch {
        mutex.withLock {
            val id = _state.value.sessionId ?: return@withLock
            runCatching { repo.close(id) }
                .onSuccess { _state.value = _state.value.copy(sessionId = null, summary = it, message = "Inventário encerrado") }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Falha ao encerrar") }
        }
    }
}
