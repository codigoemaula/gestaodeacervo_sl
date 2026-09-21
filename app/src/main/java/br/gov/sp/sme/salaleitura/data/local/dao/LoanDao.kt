package br.gov.sp.sme.salaleitura.data.local.dao

import androidx.room.*
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.data.local.entity.LoanEntity
import br.gov.sp.sme.salaleitura.data.local.entity.LoanRenewalEntity
import kotlinx.coroutines.flow.Flow

data class ActiveLoanRow(
    val loanId: Long, val personName: String, val personCode: String,
    val copyCode: String, val bookTitle: String, val loanedAt: Long, val dueAt: Long, val loanPeriodDays: Int
)

@Dao
interface LoanDao {
    @Insert suspend fun insertLoan(value: LoanEntity): Long
    @Insert suspend fun insertRenewal(value: LoanRenewalEntity): Long
    @Update suspend fun updateLoan(value: LoanEntity)
    @Query("SELECT * FROM loans WHERE id=:id") suspend fun loanById(id: Long): LoanEntity?
    @Query("SELECT * FROM loans WHERE copyId=:copyId AND returnedAt IS NULL ORDER BY loanedAt DESC LIMIT 1") suspend fun activeLoanByCopy(copyId: Long): LoanEntity?
    @Query("SELECT * FROM loans WHERE returnedAt IS NULL ORDER BY dueAt") fun observeActiveLoans(): Flow<List<LoanEntity>>
    @Query("""SELECT l.id AS loanId, p.name AS personName, p.internalCode AS personCode, c.internalCode AS copyCode, e.title AS bookTitle, l.loanedAt AS loanedAt, l.dueAt AS dueAt, l.loanPeriodDays AS loanPeriodDays FROM loans l JOIN people p ON p.id=l.personId JOIN book_copies c ON c.id=l.copyId JOIN book_editions e ON e.id=c.editionId WHERE l.returnedAt IS NULL ORDER BY l.dueAt""") fun observeActiveLoanRows(): Flow<List<ActiveLoanRow>>
    @Query("""SELECT l.id AS loanId, p.name AS personName, p.internalCode AS personCode, c.internalCode AS copyCode, e.title AS bookTitle, l.loanedAt AS loanedAt, l.dueAt AS dueAt, l.loanPeriodDays AS loanPeriodDays FROM loans l JOIN people p ON p.id=l.personId JOIN book_copies c ON c.id=l.copyId JOIN book_editions e ON e.id=c.editionId WHERE l.personId=:personId AND l.returnedAt IS NULL AND l.dueAt<:now ORDER BY l.dueAt""") suspend fun overdueForPerson(personId: Long, now: Long): List<ActiveLoanRow>
    @Query("SELECT COUNT(*) FROM loans WHERE returnedAt IS NULL") suspend fun activeCount(): Int
    @Query("SELECT COUNT(*) FROM loans WHERE returnedAt IS NULL AND dueAt < :now") suspend fun overdueCount(now: Long): Int
    @Query("SELECT COUNT(*) FROM loans WHERE returnedAt IS NULL AND dueAt BETWEEN :start AND :end") suspend fun dueBetween(start: Long, end: Long): Int
    @Query("SELECT COUNT(*) FROM loans WHERE returnedAt BETWEEN :start AND :end") suspend fun returnedBetween(start: Long, end: Long): Int
    @Query("UPDATE loans SET dueAt=:dueAt, loanPeriodDays=:periodDays WHERE id=:loanId") suspend fun updateDueDate(loanId: Long, dueAt: Long, periodDays: Int)
    @Query("UPDATE loans SET returnedAt=:returnedAt, returnStatus=:returnStatus WHERE id=:loanId") suspend fun closeLoan(loanId: Long, returnedAt: Long, returnStatus: CopyStatus)
}
