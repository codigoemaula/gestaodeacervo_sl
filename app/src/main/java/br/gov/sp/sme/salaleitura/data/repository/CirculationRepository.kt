package br.gov.sp.sme.salaleitura.data.repository

import androidx.room.withTransaction
import br.gov.sp.sme.salaleitura.core.logic.LoanPolicy
import br.gov.sp.sme.salaleitura.core.logic.LoanTimeline
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.AuditLogEntity
import br.gov.sp.sme.salaleitura.data.local.entity.LoanEntity
import br.gov.sp.sme.salaleitura.data.local.entity.LoanRenewalEntity
import java.time.Instant
import java.time.ZoneId

class CirculationRepository(
    private val db: AppDatabase,
    private val zoneId: ZoneId = ZoneId.of("America/Sao_Paulo")
) {
    private val people = db.peopleDao()
    private val catalog = db.catalogDao()
    private val loans = db.loanDao()
    private val system = db.systemDao()

    suspend fun checkout(
        personId: Long,
        copyId: Long,
        period: LoanPeriod,
        now: Instant = Instant.now(),
        teacherConfirmedException: Boolean = false
    ): Long = db.withTransaction {
        val person = requireNotNull(people.personById(personId)) { "Pessoa não encontrada" }
        val copy = requireNotNull(catalog.copyById(copyId)) { "Exemplar não encontrado" }
        require(LoanPolicy.canLoan(person.active, copy.status)) { "Empréstimo não permitido" }
        require(loans.activeLoanByCopy(copyId) == null) { "Exemplar já possui empréstimo ativo" }
        val overdue = loans.overdueForPerson(personId, now.toEpochMilli())
        require(LoanPolicy.canCheckoutWithOverdue(overdue.size, teacherConfirmedException)) {
            "Há ${overdue.size} devolução(ões) em atraso. Confirme explicitamente a exceção para continuar."
        }
        val due = LoanTimeline.dueAt(now, period, zoneId)
        val loanId = loans.insertLoan(
            LoanEntity(
                personId = personId,
                copyId = copyId,
                loanedAt = now.toEpochMilli(),
                dueAt = due.toEpochMilli(),
                loanPeriodDays = period.days.toInt()
            )
        )
        catalog.setCopyStatus(copyId, CopyStatus.LOANED)
        system.insertAudit(
            AuditLogEntity(
                timestamp = now.toEpochMilli(), action = "CHECKOUT", entityType = "LOAN", entityId = loanId.toString(),
                details = "period=${period.days};copy=$copyId;person=$personId"
            )
        )
        if (overdue.isNotEmpty()) {
            system.insertAudit(
                AuditLogEntity(
                    timestamp = now.toEpochMilli(), action = "OVERDUE_OVERRIDE", entityType = "LOAN", entityId = loanId.toString(),
                    details = "person=$personId;overdueCount=${overdue.size};teacherConfirmed=true"
                )
            )
        }
        loanId
    }

    suspend fun returnCopy(copyId: Long, returnStatus: CopyStatus = CopyStatus.AVAILABLE, now: Instant = Instant.now()) = db.withTransaction {
        require(returnStatus in setOf(CopyStatus.AVAILABLE, CopyStatus.DAMAGED, CopyStatus.MAINTENANCE, CopyStatus.WITHDRAWN)) { "Situação inválida para devolução" }
        val active = requireNotNull(loans.activeLoanByCopy(copyId)) { "Não há empréstimo ativo para o exemplar" }
        loans.closeLoan(active.id, now.toEpochMilli(), returnStatus)
        catalog.setCopyStatus(copyId, returnStatus)
        system.insertAudit(
            AuditLogEntity(
                timestamp = now.toEpochMilli(), action = "RETURN", entityType = "LOAN", entityId = active.id.toString(),
                details = "copy=$copyId;status=${returnStatus.name}"
            )
        )
    }

    suspend fun renew(loanId: Long, period: LoanPeriod, now: Instant = Instant.now()): Long = db.withTransaction {
        val active = requireNotNull(loans.loanById(loanId)) { "Empréstimo não encontrado" }
        require(active.returnedAt == null) { "Empréstimo já encerrado" }
        val newDue = LoanTimeline.dueAt(now, period, zoneId)
        val renewalId = loans.insertRenewal(
            LoanRenewalEntity(
                loanId = loanId,
                renewedAt = now.toEpochMilli(),
                periodDays = period.days.toInt(),
                previousDueAt = active.dueAt,
                newDueAt = newDue.toEpochMilli()
            )
        )
        loans.updateDueDate(loanId, newDue.toEpochMilli(), period.days.toInt())
        system.insertAudit(
            AuditLogEntity(
                timestamp = now.toEpochMilli(), action = "RENEW", entityType = "LOAN", entityId = loanId.toString(),
                details = "period=${period.days};renewal=$renewalId"
            )
        )
        renewalId
    }
}
