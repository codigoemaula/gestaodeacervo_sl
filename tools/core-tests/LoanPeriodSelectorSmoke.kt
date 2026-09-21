import br.gov.sp.sme.salaleitura.core.logic.LoanPeriodSelector
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod

fun main() {
    check(LoanPeriodSelector.fromDefaultDays(7) == LoanPeriod.SEVEN)
    check(LoanPeriodSelector.fromDefaultDays(14) == LoanPeriod.FOURTEEN)
    try { LoanPeriodSelector.fromDefaultDays(10); error("invalid period accepted") } catch (_: IllegalArgumentException) {}
    check(LoanPeriodSelector.toggle(LoanPeriod.SEVEN) == LoanPeriod.FOURTEEN)
    check(LoanPeriodSelector.toggle(LoanPeriod.FOURTEEN) == LoanPeriod.SEVEN)
    println("Loan period selector smoke tests: PASS")
}
