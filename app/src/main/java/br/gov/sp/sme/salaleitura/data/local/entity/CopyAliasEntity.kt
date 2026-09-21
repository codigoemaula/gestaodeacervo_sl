package br.gov.sp.sme.salaleitura.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Optional existing school barcode bound to one permanently coded physical copy. */
@Entity(
    tableName = "copy_aliases",
    primaryKeys = ["copyId"],
    foreignKeys = [ForeignKey(
        entity = BookCopyEntity::class, parentColumns = ["id"], childColumns = ["copyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["normalizedCode"], unique = true)]
)
data class CopyAliasEntity(val copyId: Long, val normalizedCode: String)
