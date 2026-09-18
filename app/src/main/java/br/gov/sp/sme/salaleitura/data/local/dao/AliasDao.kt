package br.gov.sp.sme.salaleitura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.gov.sp.sme.salaleitura.data.local.entity.CopyAliasEntity

@Dao
interface AliasDao {
    @Query("SELECT * FROM copy_aliases WHERE copyId=:copyId LIMIT 1") suspend fun aliasForCopy(copyId: Long): CopyAliasEntity?
    @Query("SELECT * FROM copy_aliases WHERE normalizedCode=:code LIMIT 1") suspend fun aliasByCode(code: String): CopyAliasEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun add(value: CopyAliasEntity)
    @Query("DELETE FROM copy_aliases WHERE copyId=:copyId") suspend fun removeForCopy(copyId: Long)
}
