package br.gov.sp.sme.salaleitura.feature.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.repository.MetadataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UniversalScanUi(
    val title: String,
    val description: String,
    val code: String,
    val actions: List<UniversalScanAction>,
    val resolving: Boolean = false
)

/** Resolve a scan against stored records or public bibliographic data; recognition never records a loan. */
class UniversalScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val metadata = MetadataRepository(db)
    private val _scan = MutableStateFlow<UniversalScanUi?>(null)
    val scan: StateFlow<UniversalScanUi?> = _scan.asStateFlow()

    fun accept(result: ScanResult) {
        if (_scan.value != null) return
        _scan.value = UniversalScanUi("Identificando código", "Consultando o acervo e os dados bibliográficos…", "", emptyList(), true)
        viewModelScope.launch {
            val resolved = runCatching {
                when (result) {
                    is ScanResult.Copy -> {
                        val copy = db.catalogDao().copyByCode(result.code)
                        if (copy == null) notFound(result.code) else {
                            val book = db.catalogDao().editionById(copy.editionId)
                            UniversalScanUi(
                                title = book?.title ?: "Exemplar identificado",
                                description = "${copy.internalCode} · Situação: ${copy.status.displayName()}",
                                code = copy.internalCode,
                                actions = UniversalScanPolicy.copyActions(copy.status)
                            )
                        }
                    }
                    is ScanResult.Person -> {
                        val person = db.peopleDao().personByCode(result.code)
                        if (person == null) notFound(result.code) else UniversalScanUi(
                            title = person.name,
                            description = "Leitor identificado · ${person.internalCode}${if (!person.active) " · Cadastro inativo" else ""}",
                            code = person.internalCode,
                            actions = if (person.active) listOf(UniversalScanAction.CHECKOUT_PERSON) else listOf(UniversalScanAction.MANUAL_SEARCH)
                        )
                    }
                    is ScanResult.Isbn -> {
                        val edition = db.catalogDao().editionByIsbn13(result.isbn13)
                        if (edition != null) UniversalScanUi(
                            title = edition.title,
                            description = "${edition.authors} · Esta edição já está cadastrada. O cadastro acrescentará exemplares sem duplicá-la.",
                            code = result.isbn13,
                            actions = listOf(UniversalScanAction.REGISTER_ISBN, UniversalScanAction.CATALOG)
                        ) else {
                            // Resolving from the scanner seeds the same persistent cache used by the form.
                            val book = metadata.lookup(result.isbn13)
                            val (title, description) = BookScanSummary.from(book)
                            UniversalScanUi(
                                title = title,
                                description = "ISBN ${result.isbn13} · $description",
                                code = result.isbn13,
                                actions = listOf(UniversalScanAction.REGISTER_ISBN, UniversalScanAction.CATALOG)
                            )
                        }
                    }
                    is ScanResult.Unknown -> {
                        val copy = db.catalogDao().copyByCode(result.raw)
                        val person = db.peopleDao().personByCode(result.raw)
                        when {
                            copy != null && person != null -> UniversalScanUi(
                                "Identificação ambígua", "O código existe para pessoa e exemplar. Consulte os cadastros antes de agir.",
                                result.raw, UniversalScanPolicy.unknownActions()
                            )
                            copy != null -> UniversalScanUi(
                                db.catalogDao().editionById(copy.editionId)?.title ?: "Exemplar identificado",
                                "${copy.internalCode} · Situação: ${copy.status.displayName()}",
                                copy.internalCode, UniversalScanPolicy.copyActions(copy.status)
                            )
                            person != null -> UniversalScanUi(person.name, "Leitor identificado · ${person.internalCode}",
                                person.internalCode, if (person.active) listOf(UniversalScanAction.CHECKOUT_PERSON) else UniversalScanPolicy.unknownActions())
                            else -> notFound(result.raw)
                        }
                    }
                }
            }.getOrElse { UniversalScanUi("Não foi possível consultar", it.message ?: "Consulte manualmente o cadastro.", "", UniversalScanPolicy.unknownActions()) }
            _scan.value = resolved
        }
    }

    fun dismiss() { _scan.value = null }

    private fun notFound(code: String) = UniversalScanUi(
        "Código não localizado", "Nenhum registro local corresponde a $code. Confira a etiqueta ou use a busca manual.",
        code, UniversalScanPolicy.unknownActions()
    )
}

private fun br.gov.sp.sme.salaleitura.core.model.CopyStatus.displayName(): String = when(this) {
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.AVAILABLE -> "disponível"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.LOANED -> "emprestado"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.DAMAGED -> "danificado"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.LOST -> "extraviado"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.MAINTENANCE -> "em manutenção"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.WITHDRAWN -> "baixado"
}
