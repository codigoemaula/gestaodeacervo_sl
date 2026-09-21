import br.gov.sp.sme.salaleitura.core.logic.InventoryCopy
import br.gov.sp.sme.salaleitura.core.logic.InventorySummaryCalculator
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
fun main(){
 val copies=listOf(
  InventoryCopy(1,CopyStatus.AVAILABLE),InventoryCopy(2,CopyStatus.LOANED),InventoryCopy(3,CopyStatus.DAMAGED),
  InventoryCopy(4,CopyStatus.WITHDRAWN),InventoryCopy(5,CopyStatus.AVAILABLE)
 )
 val s=InventorySummaryCalculator.calculate(copies,setOf(1,3,4))
 check(s.registered==5);check(s.found==3);check(s.loaned==1);check(s.damaged==1);check(s.withdrawn==1);check(s.missing==1)
 println("Inventory summary smoke tests: PASS")
}
