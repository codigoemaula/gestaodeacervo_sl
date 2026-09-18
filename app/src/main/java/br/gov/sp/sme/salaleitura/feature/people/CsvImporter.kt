package br.gov.sp.sme.salaleitura.feature.people

import java.util.Locale

data class PersonImportRow(
    val line: Int,
    val name: String,
    val type: String,
    val institutionalId: String? = null,
    val className: String? = null,
    val grade: String? = null,
    val shift: String? = null,
    val role: String? = null,
    val internalCode: String? = null,
    val schoolYear: Int? = null
)

data class CsvImportResult(val rows: List<PersonImportRow>, val errors: List<String>)

/** Import/export format is independent of Android so it can be regression-tested without a device. */
object CsvImporter {
    val HEADERS = listOf("nome", "tipo", "identificador", "turma", "ano", "turno", "funcao", "codigo_interno")
    private val required = setOf("nome", "tipo")

    fun parse(text: String): CsvImportResult {
        val clean = text.removePrefix("\uFEFF")
        if (clean.isBlank()) return CsvImportResult(emptyList(), listOf("Arquivo CSV vazio"))
        val delimiter = detectDelimiter(clean)
        val result = readRecords(clean, delimiter)
        if (result.errors.isNotEmpty()) return CsvImportResult(emptyList(), result.errors)
        val records = result.rows.filter { row -> row.cells.any { it.isNotBlank() } }
        if (records.isEmpty()) return CsvImportResult(emptyList(), listOf("Arquivo CSV vazio"))
        val headers = records.first().cells.map { it.trim().lowercase(Locale.ROOT).removePrefix("\uFEFF") }
        val missing = required - headers.toSet()
        if (missing.isNotEmpty()) return CsvImportResult(emptyList(), listOf("Cabeçalhos obrigatórios ausentes: ${missing.joinToString()}"))
        if (headers.size != headers.toSet().size) return CsvImportResult(emptyList(), listOf("Cabeçalhos duplicados"))
        val unknown = headers - HEADERS.toSet() - setOf("função")
        if (unknown.isNotEmpty()) return CsvImportResult(emptyList(), listOf("Cabeçalhos desconhecidos: ${unknown.joinToString()}"))

        val rows = mutableListOf<PersonImportRow>()
        val errors = mutableListOf<String>()
        records.drop(1).forEach { record ->
            if (record.cells.size != headers.size) {
                errors += "Linha ${record.line}: esperadas ${headers.size} colunas; encontradas ${record.cells.size}"
                return@forEach
            }
            fun field(name: String): String? = headers.indexOf(name).takeIf { it >= 0 }
                ?.let { record.cells[it].trim().unprotectFormula().takeIf(String::isNotBlank) }
            val name = field("nome").orEmpty()
            val rawType = field("tipo")?.uppercase(Locale.ROOT).orEmpty()
            val type = when (rawType) {
                "ESTUDANTE", "STUDENT" -> "ESTUDANTE"
                "PROFISSIONAL", "PROFESSIONAL" -> "PROFISSIONAL"
                else -> null
            }
            val year = field("ano")
            val parsedYear = year?.toIntOrNull()
            val code = field("codigo_interno")
            if (name.isBlank()) errors += "Linha ${record.line}: nome vazio"
            if (type == null) errors += "Linha ${record.line}: tipo deve ser ESTUDANTE ou PROFISSIONAL"
            if (year != null && (parsedYear == null || parsedYear !in 1900..2100)) errors += "Linha ${record.line}: ano letivo inválido"
            if (code != null && !code.matches(Regex("P-[A-Za-z0-9-]{4,64}"))) errors += "Linha ${record.line}: codigo_interno inválido"
            if (name.isNotBlank() && type != null && (year == null || parsedYear in 1900..2100) && (code == null || code.matches(Regex("P-[A-Za-z0-9-]{4,64}")))) {
                rows += PersonImportRow(
                    line = record.line, name = name, type = type,
                    institutionalId = field("identificador"), className = field("turma"),
                    shift = field("turno"), role = field("funcao") ?: field("função"),
                    internalCode = code, schoolYear = parsedYear
                )
            }
        }
        return CsvImportResult(rows, errors)
    }

    private fun String.unprotectFormula(): String =
        if (length > 1 && first() == '\t' && get(1) in "=+-@") drop(1) else this

    private fun detectDelimiter(input: String): Char {
        var quoted = false
        var comma = 0
        var semicolon = 0
        var index = 0
        while (index < input.length) {
            when (input[index]) {
                '"' -> if (quoted && index + 1 < input.length && input[index + 1] == '"') index++ else quoted = !quoted
                '\n', '\r' -> if (!quoted) break
                ',' -> if (!quoted) comma++
                ';' -> if (!quoted) semicolon++
            }
            index++
        }
        return if (semicolon > comma) ';' else ','
    }

    private data class Record(val line: Int, val cells: List<String>)
    private data class Records(val rows: List<Record>, val errors: List<String>)

    private fun readRecords(input: String, delimiter: Char): Records {
        val rows = mutableListOf<Record>()
        val errors = mutableListOf<String>()
        val cells = mutableListOf<String>()
        val buffer = StringBuilder()
        var line = 1
        var rowStart = 1
        var quoted = false
        var closedQuote = false
        var index = 0
        fun finishCell() { cells += buffer.toString(); buffer.clear(); closedQuote = false }
        fun finishRow() { finishCell(); rows += Record(rowStart, cells.toList()); cells.clear(); rowStart = line + 1 }
        while (index < input.length) {
            val c = input[index]
            when {
                quoted && c == '"' && index + 1 < input.length && input[index + 1] == '"' -> { buffer.append('"'); index++ }
                quoted && c == '"' -> { quoted = false; closedQuote = true }
                quoted && c == '\r' -> { buffer.append('\n'); if (index + 1 < input.length && input[index + 1] == '\n') index++; line++ }
                quoted && c == '\n' -> { buffer.append('\n'); line++ }
                quoted -> buffer.append(c)
                c == '"' && buffer.isEmpty() && !closedQuote -> quoted = true
                c == '"' -> errors += "Linha $line: aspas fora de um campo entre aspas"
                c == delimiter -> finishCell()
                c == '\r' || c == '\n' -> {
                    if (c == '\r' && index + 1 < input.length && input[index + 1] == '\n') index++
                    finishRow(); line++
                }
                closedQuote && !c.isWhitespace() -> errors += "Linha $line: texto após fechar aspas"
                closedQuote -> Unit
                else -> buffer.append(c)
            }
            index++
        }
        if (quoted) errors += "Linha $rowStart: campo com aspas não fechado"
        if (cells.isNotEmpty() || buffer.isNotEmpty() || closedQuote) { finishCell(); rows += Record(rowStart, cells.toList()) }
        return Records(rows, errors)
    }
}
