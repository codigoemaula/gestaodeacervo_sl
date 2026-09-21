package br.gov.sp.sme.salaleitura.feature.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.BookEditionEntity
import br.gov.sp.sme.salaleitura.data.remote.BookMetadata
import br.gov.sp.sme.salaleitura.data.remote.MetadataLookupResult
import br.gov.sp.sme.salaleitura.data.repository.CatalogRepository
import br.gov.sp.sme.salaleitura.data.repository.CopyIdentityRepository
import br.gov.sp.sme.salaleitura.data.repository.MetadataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BookFormState(
    val isbn: String = "", val title: String = "", val subtitle: String = "", val authors: String = "", val publisher: String = "",
    val publicationYear: String = "", val language: String = "pt-BR", val pageCount: String = "", val subjects: String = "", val series: String = "",
    val cdd: String = "", val cdu: String = "", val location: String = "", val quantity: String = "1", val error: String? = null,
    val loadingMetadata: Boolean = false, val saved: Boolean = false, val existingEditionId: Long? = null,
    val metadataSource: String? = null, val saving: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val catalogRepo = CatalogRepository(db)
    private val identity = CopyIdentityRepository(db)
    private val metadataRepo = MetadataRepository(db)
    val editions = db.catalogDao().observeEditions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _selectedEdition = MutableStateFlow<Long?>(null)
    val selectedEdition: StateFlow<Long?> = _selectedEdition.asStateFlow()
    val selectedCopies = _selectedEdition.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else db.catalogDao().observeCopies(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedAliases = _selectedEdition.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else db.aliasDao().observeForEdition(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _catalogMessage = MutableStateFlow<String?>(null)
    val catalogMessage: StateFlow<String?> = _catalogMessage.asStateFlow()
    private val _form = MutableStateFlow(BookFormState())
    val form: StateFlow<BookFormState> = _form.asStateFlow()
    private var lookupRevision = 0

    fun selectEdition(id: Long) { _selectedEdition.value = if (_selectedEdition.value == id) null else id; _catalogMessage.value = null }
    fun findCopy(code: String) = viewModelScope.launch {
        val clean = code.trim().removePrefix("SL:")
        val copy = db.catalogDao().copyByCode(clean)
        if (copy == null) _catalogMessage.value = "Exemplar ou patrimônio não encontrado: $clean"
        else {
            _selectedEdition.value = copy.editionId
            _catalogMessage.value = "Exemplar encontrado: ${copy.internalCode}"
        }
    }
    fun bindPatrimony(copyId: Long, code: String) = viewModelScope.launch {
        runCatching { identity.bindPatrimony(copyId, code) }
            .onSuccess { _catalogMessage.value = "Patrimônio $it vinculado ao exemplar" }
            .onFailure { _catalogMessage.value = it.message ?: "Não foi possível vincular o patrimônio" }
    }
    fun removePatrimony(copyId: Long) = viewModelScope.launch {
        runCatching { identity.unlinkPatrimony(copyId) }
            .onSuccess { _catalogMessage.value = "Código patrimonial desvinculado; identificação interna preservada" }
            .onFailure { _catalogMessage.value = it.message ?: "Não foi possível desvincular" }
    }

    fun update(transform: (BookFormState) -> BookFormState) {
        val old = _form.value
        val updated = transform(old)
        _form.value = if (old.isbn != updated.isbn) {
            ++lookupRevision
            // Never leave another edition's title/author on screen after changing ISBN by hand.
            BookFormState(isbn = updated.isbn, location = updated.location, quantity = updated.quantity)
        } else updated.copy(error = null)
    }

    /** Explicit button bypasses cache, while camera uses fresh cached data whenever possible. */
    fun lookupMetadata() { lookupMetadata(forceRefresh = true) }

    private fun lookupMetadata(forceRefresh: Boolean) {
        val current = _form.value
        val valid = Isbn.normalize(current.isbn)
        if (valid !is IsbnResult.Valid) { _form.value = current.copy(error = (valid as IsbnResult.Invalid).reason); return }
        val revision = ++lookupRevision
        viewModelScope.launch {
            _form.value = _form.value.copy(loadingMetadata = true, error = null, metadataSource = null)
            val local = db.catalogDao().editionByIsbn13(valid.isbn13)
            if (revision != lookupRevision) return@launch
            if (local != null) {
                _form.value = _form.value.copy(
                    isbn = valid.isbn13, title = local.title, subtitle = local.subtitle.orEmpty(), authors = local.authors,
                    publisher = local.publisher.orEmpty(), publicationYear = local.publicationYear?.toString().orEmpty(),
                    language = local.language.orEmpty().ifBlank { "pt-BR" }, pageCount = local.pageCount?.toString().orEmpty(),
                    subjects = local.subjects.orEmpty(), series = local.series.orEmpty(), cdd = local.cdd.orEmpty(), cdu = local.cdu.orEmpty(),
                    existingEditionId = local.id, loadingMetadata = false, error = null, metadataSource = "ACERVO LOCAL"
                )
                return@launch
            }
            val result = runCatching { metadataRepo.lookupDetailed(valid.isbn13, forceRefresh = forceRefresh) }.getOrElse {
                MetadataLookupResult.Unavailable(listOf("consulta bibliográfica"), emptyList())
            }
            if (revision != lookupRevision) return@launch
            _form.value = when (result) {
                is MetadataLookupResult.Found -> applyMetadata(_form.value, result.book).copy(
                    loadingMetadata = false, isbn = valid.isbn13, existingEditionId = null,
                    metadataSource = result.book.source, error = null
                )
                is MetadataLookupResult.Missing -> _form.value.copy(
                    isbn = valid.isbn13, loadingMetadata = false, existingEditionId = null,
                    error = "As fontes consultadas (${result.searchedSources.joinToString()}) não localizaram este ISBN. Ele pode constar em outros catálogos; confira a edição ou preencha manualmente."
                )
                is MetadataLookupResult.Unavailable -> _form.value.copy(
                    isbn = valid.isbn13, loadingMetadata = false, existingEditionId = null,
                    error = "Consulta incompleta: ${result.failedSources.joinToString()} indisponível(is). Verifique a conexão e tente novamente. Isto não significa que o livro não exista."
                )
            }
        }
    }

    fun save() {
        val s = _form.value; if (s.saving) return
        val q = s.quantity.toIntOrNull() ?: 0
        if (s.title.isBlank()) { _form.value = s.copy(error = "Informe o título"); return }
        if (q !in 1..999) { _form.value = s.copy(error = "Quantidade deve estar entre 1 e 999"); return }
        val isbnResult = s.isbn.takeIf(String::isNotBlank)?.let(Isbn::normalize)
        if (isbnResult is IsbnResult.Invalid) { _form.value = s.copy(error = isbnResult.reason); return }
        val valid = isbnResult as? IsbnResult.Valid
        _form.value = s.copy(saving = true)
        viewModelScope.launch {
            runCatching {
                val existing = s.existingEditionId ?: valid?.isbn13?.let { db.catalogDao().editionByIsbn13(it)?.id }
                if (existing != null) {
                    catalogRepo.addCopies(existing, q, s.location.trim().ifBlank { null }, System.currentTimeMillis())
                    existing
                } else catalogRepo.createEditionWithCopies(
                    BookEditionEntity(
                        isbn10 = valid?.isbn10, isbn13 = valid?.isbn13, title = s.title.trim(), subtitle = s.subtitle.trim().ifBlank { null },
                        authors = s.authors.trim(), publisher = s.publisher.trim().ifBlank { null }, publicationYear = s.publicationYear.toIntOrNull(),
                        language = s.language.trim().ifBlank { null }, subjects = s.subjects.trim().ifBlank { null }, pageCount = s.pageCount.toIntOrNull(),
                        series = s.series.trim().ifBlank { null }, cdd = s.cdd.trim().ifBlank { null }, cdu = s.cdu.trim().ifBlank { null }, createdAt = System.currentTimeMillis()
                    ), q, s.location.trim().ifBlank { null }, System.currentTimeMillis()
                )
            }.onSuccess { _form.value = _form.value.copy(saved = true, saving = false) }
             .onFailure { _form.value = _form.value.copy(saving = false, error = it.message ?: "Falha ao salvar") }
        }
    }

    fun applyScannedIsbn(value: String) {
        val previous = _form.value
        _form.value = if (previous.isbn == value) previous.copy(error = null) else BookFormState(
            isbn = value, location = previous.location, quantity = previous.quantity
        )
        lookupMetadata(forceRefresh = false)
    }

    private fun applyMetadata(s: BookFormState, m: BookMetadata) = s.copy(
        title = m.title, subtitle = m.subtitle.orEmpty(), authors = m.authors.joinToString("; "), publisher = m.publisher.orEmpty(),
        publicationYear = m.publicationYear?.toString().orEmpty(), language = m.language ?: s.language, pageCount = m.pageCount?.toString().orEmpty(), subjects = m.subjects.joinToString("; ")
    )
}
