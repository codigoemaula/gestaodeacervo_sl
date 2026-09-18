package br.gov.sp.sme.salaleitura.data.repository

import androidx.room.withTransaction
import br.gov.sp.sme.salaleitura.core.logic.*
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.local.entity.AuditLogEntity
import br.gov.sp.sme.salaleitura.data.local.entity.InventoryItemEntity
import br.gov.sp.sme.salaleitura.data.local.entity.InventorySessionEntity

class InventoryRepository(private val db:AppDatabase){
 private val inventory=db.inventoryDao();private val catalog=db.catalogDao();private val system=db.systemDao()
 suspend fun start(now:Long=System.currentTimeMillis()):Long=db.withTransaction{
  require(inventory.activeSession()==null){"Já existe um inventário em andamento"}
  val id=inventory.insertSession(InventorySessionEntity(startedAt=now));system.insertAudit(AuditLogEntity(timestamp=now,action="START_INVENTORY",entityType="INVENTORY",entityId=id.toString()));id
 }
 suspend fun scan(sessionId:Long,copyCode:String,now:Long=System.currentTimeMillis()):Boolean=db.withTransaction{
  val copy=catalog.copyByCode(copyCode)?:return@withTransaction false
  inventory.insertItem(InventoryItemEntity(sessionId=sessionId,copyId=copy.id,scannedAt=now))>=0
 }
 suspend fun summary(sessionId:Long):InventorySummary{
  val all=catalog.allCopies().map{InventoryCopy(it.id,it.status)};val found=inventory.foundCopyIds(sessionId).toSet();return InventorySummaryCalculator.calculate(all,found)
 }
 suspend fun close(sessionId:Long,now:Long=System.currentTimeMillis()):InventorySummary=db.withTransaction{
  inventory.closeSession(sessionId,now);system.insertAudit(AuditLogEntity(timestamp=now,action="CLOSE_INVENTORY",entityType="INVENTORY",entityId=sessionId.toString()));summary(sessionId)
 }
}
