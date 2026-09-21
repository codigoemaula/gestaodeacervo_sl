import br.gov.sp.sme.salaleitura.feature.people.CsvImporter

fun main() {
    val csv = """nome,tipo,identificador,turma,ano,turno,funcao
Ana Silva,ESTUDANTE,123,7A,7,MATUTINO,
"João, Souza",PROFISSIONAL,RF99,,,,Professor
""".trimIndent()
    val result = CsvImporter.parse(csv)
    check(result.errors.isEmpty())
    check(result.rows.size == 2)
    check(result.rows[0].name == "Ana Silva")
    check(result.rows[1].name == "João, Souza")
    check(result.rows[1].role == "Professor")

    val bad = CsvImporter.parse("nome,tipo\nSem Tipo,")
    check(bad.errors.isNotEmpty())
    println("CSV importer smoke tests: PASS")
}
