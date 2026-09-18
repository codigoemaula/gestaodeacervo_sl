package br.gov.sp.sme.salaleitura.feature.circulation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.logic.LoanPeriodSelector
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.BookCopyEntity
import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import br.gov.sp.sme.salaleitura.data.repository.CirculationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val personCode: String = "", val copyCode: String = "", val person: PersonEntity? = null, val copy: BookCopyEntity? = null,
    val period: LoanPeriod = LoanPeriod.SEVEN, val message: String? = null, val error: String? = null
)

data class ReturnUiState(val copyCode: String = "", val copy: BookCopyEntity? = null, val loanId: Long? = null, val person: PersonEntity? = null, val message: String? = null, val error: String? = null)

class CirculationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application); private val repo = CirculationRepository(db)
    val activeLoans = db.loanDao().observeActiveLoanRows().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _checkout = MutableStateFlow(CheckoutUiState()); val checkout: StateFlow<CheckoutUiState> = _checkout.asStateFlow()
    private val _return = MutableStateFlow(ReturnUiState()); val returns: StateFlow<ReturnUiState> = _return.asStateFlow()

    init { viewModelScope.launch { db.schoolDao().get()?.let { _checkout.value = _checkout.value.copy(period = LoanPeriodSelector.fromDefaultDays(it.defaultLoanDays)) } } }

    fun setPersonCode(value: String) { _checkout.value = _checkout.value.copy(personCode = value, person = null, error = null, message = null) }
    fun setCopyCode(value: String) { _checkout.value = _checkout.value.copy(copyCode = value, copy = null, error = null, message = null) }
    fun selectPeriod(value: LoanPeriod) { _checkout.value = _checkout.value.copy(period = value) }

    fun resolveCheckout() = viewModelScope.launch {
        val s = _checkout.value
        val person = db.peopleDao().personByCode(s.personCode.trim())
        val copy = db.catalogDao().copyByCode(s.copyCode.trim())
        _checkout.value = s.copy(person = person, copy = copy, error = when { person == null -> "Pessoa não encontrada"; copy == null -> "Exemplar não encontrado"; else -> null })
    }

    fun checkout() = viewModelScope.launch {
        val s = _checkout.value; val person = s.person ?: return@launch; val copy = s.copy ?: return@launch
        runCatching { repo.checkout(person.id, copy.id, s.period) }
            .onSuccess { _checkout.value = CheckoutUiState(period = s.period, message = "Empréstimo registrado") }
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
            .onSuccess { _return.value = ReturnUiState(message = "Devolução registrada") }
            .onFailure { _return.value = s.copy(error = it.message ?: "Falha na devolução") }
    }

    fun renew(loanId: Long, period: LoanPeriod) = viewModelScope.launch { repo.renew(loanId, period) }
}
