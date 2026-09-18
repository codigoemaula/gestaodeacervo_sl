package br.gov.sp.sme.salaleitura.feature.people

data class PersonImportRow(
    val line: Int,
    val name: String,
    val type: String,
    val institutionalId: String? = null,
    val className: String? = null,
    val grade: String? = null,
    val shift: String? = null,
    val role: String? = null
)

data class CsvImportResult(val rows: List<PersonImportRow>, val errors: List<String>)

object CsvImporter {
    private val required = setOf("nome", "tipo")

    fun parse(text: String): CsvImportResult {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return CsvImportResult(emptyList(), listOf("Arquivo CSV vazio"))
        val headers = parseLine(lines.first()).map { normalizeHeader(it) }
        val missing = required - headers.toSet()
        if (missing.isNotEmpty()) return CsvImportResult(emptyList(), listOf("Cabeçalhos obrigatórios ausentes: ${missing.joinToString()}"))
        val rows = mutableListOf<PersonImportRow>(); val errors = mutableListOf<String>()
        lines.drop(1).forEachIndexed { index, raw ->
            val lineNumber = index + 2
            val values = parseLine(raw)
            fun v(name: String): String? = headers.indexOf(name).takeIf { it >= 0 }?.let { values.getOrNull(it)?.trim() }?.takeIf(String::isNotBlank)
            val name = v("nome").orEmpty(); val type = v("tipo").orEmpty().uppercase()
            if (name.isBlank()) errors += "Linha $lineNumber: nome vazio"
            if (type !in setOf("ESTUDANTE", "STUDENT", "PROFISSIONAL", "PROFESSIONAL")) errors += "Linha $lineNumber: tipo deve ser ESTUDANTE ou PROFISSIONAL"
            if (name.isNotBlank() && type in setOf("ESTUDANTE", "STUDENT", "PROFISSIONAL", "PROFESSIONAL")) {
                rows += PersonImportRow(lineNumber, name, type, v("identificador"), v("turma"), v("ano"), v("turno"), v("funcao") ?: v("função"))
            }
        }
        return CsvImportResult(rows, errors)
    }

    private fun normalizeHeader(value: String): String = value.trim().lowercase()

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>(); val current = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { current.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { result += current.toString(); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        result += current.toString()
        return result
    }
}
