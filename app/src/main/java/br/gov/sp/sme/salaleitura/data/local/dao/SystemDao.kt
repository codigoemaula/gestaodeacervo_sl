package br.gov.sp.sme.salaleitura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.gov.sp.sme.salaleitura.data.local.entity.*

@Dao
interface SystemDao {
    @Insert suspend fun insertAudit(value: AuditLogEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMetadata(value: MetadataCacheEntity)
    @Query("SELECT * FROM metadata_cache WHERE isbn13=:isbn13") suspend fun metadata(isbn13: String): MetadataCacheEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putIsbnRanges(value: IsbnRangeDataEntity)
    @Query("SELECT * FROM isbn_range_data WHERE id=1") suspend fun isbnRanges(): IsbnRangeDataEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSyncStatus(value: SyncStatusEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSettings(value: AppSettingsEntity)
    @Query("SELECT * FROM app_settings WHERE id=1") suspend fun settings(): AppSettingsEntity?
}
