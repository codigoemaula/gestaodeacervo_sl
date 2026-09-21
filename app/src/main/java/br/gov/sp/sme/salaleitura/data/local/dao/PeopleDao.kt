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
    @Delete suspend fun deletePerson(value: PersonEntity)
    @Query("SELECT * FROM class_groups WHERE active=1 ORDER BY name") fun observeClassGroups(): Flow<List<ClassGroupEntity>>
    @Query("SELECT * FROM people WHERE active=1 ORDER BY name COLLATE NOCASE") fun observePeople(): Flow<List<PersonEntity>>
    @Query("SELECT * FROM people WHERE active=1 ORDER BY name COLLATE NOCASE") suspend fun allPeople(): List<PersonEntity>
    @Query("SELECT * FROM class_groups ORDER BY schoolYear DESC, name") suspend fun allClassGroups(): List<ClassGroupEntity>
    @Query("SELECT * FROM people WHERE id=:id") suspend fun personById(id: Long): PersonEntity?
    @Query("SELECT * FROM people WHERE internalCode=:code AND active=1 LIMIT 1") suspend fun personByCode(code: String): PersonEntity?
    @Query("SELECT * FROM people WHERE active=1 AND name LIKE '%' || :query || '%' ORDER BY name LIMIT 50") suspend fun search(query: String): List<PersonEntity>
    @Query("SELECT COUNT(*) FROM loans WHERE personId=:id AND returnedAt IS NULL") suspend fun activeLoansForPerson(id: Long): Int
    @Query("SELECT COUNT(*) FROM loans WHERE personId=:id") suspend fun historyForPerson(id: Long): Int
}
