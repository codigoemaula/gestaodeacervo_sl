package br.gov.sp.sme.salaleitura.data.repository

import androidx.room.withTransaction
import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.core.model.Shift
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.ClassGroupEntity
import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import br.gov.sp.sme.salaleitura.feature.people.*
import java.util.UUID

class PeopleRepository(private val db: AppDatabase) {
    private val dao = db.peopleDao()

    suspend fun createClassGroup(value: ClassGroupEntity): Long = dao.insertClassGroup(value.copy(id = 0))

    suspend fun createPerson(value: PersonEntity): Long {
        val code = value.internalCode.ifBlank { "P-${UUID.randomUUID().toString().take(8).uppercase()}" }
        return dao.insertPerson(value.copy(id = 0, internalCode = code))
    }

    suspend fun previewCsv(rows: List<PersonImportRow>): CsvPlan {
        val people = dao.allPeople()
        val groups = dao.allClassGroups()
        val year = db.schoolDao().get()?.schoolYear ?: java.time.Year.now().value
        return CsvReconciler.plan(rows, people.map { it.reader() }, groups.map { ExistingClass(it.id, it.name, it.schoolYear) }, year)
    }

    /** Re-plan under the transaction: a stale preview never overwrites newer records. */
    suspend fun importCsv(rows: List<PersonImportRow>): Pair<Int, Int> = db.withTransaction {
        require(rows.isNotEmpty()) { "Nenhum cadastro válido" }
        val plan = previewCsv(rows)
        require(plan.errors.isEmpty()) { plan.errors.joinToString("\n") }
        val previous = dao.allPeople().associateBy { it.id }
        plan.inserts.forEach { action ->
            val row = action.row
            createPerson(PersonEntity(
                internalCode = "", name = row.name,
                type = row.type.personType(), institutionalId = row.institutionalId,
                classGroupId = action.classGroupId, shift = row.shift.toShift(), role = row.role
            ))
        }
        plan.updates.forEach { action ->
            val original = requireNotNull(previous[action.personId])
            val row = action.row
            dao.updatePerson(original.copy(
                name = row.name,
                institutionalId = row.institutionalId ?: original.institutionalId,
                classGroupId = if (row.className != null) action.classGroupId else original.classGroupId,
                shift = row.shift.toShift() ?: original.shift,
                role = row.role ?: original.role
            ))
        }
        plan.inserts.size to plan.updates.size
    }

    suspend fun exportCsv(): String {
        val groups = dao.allClassGroups().associateBy { it.id }
        val schoolYear = db.schoolDao().get()?.schoolYear
        val rows = dao.allPeople().mapIndexed { index, person ->
            val group = person.classGroupId?.let(groups::get)
            PersonImportRow(
                line = index + 2, name = person.name,
                type = if (person.type == PersonType.STUDENT) "ESTUDANTE" else "PROFISSIONAL",
                institutionalId = person.institutionalId,
                className = group?.name, shift = person.shift?.name,
                role = person.role, internalCode = person.internalCode,
                schoolYear = group?.schoolYear ?: schoolYear
            )
        }
        return CsvExporter.export(rows)
    }

    private fun PersonEntity.reader() = ExistingReader(
        id, internalCode, if (type == PersonType.STUDENT) "ESTUDANTE" else "PROFISSIONAL", institutionalId
    )

    private fun String.personType() = if (this == "ESTUDANTE") PersonType.STUDENT else PersonType.PROFESSIONAL

    private fun String?.toShift(): Shift? = when (this?.trim()?.uppercase()) {
        "MATUTINO", "MANHA", "MANHÃ", "MORNING" -> Shift.MORNING
        "VESPERTINO", "TARDE", "AFTERNOON" -> Shift.AFTERNOON
        "NOTURNO", "NOITE", "EVENING" -> Shift.EVENING
        "INTEGRAL", "FULL_TIME" -> Shift.FULL_TIME
        "OUTRO", "OTHER" -> Shift.OTHER
        null, "" -> null
        else -> Shift.OTHER
    }
}
