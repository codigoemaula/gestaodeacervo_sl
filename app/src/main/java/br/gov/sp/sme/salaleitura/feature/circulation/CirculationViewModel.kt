package br.gov.sp.sme.salaleitura.feature.circulation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.logic.LoanPeriodSelector
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.dao.ActiveLoanRow
import br.gov.sp.sme.salaleitura.data.local.entity.BookCopyEntity
import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import br.gov.sp.sme.salaleitura.data.repository.CirculationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Negative IDs represent separate lists, never persisted as classroom foreign keys. */
object ReaderGroup { const val PROFESSIONALS = -1L; const val WITHOUT_CLASS = -2L }

data class CheckoutUiState(
    val personCode: String = "", val copyCode: String = "", val person: PersonEntity? = null, val copy: BookCopyEntity? = null,
    val period: LoanPeriod = LoanPeriod.SEVEN, val message: String? = null, val error: String? = null,
    val overdue: List<ActiveLoanRow> = emptyList(), val teacherConfirmedException: Boolean = false,
    val selectedClassId: Long? = null
)

data class ReturnUiState(val copyCode: String = "", val copy: BookCopyEntity? = null, val loanId: Long? = null, val person: PersonEntity? = null, val message: String? = null, val error: String? = null)

class CirculationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val repo = CirculationRepository(db)
    val activeLoans = db.loanDao().observeActiveLoanRows().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val readers = db.peopleDao().observePeople().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classes = db.peopleDao().observeClassGroups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _checkout = MutableStateFlow(CheckoutUiState()); val checkout: StateFlow<CheckoutUiState> = _checkout.asStateFlow()
    private val _return = MutableStateFlow(ReturnUiState()); val returns: StateFlow<ReturnUiState> = _return.asStateFlow()

    init { viewModelScope.launch { db.schoolDao().get()?.let { _checkout.value = _checkout.value.copy(period = LoanPeriodSelector.fromDefaultDays(it.defaultLoanDays)) } } }

    fun selectGroup(groupId: Long) {
        _checkout.value = _checkout.value.copy(selectedClassId = groupId, personCode = "", person = null,
            copy = null, overdue = emptyList(), teacherConfirmedException = false, error = null, message = null)
    }

    fun selectReader(reader: PersonEntity) {
        val group = _checkout.value.selectedClassId
        val belongs = reader.active && when (group) {
            ReaderGroup.PROFESSIONALS -> reader.type == PersonType.PROFESSIONAL
            ReaderGroup.WITHOUT_CLASS -> reader.type == PersonType.STUDENT && reader.classGroupId == null
            null -> false
            else -> reader.type == PersonType.STUDENT && reader.classGroupId == group
        }
        if (!belongs) { _checkout.value = _checkout.value.copy(error = "Selecione uma pessoa da turma escolhida"); return }
        _checkout.value = _checkout.value.copy(personCode = reader.internalCode, person = reader, copy = null,
            overdue = emptyList(), teacherConfirmedException = false, error = null, message = null)
    }

    fun setCopyCode(value: String) { _checkout.value = _checkout.value.copy(copyCode = value, copy = null, teacherConfirmedException = false, error = null, message = null) }
    fun selectPeriod(value: LoanPeriod) { _checkout.value = _checkout.value.copy(period = value) }
    fun confirmOverdueException(value: Boolean) { _checkout.value = _checkout.value.copy(teacherConfirmedException = value) }
    fun changeReader() {
        val s = _checkout.value
        _checkout.value = CheckoutUiState(copyCode = s.copyCode, period = s.period, selectedClassId = s.selectedClassId)
    }

    fun resolveCheckout() = viewModelScope.launch {
        val s = _checkout.value
        val person = s.person?.let { db.peopleDao().personById(it.id) }
        val copy = s.copyCode.trim().takeIf(String::isNotEmpty)?.let { db.catalogDao().copyByCode(it) }
        val validReader = person?.takeIf { it.active && it.internalCode == s.personCode }
        val overdue = validReader?.let { db.loanDao().overdueForPerson(it.id, System.currentTimeMillis()) }.orEmpty()
        _checkout.value = s.copy(person = validReader, copy = copy, overdue = overdue, teacherConfirmedException = false,
            error = when { validReader == null -> "Selecione um estudante ou profissional"; copy == null -> "Exemplar não encontrado"; else -> null })
    }

    fun checkout() = viewModelScope.launch {
        val s = _checkout.value; val person = s.person ?: return@launch; val copy = s.copy ?: return@launch
        runCatching { repo.checkout(person.id, copy.id, s.period, teacherConfirmedException = s.teacherConfirmedException) }
            .onSuccess { _checkout.value = s.copy(copyCode = "", copy = null, teacherConfirmedException = false,
                message = "Empréstimo confirmado: ${copy.internalCode}. Escaneie o próximo exemplar ou troque de leitor.", error = null) }
            .onFailure { _checkout.value = s.copy(error = it.message ?: "Falha no empréstimo") }
    }

    fun setReturnCopyCode(value: String) { _return.value = ReturnUiState(copyCode = value) }
    fun resolveReturn() = viewModelScope.launch {
        val code = _return.value.copyCode.trim(); val copy = db.catalogDao().copyByCode(code)
        if (copy == null) { _return.value = _return.value.copy(error = "Exemplar não encontrado"); return@launch }
        val loan = db.loanDao().activeLoanByCopy(copy.id)
        if (loan == null) { _return.value = _return.value.copy(copy = copy, error = "Este exemplar não possui empréstimo ativo"); return@launch }
        val person = db.peopleDao().personById(loan.personId)
        _return.value = _return.value.copy(copy = copy, loanId = loan.id, person = person, error = null)
    }

    fun returnCopy(status: CopyStatus) = viewModelScope.launch {
        val s = _return.value; val copy = s.copy ?: return@launch
        runCatching { repo.returnCopy(copy.id, status) }
            .onSuccess { _return.value = ReturnUiState(message = "Devolução registrada: ${copy.internalCode}. Escaneie o próximo exemplar.") }
            .onFailure { _return.value = s.copy(error = it.message ?: "Falha na devolução") }
    }

    fun renew(loanId: Long, period: LoanPeriod) = viewModelScope.launch { repo.renew(loanId, period) }
}
