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

/** Resolve only books/copies from camera. No person lookup, image capture or circulation mutation. */
class UniversalScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val metadata = MetadataRepository(db)
    private val _scan = MutableStateFlow<UniversalScanUi?>(null)
    val scan: StateFlow<UniversalScanUi?> = _scan.asStateFlow()

    fun accept(result: ScanResult) {
        if (_scan.value != null) return
        if (result is ScanResult.Person || (result is ScanResult.Unknown && isPersonLabel(result.raw))) {
            _scan.value = UniversalScanUi("Código não utilizado", "A identificação de estudantes e profissionais é feita por seleção na lista, sem câmera.", "", emptyList())
            return
        }
        _scan.value = UniversalScanUi("Identificando exemplar", "Consultando o acervo e os dados bibliográficos…", "", emptyList(), true)
        viewModelScope.launch {
            val resolved = runCatching {
                when (result) {
                    is ScanResult.Copy -> {
                        val copy = db.catalogDao().copyByCode(result.code)
                        if (copy == null) notFound() else {
                            val book = db.catalogDao().editionById(copy.editionId)
                            UniversalScanUi(
                                title = book?.title ?: "Exemplar identificado",
                                description = "${copy.internalCode} · Situação: ${copy.status.displayName()}",
                                code = copy.internalCode,
                                actions = UniversalScanPolicy.copyActions(copy.status)
                            )
                        }
                    }
                    is ScanResult.Person -> UniversalScanUi("Código não utilizado", "Selecione a pessoa pela turma, sem câmera.", "", emptyList())
                    is ScanResult.Isbn -> {
                        val edition = db.catalogDao().editionByIsbn13(result.isbn13)
                        if (edition != null) UniversalScanUi(
                            title = edition.title,
                            description = "${edition.authors} · Edição já cadastrada. O cadastro acrescentará exemplares sem duplicá-la.",
                            code = result.isbn13,
                            actions = listOf(UniversalScanAction.REGISTER_ISBN, UniversalScanAction.CATALOG)
                        ) else {
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
                        if (copy == null) notFound() else UniversalScanUi(
                            db.catalogDao().editionById(copy.editionId)?.title ?: "Exemplar identificado",
                            "${copy.internalCode} · Situação: ${copy.status.displayName()}",
                            copy.internalCode, UniversalScanPolicy.copyActions(copy.status)
                        )
                    }
                }
            }.getOrElse { UniversalScanUi("Não foi possível consultar", "Confira o exemplar ou utilize a busca no acervo.", "", UniversalScanPolicy.unknownActions()) }
            _scan.value = resolved
        }
    }

    fun dismiss() { _scan.value = null }

    private fun isPersonLabel(raw: String): Boolean = raw.trim().let { it.startsWith("PERSON:", true) || (it.startsWith("P-", true) && it.length >= 5) }
    private fun notFound() = UniversalScanUi("Exemplar não localizado", "Confira a etiqueta ou utilize a busca no acervo.", "", UniversalScanPolicy.unknownActions())
}

private fun br.gov.sp.sme.salaleitura.core.model.CopyStatus.displayName(): String = when (this) {
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.AVAILABLE -> "disponível"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.LOANED -> "emprestado"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.DAMAGED -> "danificado"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.LOST -> "extraviado"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.MAINTENANCE -> "em manutenção"
    br.gov.sp.sme.salaleitura.core.model.CopyStatus.WITHDRAWN -> "baixado"
}
