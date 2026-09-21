package br.gov.sp.sme.salaleitura.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.gov.sp.sme.salaleitura.data.local.dao.*
import br.gov.sp.sme.salaleitura.data.local.entity.*

@Database(
    entities = [
        SchoolEntity::class, ClassGroupEntity::class, PersonEntity::class,
        BookEditionEntity::class, BookCopyEntity::class, CopyAliasEntity::class, LoanEntity::class,
        LoanRenewalEntity::class, InventorySessionEntity::class, InventoryItemEntity::class,
        MetadataCacheEntity::class, IsbnRangeDataEntity::class, SyncStatusEntity::class,
        AuditLogEntity::class, AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun catalogDao(): CatalogDao
    abstract fun aliasDao(): AliasDao
    abstract fun peopleDao(): PeopleDao
    abstract fun loanDao(): LoanDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun systemDao(): SystemDao

    companion object {
        const val DB_NAME = "sala-leitura.db"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `copy_aliases` (`copyId` INTEGER NOT NULL, `normalizedCode` TEXT NOT NULL, PRIMARY KEY(`copyId`), FOREIGN KEY(`copyId`) REFERENCES `book_copies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_copy_aliases_normalizedCode` ON `copy_aliases` (`normalizedCode`)")
            }
        }
        // Existing loans and people keep their primary keys; photographs are never mandatory.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `people` ADD COLUMN `photoFilename` TEXT")
            }
        }
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }

        fun closeInstance() = synchronized(this) {
            instance?.close()
            instance = null
        }
    }
}
