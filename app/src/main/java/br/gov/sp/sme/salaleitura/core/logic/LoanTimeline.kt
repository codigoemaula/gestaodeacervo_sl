package br.gov.sp.sme.salaleitura.core.logic

import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object LoanTimeline {
    fun dueAt(start: Instant, period: LoanPeriod, zoneId: ZoneId): Instant {
        val dueDate = start.atZone(zoneId).toLocalDate().plusDays(period.days)
        return dueDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
    }
}
