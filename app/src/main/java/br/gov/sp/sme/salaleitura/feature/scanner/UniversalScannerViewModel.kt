package br.gov.sp.sme.salaleitura.feature.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
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

/** Resolve a single scan against local records; no write is made by recognition. */
class UniversalScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val _scan = MutableStateFlow<UniversalScanUi?>(null)
    val scan: StateFlow<UniversalScanUi?> = _scan.asStateFlow()

    fun accept(result: ScanResult) {
        if (_scan.value != null) return // Keep focus on the current bottom panel until dismissed.
        _scan.value = UniversalScanUi("Identificando código", "Consultando os registros deste aparelho…", "", emptyList(), true)
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
                        UniversalScanUi(
                            title = edition?.title ?: "ISBN identificado",
                            description = if (edition != null) "Esta edição já consta do acervo. Confira antes de acrescentar exemplares." else "ISBN ${result.isbn13} · Obra ainda não cadastrada",
                            code = result.isbn13,
                            actions = listOf(UniversalScanAction.REGISTER_ISBN, UniversalScanAction.CATALOG)
                        )
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
