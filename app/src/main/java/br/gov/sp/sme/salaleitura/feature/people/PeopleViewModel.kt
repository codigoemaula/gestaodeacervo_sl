package br.gov.sp.sme.salaleitura.feature.people

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.core.model.Shift
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.ClassGroupEntity
import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import br.gov.sp.sme.salaleitura.data.repository.PeopleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PeopleViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application); private val repo = PeopleRepository(db)
    val people = db.peopleDao().observePeople().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classes = db.peopleDao().observeClassGroups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _import = MutableStateFlow(CsvImportResult(emptyList(), emptyList())); val import: StateFlow<CsvImportResult> = _import.asStateFlow()

    fun addClass(name: String, grade: String, shift: Shift, year: Int, onDone: () -> Unit = {}) = viewModelScope.launch {
        if (name.isNotBlank()) { repo.createClassGroup(ClassGroupEntity(name = name.trim(), grade = grade.trim(), shift = shift, schoolYear = year)); onDone() }
    }

    fun addPerson(name: String, type: PersonType, institutionalId: String?, classId: Long?, grade: String?, shift: Shift?, role: String?, onDone: () -> Unit = {}) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.createPerson(PersonEntity(internalCode = "", name = name.trim(), type = type, institutionalId = institutionalId?.trim()?.ifBlank { null }, classGroupId = classId, grade = grade, shift = shift, role = role?.trim()?.ifBlank { null })); onDone()
    }

    fun previewCsv(text: String) { _import.value = CsvImporter.parse(text) }

    fun commitCsv(onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        val rows = _import.value.rows; if (_import.value.errors.isNotEmpty()) return@launch
        var count = 0
        val knownClasses = classes.value
        rows.forEach { row ->
            val type = if (row.type in setOf("ESTUDANTE", "STUDENT")) PersonType.STUDENT else PersonType.PROFESSIONAL
            val classId = row.className?.let { requested -> knownClasses.firstOrNull { it.name.equals(requested, ignoreCase = true) }?.id }
            repo.createPerson(PersonEntity(internalCode = "", name = row.name, type = type, institutionalId = row.institutionalId, classGroupId = classId, grade = row.grade, shift = parseShift(row.shift), role = row.role)); count++
        }
        onDone(count)
    }

    private fun parseShift(value: String?): Shift? = when (value?.uppercase()) {
        "MATUTINO", "MANHA", "MANHÃ", "MORNING" -> Shift.MORNING
        "VESPERTINO", "TARDE", "AFTERNOON" -> Shift.AFTERNOON
        "NOTURNO", "NOITE", "EVENING" -> Shift.EVENING
        "INTEGRAL", "FULL_TIME" -> Shift.FULL_TIME
        null, "" -> null
        else -> Shift.OTHER
    }
}
