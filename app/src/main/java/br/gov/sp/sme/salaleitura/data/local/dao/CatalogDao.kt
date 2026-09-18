package br.gov.sp.sme.salaleitura.data.local.dao

import androidx.room.*
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.data.local.entity.BookCopyEntity
import br.gov.sp.sme.salaleitura.data.local.entity.BookEditionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Insert suspend fun insertEdition(value: BookEditionEntity): Long
    @Insert suspend fun insertCopies(values: List<BookCopyEntity>): List<Long>
    @Update suspend fun updateCopy(value: BookCopyEntity)
    @Query("SELECT * FROM book_editions WHERE id=:id") suspend fun editionById(id: Long): BookEditionEntity?
    @Query("SELECT * FROM book_editions WHERE isbn13=:isbn LIMIT 1") suspend fun editionByIsbn13(isbn: String): BookEditionEntity?
    @Query("SELECT * FROM book_copies WHERE id=:id") suspend fun copyById(id: Long): BookCopyEntity?
    @Query("SELECT * FROM book_copies WHERE internalCode=:code COLLATE NOCASE LIMIT 1") suspend fun copyByInternalCode(code: String): BookCopyEntity?
    @Query("SELECT * FROM book_copies WHERE internalCode=:code COLLATE NOCASE OR id=(SELECT copyId FROM copy_aliases WHERE normalizedCode=UPPER(TRIM(:code)) LIMIT 1) LIMIT 1") suspend fun copyByCode(code: String): BookCopyEntity?
    @Query("SELECT * FROM book_editions ORDER BY title COLLATE NOCASE") fun observeEditions(): Flow<List<BookEditionEntity>>
    @Query("SELECT * FROM book_copies WHERE editionId=:editionId ORDER BY sequence") fun observeCopies(editionId: Long): Flow<List<BookCopyEntity>>
    @Query("SELECT * FROM book_copies WHERE editionId=:editionId ORDER BY sequence") suspend fun copiesByEdition(editionId: Long): List<BookCopyEntity>
    @Query("SELECT * FROM book_copies ORDER BY id") suspend fun allCopies(): List<BookCopyEntity>
    @Query("SELECT COUNT(*) FROM book_editions") suspend fun editionCount(): Int
    @Query("SELECT COUNT(*) FROM book_copies") suspend fun copyCount(): Int
    @Query("SELECT COUNT(*) FROM book_copies WHERE status=:status") suspend fun countByStatus(status: CopyStatus): Int
    @Query("UPDATE book_copies SET status=:status WHERE id=:copyId") suspend fun setCopyStatus(copyId: Long, status: CopyStatus)
    @Query("SELECT COALESCE(MAX(sequence), 0) FROM book_copies WHERE editionId=:editionId") suspend fun maxSequence(editionId: Long): Int
}
