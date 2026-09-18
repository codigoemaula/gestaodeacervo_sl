package br.gov.sp.sme.salaleitura.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.core.model.Shift

@Entity(tableName = "schools", indices = [Index(value = ["eolCode"], unique = true)])
data class SchoolEntity(
    @PrimaryKey val id: Long = 1,
    val name: String,
    val eolCode: String,
    val dre: String,
    val schoolYear: Int,
    val readingRoomName: String,
    val defaultLoanDays: Int
)

@Entity(tableName = "class_groups", indices = [Index(value = ["name", "schoolYear"], unique = true)])
data class ClassGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val schoolYear: Int,
    val grade: String,
    val shift: Shift,
    val active: Boolean = true
)

@Entity(
    tableName = "people",
    foreignKeys = [ForeignKey(entity = ClassGroupEntity::class, parentColumns = ["id"], childColumns = ["classGroupId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["internalCode"], unique = true), Index(value = ["institutionalId"]), Index(value = ["classGroupId"]), Index(value = ["name"])]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val internalCode: String,
    val name: String,
    val type: PersonType,
    val institutionalId: String? = null,
    val classGroupId: Long? = null,
    val grade: String? = null,
    val shift: Shift? = null,
    val role: String? = null,
    val active: Boolean = true,
    /** Random file basename in private filesDir/reader_photos. Never a public URI. */
    val photoFilename: String? = null
)

@Entity(tableName = "book_editions", indices = [Index(value = ["isbn10"], unique = true), Index(value = ["isbn13"], unique = true), Index(value = ["title"])])
data class BookEditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isbn10: String? = null,
    val isbn13: String? = null,
    val title: String,
    val subtitle: String? = null,
    val authors: String = "",
    val contributors: String? = null,
    val publisher: String? = null,
    val editionLabel: String? = null,
    val publicationYear: Int? = null,
    val language: String? = null,
    val subjects: String? = null,
    val pageCount: Int? = null,
    val series: String? = null,
    val cdd: String? = null,
    val cdu: String? = null,
    val coverUri: String? = null,
    val createdAt: Long
)

@Entity(tableName = "book_copies", foreignKeys = [ForeignKey(entity = BookEditionEntity::class, parentColumns = ["id"], childColumns = ["editionId"], onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["internalCode"], unique = true), Index(value = ["editionId"]), Index(value = ["status"])])
data class BookCopyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val internalCode: String,
    val editionId: Long,
    val sequence: Int,
    val location: String? = null,
    val status: CopyStatus = CopyStatus.AVAILABLE,
    val registeredAt: Long,
    val notes: String? = null
)

@Entity(tableName = "loans", foreignKeys = [
    ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = BookCopyEntity::class, parentColumns = ["id"], childColumns = ["copyId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index(value = ["personId"]), Index(value = ["copyId"]), Index(value = ["returnedAt"]), Index(value = ["dueAt"])])
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val copyId: Long,
    val loanedAt: Long,
    val dueAt: Long,
    val loanPeriodDays: Int,
    val returnedAt: Long? = null,
    val returnStatus: CopyStatus? = null
)

@Entity(tableName = "loan_renewals", foreignKeys = [ForeignKey(entity = LoanEntity::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["loanId"])])
data class LoanRenewalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val renewedAt: Long,
    val periodDays: Int,
    val previousDueAt: Long,
    val newDueAt: Long
)

@Entity(tableName = "inventory_sessions")
data class InventorySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val closedAt: Long? = null,
    val notes: String? = null
)

@Entity(tableName = "inventory_items", foreignKeys = [
    ForeignKey(entity = InventorySessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = BookCopyEntity::class, parentColumns = ["id"], childColumns = ["copyId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index(value = ["sessionId", "copyId"], unique = true), Index(value = ["copyId"])])
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val copyId: Long,
    val scannedAt: Long
)

@Entity(tableName = "metadata_cache")
data class MetadataCacheEntity(@PrimaryKey val isbn13: String, val source: String, val payloadJson: String, val fetchedAt: Long)

@Entity(tableName = "isbn_range_data")
data class IsbnRangeDataEntity(@PrimaryKey val id: Int = 1, val version: String, val payload: String, val updatedAt: Long)

@Entity(tableName = "sync_status")
data class SyncStatusEntity(@PrimaryKey val key: String, val lastAttemptAt: Long? = null, val lastSuccessAt: Long? = null, val status: String, val message: String? = null)

@Entity(tableName = "audit_log", indices = [Index(value = ["timestamp"]), Index(value = ["entityType", "entityId"])])
data class AuditLogEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val timestamp: Long, val action: String, val entityType: String, val entityId: String, val details: String? = null)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(@PrimaryKey val id: Int = 1, val lastMetadataSyncAt: Long? = null, val lastIsbnRangeSyncAt: Long? = null, val backupSchemaVersion: Int = 1)
