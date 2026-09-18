package br.gov.sp.sme.salaleitura.data.local.dao

import androidx.room.*
import br.gov.sp.sme.salaleitura.data.local.entity.ClassGroupEntity
import br.gov.sp.sme.salaleitura.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeopleDao {
    @Insert suspend fun insertClassGroup(value: ClassGroupEntity): Long
    @Insert suspend fun insertPerson(value: PersonEntity): Long
    @Insert suspend fun insertPeople(values: List<PersonEntity>): List<Long>
    @Update suspend fun updatePerson(value: PersonEntity)
    @Query("SELECT * FROM class_groups WHERE active=1 ORDER BY name") fun observeClassGroups(): Flow<List<ClassGroupEntity>>
    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE") fun observePeople(): Flow<List<PersonEntity>>
    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE") suspend fun allPeople(): List<PersonEntity>
    @Query("SELECT * FROM class_groups ORDER BY schoolYear DESC, name") suspend fun allClassGroups(): List<ClassGroupEntity>
    @Query("SELECT * FROM people WHERE id=:id") suspend fun personById(id: Long): PersonEntity?
    @Query("SELECT * FROM people WHERE internalCode=:code LIMIT 1") suspend fun personByCode(code: String): PersonEntity?
    @Query("SELECT * FROM people WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT 50") suspend fun search(query: String): List<PersonEntity>
}
