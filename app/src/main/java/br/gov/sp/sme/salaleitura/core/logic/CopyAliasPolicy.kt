package br.gov.sp.sme.salaleitura.core.logic

object CopyAliasPolicy {
    fun normalize(raw: String): String = raw.trim().uppercase().also { normalized ->
        require(normalized.isNotEmpty()) { "Informe o número de patrimônio" }
        require(normalized.length <= 64) { "Código patrimonial deve ter até 64 caracteres" }
    }
}
