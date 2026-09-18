import br.gov.sp.sme.salaleitura.core.logic.CopyCodeGenerator
import br.gov.sp.sme.salaleitura.core.logic.LoanTimeline
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import java.time.Instant
import java.time.ZoneId

fun main() {
    check(CopyCodeGenerator.generate("SL", 123, 1) == "SL-000123-001")
    check(CopyCodeGenerator.generate("SL", 123, 12) == "SL-000123-012")
    val zone = ZoneId.of("America/Sao_Paulo")
    val loanedAt = Instant.parse("2026-09-17T13:00:00Z")
    val due = LoanTimeline.dueAt(loanedAt, LoanPeriod.SEVEN, zone)
    check(due.atZone(zone).toLocalDate().toString() == "2026-09-24")
    check(due.atZone(zone).hour == 23)
    val renewedAt = Instant.parse("2026-09-22T13:00:00Z")
    val renewedDue = LoanTimeline.dueAt(renewedAt, LoanPeriod.FOURTEEN, zone)
    check(renewedDue.atZone(zone).toLocalDate().toString() == "2026-10-06")
    println("Catalog/timeline smoke tests: PASS")
}
