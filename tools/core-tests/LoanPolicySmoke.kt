import br.gov.sp.sme.salaleitura.core.logic.LoanPolicy
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import java.time.LocalDate

fun main() {
    check(LoanPolicy.dueDate(LocalDate.of(2026, 9, 17), LoanPeriod.SEVEN) == LocalDate.of(2026, 9, 24))
    check(LoanPolicy.dueDate(LocalDate.of(2026, 9, 17), LoanPeriod.FOURTEEN) == LocalDate.of(2026, 10, 1))
    check(!LoanPolicy.canLoan(false, CopyStatus.AVAILABLE))
    check(!LoanPolicy.canLoan(true, CopyStatus.LOANED))
    check(LoanPolicy.canLoan(true, CopyStatus.AVAILABLE))
    println("LoanPolicy smoke tests: PASS")
}
