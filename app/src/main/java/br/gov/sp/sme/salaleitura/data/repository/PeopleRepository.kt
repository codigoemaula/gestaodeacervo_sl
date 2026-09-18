package br.gov.sp.sme.salaleitura.data.repository

import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.ClassGroupEntity
import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import java.util.UUID

class PeopleRepository(private val db: AppDatabase) {
    private val dao = db.peopleDao()

    suspend fun createClassGroup(value: ClassGroupEntity): Long = dao.insertClassGroup(value.copy(id = 0))

    suspend fun createPerson(value: PersonEntity): Long {
        val code = value.internalCode.ifBlank { "P-${UUID.randomUUID().toString().take(8).uppercase()}" }
        return dao.insertPerson(value.copy(id = 0, internalCode = code))
    }
}
