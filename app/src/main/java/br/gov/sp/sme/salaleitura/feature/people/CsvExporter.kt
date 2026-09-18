package br.gov.sp.sme.salaleitura.feature.people

/** CSV safe for spreadsheets; a leading tab neutralizes formula triggers and is stripped on import. */
object CsvExporter {
    fun template(): String = export(
        listOf(
            PersonImportRow(2, "Ana Silva", "ESTUDANTE", "123456", "7A", shift = "MANHÃ", schoolYear = 2026),
            PersonImportRow(3, "João Souza", "PROFISSIONAL", "RF123456", role = "Professor", schoolYear = 2026)
        )
    )

    fun export(rows: List<PersonImportRow>, delimiter: Char = ','): String {
        require(delimiter == ',' || delimiter == ';') { "Delimitador não suportado" }
        fun protect(text: String): String = if (text.firstOrNull() in listOf('=', '+', '-', '@')) "\t$text" else text
        fun quote(value: String): String {
            val content = protect(value).replace("\"", "\"\"")
            return if (content.any { it == delimiter || it == '\n' || it == '\r' || it == '"' || it == '\t' }) "\"$content\"" else content
        }
        val lines = rows.map { row ->
            listOf(
                row.name, row.type, row.institutionalId.orEmpty(), row.className.orEmpty(),
                row.schoolYear?.toString().orEmpty(), row.shift.orEmpty(), row.role.orEmpty(), row.internalCode.orEmpty()
            ).joinToString(delimiter.toString(), transform = ::quote)
        }
        return (listOf(CsvImporter.HEADERS.joinToString(delimiter.toString())) + lines).joinToString("\r\n") + "\r\n"
    }
}
