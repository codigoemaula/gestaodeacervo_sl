package br.gov.sp.sme.salaleitura.feature.circulation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.logic.CopySelectionPolicy
import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnCopyDecision
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult
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
    val selectedClassId: Long? = null, val candidateCopies: List<BookCopyEntity> = emptyList(),
    val candidateTitle: String? = null
)

data class ReturnUiState(val copyCode: String = "", val copy: BookCopyEntity? = null, val loanId: Long? = null, val person: PersonEntity? = null, val message: String? = null, val error: String? = null)

class CirculationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val repo = CirculationRepository(db)
    val activeLoans = db.loanDao().observeActiveLoanRows().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val readers = db.peopleDao().observePeople().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classes = db.peopleDao().observeClassGroups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val editions = db.catalogDao().observeEditions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _checkout = MutableStateFlow(CheckoutUiState()); val checkout: StateFlow<CheckoutUiState> = _checkout.asStateFlow()
    private val _return = MutableStateFlow(ReturnUiState()); val returns: StateFlow<ReturnUiState> = _return.asStateFlow()

    init { viewModelScope.launch { db.schoolDao().get()?.let { _checkout.value = _checkout.value.copy(period = LoanPeriodSelector.fromDefaultDays(it.defaultLoanDays)) } } }

    fun selectGroup(groupId: Long) {
        _checkout.value = _checkout.value.copy(selectedClassId = groupId, personCode = "", person = null,
            copyCode = "", copy = null, candidateCopies = emptyList(), candidateTitle = null,
            overdue = emptyList(), teacherConfirmedException = false, error = null, message = null)
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
        _checkout.value = _checkout.value.copy(personCode = reader.internalCode, person = reader,
            copyCode = "", copy = null, candidateCopies = emptyList(), candidateTitle = null,
            overdue = emptyList(), teacherConfirmedException = false, error = null, message = null)
    }

    fun setCopyCode(value: String) { _checkout.value = _checkout.value.copy(copyCode = value, copy = null, candidateCopies = emptyList(),
        candidateTitle = null, teacherConfirmedException = false, error = null, message = null) }
    fun selectPeriod(value: LoanPeriod) { _checkout.value = _checkout.value.copy(period = value) }
    fun confirmOverdueException(value: Boolean) { _checkout.value = _checkout.value.copy(teacherConfirmedException = value) }
    fun changeReader() {
        val s = _checkout.value
        _checkout.value = CheckoutUiState(period = s.period, selectedClassId = s.selectedClassId)
    }

    /** Choosing an edition uses the same local database as the Acervo screen; no network required. */
    fun selectEditionForCheckout(editionId: Long) = viewModelScope.launch {
        val edition = db.catalogDao().editionById(editionId) ?: run {
            _checkout.value = _checkout.value.copy(error = "Obra não encontrada no acervo"); return@launch
        }
        resolveEditionCopies(edition.id, edition.title)
    }

    private suspend fun resolveEditionCopies(editionId: Long, title: String) {
        val decision = CopySelectionPolicy.resolve(db.catalogDao().copiesByEdition(editionId))
        val s = _checkout.value
        _checkout.value = when (decision) {
            IsbnCopyDecision.NotCataloged -> s.copy(copy = null, candidateCopies = emptyList(), candidateTitle = null,
                error = "Obra sem exemplares físicos cadastrados", message = null)
            IsbnCopyDecision.NoneAvailable -> s.copy(copy = null, candidateCopies = emptyList(), candidateTitle = title,
                error = "Nenhum exemplar de '$title' está disponível para empréstimo", message = null)
            is IsbnCopyDecision.Single -> s.copy(copyCode = decision.copy.internalCode, copy = null,
                candidateCopies = emptyList(), candidateTitle = null, error = null, message = null).also {
                resolveCheckoutCode(it.copyCode)
            }
            is IsbnCopyDecision.ChooseCopy -> s.copy(copy = null, copyCode = "", candidateCopies = decision.available,
                candidateTitle = title, error = null, message = "Escolha o exemplar físico de '$title' que será entregue.")
        }
    }

    fun chooseCopy(copyId: Long) = viewModelScope.launch {
        val s = _checkout.value
        if (s.candidateCopies.none { it.id == copyId }) return@launch
        val fresh = db.catalogDao().copyById(copyId)
        if (fresh == null || fresh.status != CopyStatus.AVAILABLE) {
            _checkout.value = s.copy(error = "Exemplar indisponível. Consulte novamente o acervo.", copy = null)
            return@launch
        }
        resolveCheckoutCode(fresh.internalCode)
    }

    fun resolveCheckout() = viewModelScope.launch { resolveCheckoutCode(_checkout.value.copyCode) }

    private suspend fun resolveCheckoutCode(rawCode: String) {
        val s = _checkout.value
        val person = s.person?.let { db.peopleDao().personById(it.id) }
        val validReader = person?.takeIf { it.active && it.internalCode == s.personCode }
        if (validReader == null) {
            _checkout.value = s.copy(person = null, copy = null, error = "Selecione um estudante ou profissional")
            return
        }
        val code = rawCode.trim().let { if (it.startsWith("SL:", ignoreCase = true)) it.substringAfter(':').trim() else it }
        val copy = code.takeIf(String::isNotEmpty)?.let { db.catalogDao().copyByCode(it) }
        if (copy == null) {
            val isbn = Isbn.normalize(code)
            if (isbn is IsbnResult.Valid) {
                val edition = db.catalogDao().editionByIsbn13(isbn.isbn13)
                if (edition == null) {
                    _checkout.value = s.copy(copy = null, candidateCopies = emptyList(), candidateTitle = null,
                        error = "ISBN não cadastrado no acervo. Cadastre a obra e os exemplares primeiro.", message = null)
                } else resolveEditionCopies(edition.id, edition.title)
            } else _checkout.value = s.copy(copy = null, candidateCopies = emptyList(), candidateTitle = null,
                error = "Exemplar não encontrado. Informe o código interno, patrimônio ou ISBN cadastrado.", message = null)
            return
        }
        val overdue = db.loanDao().overdueForPerson(validReader.id, System.currentTimeMillis())
        _checkout.value = s.copy(person = validReader, copyCode = copy.internalCode, copy = copy,
            candidateCopies = emptyList(), candidateTitle = null, overdue = overdue, teacherConfirmedException = false,
            error = if (copy.status != CopyStatus.AVAILABLE) "Este exemplar não está disponível (${copy.status.name})." else null,
            message = null)
    }

    fun checkout() = viewModelScope.launch {
        val s = _checkout.value; val person = s.person ?: return@launch; val copy = s.copy ?: return@launch
        runCatching { repo.checkout(person.id, copy.id, s.period, teacherConfirmedException = s.teacherConfirmedException) }
            .onSuccess { _checkout.value = s.copy(copyCode = "", copy = null, candidateCopies = emptyList(), candidateTitle = null,
                teacherConfirmedException = false, message = "Empréstimo confirmado: ${copy.internalCode}. Escaneie o próximo exemplar ou troque de leitor.", error = null) }
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
