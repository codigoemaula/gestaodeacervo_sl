package br.gov.sp.sme.salaleitura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.gov.sp.sme.salaleitura.data.local.entity.SchoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools WHERE id = 1") fun observe(): Flow<SchoolEntity?>
    @Query("SELECT * FROM schools WHERE id = 1") suspend fun get(): SchoolEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(school: SchoolEntity)
}
