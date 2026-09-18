package br.gov.sp.sme.salaleitura.feature.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.logic.SchoolSetupData
import br.gov.sp.sme.salaleitura.core.logic.SchoolSetupValidator
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.SchoolEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SchoolSetupUiState(
    val name: String = "",
    val eolCode: String = "",
    val dre: String = "",
    val schoolYear: String = java.time.Year.now().value.toString(),
    val readingRoomName: String = "Sala de Leitura",
    val defaultLoanDays: Int = 7,
    val errors: List<String> = emptyList(),
    val saved: Boolean = false
)

class SchoolSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).schoolDao()
    private val _state = MutableStateFlow(SchoolSetupUiState())
    val state: StateFlow<SchoolSetupUiState> = _state.asStateFlow()

    fun update(transform: (SchoolSetupUiState) -> SchoolSetupUiState) { _state.value = transform(_state.value).copy(errors = emptyList()) }

    fun save() {
        val s = _state.value
        val year = s.schoolYear.toIntOrNull() ?: 0
        val input = SchoolSetupData(s.name.trim(), s.eolCode.trim(), s.dre.trim(), year, s.readingRoomName.trim(), s.defaultLoanDays)
        val errors = SchoolSetupValidator.validate(input)
        if (errors.isNotEmpty()) { _state.value = s.copy(errors = errors); return }
        viewModelScope.launch {
            dao.upsert(SchoolEntity(name = input.name, eolCode = input.eolCode, dre = input.dre, schoolYear = input.schoolYear, readingRoomName = input.readingRoomName, defaultLoanDays = input.defaultLoanDays))
            _state.value = _state.value.copy(saved = true)
        }
    }
}
