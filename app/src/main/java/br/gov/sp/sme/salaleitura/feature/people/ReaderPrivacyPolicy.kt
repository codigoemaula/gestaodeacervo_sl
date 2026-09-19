package br.gov.sp.sme.salaleitura.feature.people

import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import java.util.UUID

object ReaderPrivacyPolicy {
    fun anonymize(person: PersonEntity, hasOpenLoans: Boolean): PersonEntity {
        require(!hasOpenLoans) { "Devolva todos os exemplares antes de remover os dados do leitor." }
        require(person.active) { "Cadastro já removido." }
        return person.copy(
            internalCode = "ANON-${UUID.randomUUID()}",
            name = "Leitor anonimizado",
            institutionalId = null,
            classGroupId = null,
            grade = null,
            shift = null,
            role = null,
            photoFilename = null,
            active = false
        )
    }
}
