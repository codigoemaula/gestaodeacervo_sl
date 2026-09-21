import br.gov.sp.sme.salaleitura.core.logic.DatabaseValueCodec
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import br.gov.sp.sme.salaleitura.core.model.PersonType

fun main() {
    check(DatabaseValueCodec.loanPeriodToDays(LoanPeriod.SEVEN) == 7)
    check(DatabaseValueCodec.loanPeriodFromDays(14) == LoanPeriod.FOURTEEN)
    check(DatabaseValueCodec.copyStatusFromValue("DAMAGED") == CopyStatus.DAMAGED)
    check(DatabaseValueCodec.personTypeFromValue("STUDENT") == PersonType.STUDENT)
    try {
        DatabaseValueCodec.loanPeriodFromDays(10)
        error("10 days must be rejected")
    } catch (_: IllegalArgumentException) {}
    println("DatabaseValueCodec smoke tests: PASS")
}
