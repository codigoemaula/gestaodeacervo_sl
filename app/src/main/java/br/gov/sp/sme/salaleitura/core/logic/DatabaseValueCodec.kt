package br.gov.sp.sme.salaleitura.core.logic

import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.core.model.Shift

object DatabaseValueCodec {
    fun loanPeriodToDays(value: LoanPeriod): Int = value.days.toInt()
    fun loanPeriodFromDays(value: Int): LoanPeriod = LoanPeriod.fromDays(value)
    fun copyStatusToValue(value: CopyStatus): String = value.name
    fun copyStatusFromValue(value: String): CopyStatus = CopyStatus.valueOf(value)
    fun personTypeToValue(value: PersonType): String = value.name
    fun personTypeFromValue(value: String): PersonType = PersonType.valueOf(value)
    fun shiftToValue(value: Shift): String = value.name
    fun shiftFromValue(value: String): Shift = Shift.valueOf(value)
}
