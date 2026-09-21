package br.gov.sp.sme.salaleitura.core.logic
import br.gov.sp.sme.salaleitura.core.model.CopyStatus

data class InventoryCopy(val id: Long, val status: CopyStatus)
data class InventorySummary(val registered:Int,val found:Int,val loaned:Int,val missing:Int,val damaged:Int,val withdrawn:Int)
object InventorySummaryCalculator{
 fun calculate(copies:List<InventoryCopy>,foundIds:Set<Long>):InventorySummary{
  val loaned=copies.count{it.status==CopyStatus.LOANED}; val damaged=copies.count{it.status==CopyStatus.DAMAGED}; val withdrawn=copies.count{it.status==CopyStatus.WITHDRAWN}
  val found=copies.count{it.id in foundIds}
  val missing=copies.count{it.id !in foundIds && it.status !in setOf(CopyStatus.LOANED,CopyStatus.WITHDRAWN)}
  return InventorySummary(copies.size,found,loaned,missing,damaged,withdrawn)
 }
}
