package br.gov.sp.sme.salaleitura.core.logic
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
object LoanPeriodSelector {
    fun fromDefaultDays(days: Int): LoanPeriod = LoanPeriod.fromDays(days)
    fun toggle(value: LoanPeriod): LoanPeriod = if (value == LoanPeriod.SEVEN) LoanPeriod.FOURTEEN else LoanPeriod.SEVEN
}
