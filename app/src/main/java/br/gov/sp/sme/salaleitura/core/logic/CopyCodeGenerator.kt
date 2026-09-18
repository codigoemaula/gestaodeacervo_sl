package br.gov.sp.sme.salaleitura.core.logic

object CopyCodeGenerator {
    fun generate(prefix: String, editionId: Long, sequence: Int): String {
        require(prefix.isNotBlank()) { "Prefixo não pode ser vazio" }
        require(editionId > 0) { "editionId deve ser positivo" }
        require(sequence > 0) { "sequence deve ser positivo" }
        return "%s-%06d-%03d".format(prefix.uppercase(), editionId, sequence)
    }
}
