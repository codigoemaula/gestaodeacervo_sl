package br.gov.sp.sme.salaleitura.data.local.dao

import androidx.room.*
import br.gov.sp.sme.salaleitura.data.local.entity.InventoryItemEntity
import br.gov.sp.sme.salaleitura.data.local.entity.InventorySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Insert suspend fun insertSession(value: InventorySessionEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertItem(value: InventoryItemEntity): Long
    @Query("SELECT * FROM inventory_sessions WHERE closedAt IS NULL ORDER BY startedAt DESC LIMIT 1") suspend fun activeSession(): InventorySessionEntity?
    @Query("SELECT * FROM inventory_sessions ORDER BY startedAt DESC") fun observeSessions(): Flow<List<InventorySessionEntity>>
    @Query("SELECT COUNT(*) FROM inventory_items WHERE sessionId=:sessionId") suspend fun foundCount(sessionId: Long): Int
    @Query("SELECT copyId FROM inventory_items WHERE sessionId=:sessionId") suspend fun foundCopyIds(sessionId: Long): List<Long>
    @Query("UPDATE inventory_sessions SET closedAt=:closedAt WHERE id=:sessionId") suspend fun closeSession(sessionId: Long, closedAt: Long)
    @Query("SELECT * FROM inventory_sessions WHERE closedAt IS NOT NULL ORDER BY closedAt DESC LIMIT 1") suspend fun latestClosed(): InventorySessionEntity?
}
