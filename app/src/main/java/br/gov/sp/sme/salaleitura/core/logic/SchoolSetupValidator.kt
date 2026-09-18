package br.gov.sp.sme.salaleitura.core.logic

data class SchoolSetupData(
    val name: String,
    val eolCode: String,
    val dre: String,
    val schoolYear: Int,
    val readingRoomName: String,
    val defaultLoanDays: Int
)

object SchoolSetupValidator {
    fun validate(data: SchoolSetupData): List<String> = buildList {
        if (data.name.isBlank()) add("Informe o nome da unidade")
        if (data.eolCode.isBlank()) add("Informe o código EOL da escola")
        if (data.dre.isBlank()) add("Informe a DRE")
        if (data.schoolYear !in 2000..2100) add("Informe um ano letivo válido")
        if (data.readingRoomName.isBlank()) add("Informe a identificação da Sala de Leitura")
        if (data.defaultLoanDays !in setOf(7, 14)) add("O prazo padrão deve ser 7 ou 14 dias")
    }
}
