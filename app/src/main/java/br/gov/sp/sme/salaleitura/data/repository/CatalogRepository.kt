package br.gov.sp.sme.salaleitura.data.repository

import androidx.room.withTransaction
import br.gov.sp.sme.salaleitura.core.logic.CopyCodeGenerator
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.AuditLogEntity
import br.gov.sp.sme.salaleitura.data.local.entity.BookCopyEntity
import br.gov.sp.sme.salaleitura.data.local.entity.BookEditionEntity

class CatalogRepository(private val db: AppDatabase) {
    private val catalog = db.catalogDao()
    private val system = db.systemDao()

    suspend fun createEditionWithCopies(
        edition: BookEditionEntity,
        quantity: Int,
        location: String?,
        now: Long,
        prefix: String = "SL"
    ): Long = db.withTransaction {
        require(quantity > 0) { "Quantidade deve ser maior que zero" }
        val editionId = catalog.insertEdition(edition.copy(id = 0))
        val copies = (1..quantity).map { sequence ->
            BookCopyEntity(
                internalCode = CopyCodeGenerator.generate(prefix, editionId, sequence),
                editionId = editionId,
                sequence = sequence,
                location = location,
                status = CopyStatus.AVAILABLE,
                registeredAt = now
            )
        }
        catalog.insertCopies(copies)
        system.insertAudit(AuditLogEntity(timestamp = now, action = "CREATE_EDITION", entityType = "BOOK_EDITION", entityId = editionId.toString(), details = "copies=$quantity"))
        editionId
    }

    suspend fun addCopies(editionId: Long, quantity: Int, location: String?, now: Long, prefix: String = "SL"): List<Long> = db.withTransaction {
        require(quantity > 0) { "Quantidade deve ser maior que zero" }
        requireNotNull(catalog.editionById(editionId)) { "Obra/edição não encontrada" }
        val start = catalog.maxSequence(editionId) + 1
        val copies = (start until start + quantity).map { sequence ->
            BookCopyEntity(
                internalCode = CopyCodeGenerator.generate(prefix, editionId, sequence),
                editionId = editionId,
                sequence = sequence,
                location = location,
                registeredAt = now
            )
        }
        val ids = catalog.insertCopies(copies)
        system.insertAudit(AuditLogEntity(timestamp = now, action = "ADD_COPIES", entityType = "BOOK_EDITION", entityId = editionId.toString(), details = "copies=$quantity"))
        ids
    }
}
