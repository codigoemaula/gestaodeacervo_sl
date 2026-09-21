package br.gov.sp.sme.salaleitura.feature.people

import java.util.Locale

data class ExistingReader(val id: Long, val internalCode: String, val type: String, val institutionalId: String?)
data class ExistingClass(val id: Long, val name: String, val schoolYear: Int)
data class CsvRowAction(val row: PersonImportRow, val personId: Long?, val classGroupId: Long?)
data class CsvPlan(val inserts: List<CsvRowAction>, val updates: List<CsvRowAction>, val errors: List<String>)

/** No match by name: preserve an existing person's code and circulation history. */
object CsvReconciler {
    private fun norm(value: String?): String? = value?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }

    fun plan(
        rows: List<PersonImportRow>,
        existing: List<ExistingReader>,
        groups: List<ExistingClass>,
        defaultYear: Int
    ): CsvPlan {
        val adds = mutableListOf<CsvRowAction>()
        val updates = mutableListOf<CsvRowAction>()
        val errors = mutableListOf<String>()
        val visitedCodes = mutableSetOf<String>()
        val visitedIds = mutableSetOf<String>()
        val visitedPeople = mutableSetOf<Long>()
        rows.forEach { row ->
            val code = norm(row.internalCode)
            val id = norm(row.institutionalId)
            val type = norm(row.type).orEmpty()
            if (code != null && !visitedCodes.add(code)) errors += "Linha ${row.line}: codigo_interno duplicado no arquivo"
            if (id != null && !visitedIds.add("$type:$id")) errors += "Linha ${row.line}: identificador duplicado no arquivo para $type"

            val byCode = if (code != null) existing.filter { norm(it.internalCode) == code } else emptyList()
            val byId = if (id != null) existing.filter { norm(it.institutionalId) == id && norm(it.type) == type } else emptyList()
            val person = when {
                code != null && byCode.isEmpty() -> { errors += "Linha ${row.line}: codigo_interno desconhecido; revisão obrigatória"; null }
                byCode.size > 1 || byId.size > 1 -> { errors += "Linha ${row.line}: identificador ambíguo"; null }
                byCode.isNotEmpty() && norm(byCode.single().type) != type -> { errors += "Linha ${row.line}: tipo incompatível com codigo_interno"; null }
                byCode.isNotEmpty() && byId.isNotEmpty() && byCode.single().id != byId.single().id -> { errors += "Linha ${row.line}: codigo_interno e identificador apontam pessoas diferentes"; null }
                byCode.isNotEmpty() -> byCode.single()
                byId.isNotEmpty() -> byId.single()
                else -> null
            }
            if (person != null && !visitedPeople.add(person.id)) errors += "Linha ${row.line}: pessoa duplicada no arquivo"
            val matches = row.className?.let { name ->
                groups.filter { it.name.equals(name, ignoreCase = true) && it.schoolYear == (row.schoolYear ?: defaultYear) }
            } ?: emptyList()
            if (row.className != null && matches.size != 1) errors += "Linha ${row.line}: Turma '${row.className}' não encontrada ou ambígua no ano ${row.schoolYear ?: defaultYear}"
            val action = CsvRowAction(row, person?.id, matches.singleOrNull()?.id)
            if (person == null && code == null && byId.isEmpty()) adds += action
            else if (person != null) updates += action
        }
        return CsvPlan(adds, updates, errors.distinct())
    }
}
