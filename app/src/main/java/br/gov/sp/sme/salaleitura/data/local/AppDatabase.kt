package br.gov.sp.sme.salaleitura.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.gov.sp.sme.salaleitura.data.local.dao.*
import br.gov.sp.sme.salaleitura.data.local.entity.*

@Database(
    entities = [
        SchoolEntity::class, ClassGroupEntity::class, PersonEntity::class,
        BookEditionEntity::class, BookCopyEntity::class, LoanEntity::class,
        LoanRenewalEntity::class, InventorySessionEntity::class, InventoryItemEntity::class,
        MetadataCacheEntity::class, IsbnRangeDataEntity::class, SyncStatusEntity::class,
        AuditLogEntity::class, AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun catalogDao(): CatalogDao
    abstract fun peopleDao(): PeopleDao
    abstract fun loanDao(): LoanDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun systemDao(): SystemDao

    companion object {
        const val DB_NAME = "sala-leitura.db"
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            ).build().also { instance = it }
        }

        fun closeInstance() = synchronized(this) {
            instance?.close()
            instance = null
        }
    }
}
