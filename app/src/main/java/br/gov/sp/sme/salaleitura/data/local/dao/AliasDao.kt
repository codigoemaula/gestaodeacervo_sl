package br.gov.sp.sme.salaleitura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.gov.sp.sme.salaleitura.data.local.entity.CopyAliasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AliasDao {
    @Query("SELECT * FROM copy_aliases WHERE copyId=:copyId LIMIT 1") suspend fun aliasForCopy(copyId: Long): CopyAliasEntity?
    @Query("SELECT * FROM copy_aliases WHERE normalizedCode=:code LIMIT 1") suspend fun aliasByCode(code: String): CopyAliasEntity?
    @Query("SELECT a.* FROM copy_aliases a JOIN book_copies c ON c.id=a.copyId WHERE c.editionId=:editionId") fun observeForEdition(editionId: Long): Flow<List<CopyAliasEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun add(value: CopyAliasEntity)
    @Query("DELETE FROM copy_aliases WHERE copyId=:copyId") suspend fun removeForCopy(copyId: Long)
}
