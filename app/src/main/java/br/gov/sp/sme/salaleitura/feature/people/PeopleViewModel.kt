package br.gov.sp.sme.salaleitura.feature.people

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.core.model.Shift
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.ClassGroupEntity
import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import br.gov.sp.sme.salaleitura.data.repository.PeopleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PeopleViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application); private val repo = PeopleRepository(db)
    val people = db.peopleDao().observePeople().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classes = db.peopleDao().observeClassGroups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _import = MutableStateFlow(CsvImportResult(emptyList(), emptyList())); val import: StateFlow<CsvImportResult> = _import.asStateFlow()
    private val _importPlan = MutableStateFlow(CsvPlan(emptyList(), emptyList(), emptyList())); val importPlan: StateFlow<CsvPlan> = _importPlan.asStateFlow()
    private val _csvMessage = MutableStateFlow<String?>(null); val csvMessage: StateFlow<String?> = _csvMessage.asStateFlow()
    private var previewVersion = 0

    fun addClass(name: String, grade: String, shift: Shift, year: Int, onDone: () -> Unit = {}) = viewModelScope.launch {
        if (name.isNotBlank()) { repo.createClassGroup(ClassGroupEntity(name = name.trim(), grade = grade.trim(), shift = shift, schoolYear = year)); onDone() }
    }

    fun addPerson(name: String, type: PersonType, institutionalId: String?, classId: Long?, grade: String?, shift: Shift?, role: String?, onDone: () -> Unit = {}) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.createPerson(PersonEntity(internalCode = "", name = name.trim(), type = type, institutionalId = institutionalId?.trim()?.ifBlank { null }, classGroupId = classId, grade = grade, shift = shift, role = role?.trim()?.ifBlank { null })); onDone()
    }

    fun previewCsv(text: String) {
        val version = ++previewVersion
        val parsed = CsvImporter.parse(text)
        _import.value = parsed
        _importPlan.value = CsvPlan(emptyList(), emptyList(), emptyList())
        _csvMessage.value = null
        if (parsed.errors.isEmpty()) viewModelScope.launch {
            val plan = repo.previewCsv(parsed.rows)
            if (version == previewVersion) _importPlan.value = plan
        }
    }

    fun commitCsv(onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        val rows = _import.value.rows
        if (_import.value.errors.isNotEmpty() || _importPlan.value.errors.isNotEmpty() || rows.isEmpty()) return@launch
        runCatching { repo.importCsv(rows) }.onSuccess { (inserted, updated) ->
            _csvMessage.value = "$inserted novo(s), $updated atualizado(s)"
            onDone(inserted + updated)
        }.onFailure {
            _csvMessage.value = it.message ?: "Não foi possível importar o arquivo. Nenhuma alteração foi aplicada."
        }
    }

    fun saveCsv(uri: Uri, model: Boolean = false, onResult: (String) -> Unit = {}) = viewModelScope.launch {
        runCatching {
            val text = if (model) CsvExporter.template() else repo.exportCsv()
            withContext(Dispatchers.IO) {
                val stream = requireNotNull(getApplication<Application>().contentResolver.openOutputStream(uri)) { "Destino indisponível" }
                stream.bufferedWriter(Charsets.UTF_8).use { it.write("\uFEFF"); it.write(text) }
            }
        }.onSuccess { onResult(if (model) "Modelo CSV salvo" else "Cadastros exportados em CSV") }
            .onFailure { onResult(it.message ?: "Falha ao salvar o arquivo") }
    }
}
