package br.gov.sp.sme.salaleitura.data.repository

import androidx.room.withTransaction
import br.gov.sp.sme.salaleitura.core.logic.CopyAliasPolicy
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.AuditLogEntity
import br.gov.sp.sme.salaleitura.data.local.entity.CopyAliasEntity

class CopyIdentityRepository(private val db: AppDatabase) {
    suspend fun bindPatrimony(copyId: Long, entered: String): String = db.withTransaction {
        val code = CopyAliasPolicy.normalize(entered)
        val copy = requireNotNull(db.catalogDao().copyById(copyId)) { "Exemplar inexistente" }
        require(copy.internalCode != code) { "Este código já é a identificação interna do exemplar" }
        val collision = db.catalogDao().copyByCode(code)
        require(collision == null || collision.id == copyId) { "Patrimônio já vinculado a outro exemplar" }
        require(db.peopleDao().personByCode(code) == null) { "Código reservado a um leitor" }
        db.aliasDao().removeForCopy(copyId)
        db.aliasDao().add(CopyAliasEntity(copyId, code))
        db.systemDao().insertAudit(AuditLogEntity(System.currentTimeMillis(), "BIND_PATRIMONY", "BOOK_COPY", copyId.toString(), "patrimony=$code"))
        code
    }

    suspend fun unlinkPatrimony(copyId: Long) = db.withTransaction {
        db.aliasDao().removeForCopy(copyId)
        db.systemDao().insertAudit(AuditLogEntity(System.currentTimeMillis(), "UNLINK_PATRIMONY", "BOOK_COPY", copyId.toString(), "schoolCodeUnlinked=true"))
    }
}
