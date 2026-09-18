package br.gov.sp.sme.salaleitura.core.model

enum class LoanPeriod(val days: Long) {
    SEVEN(7),
    FOURTEEN(14);

    companion object {
        fun fromDays(days: Int): LoanPeriod = when (days) {
            7 -> SEVEN
            14 -> FOURTEEN
            else -> throw IllegalArgumentException("Prazo de empréstimo deve ser 7 ou 14 dias")
        }
    }
}

enum class CopyStatus {
    AVAILABLE,
    LOANED,
    DAMAGED,
    LOST,
    MAINTENANCE,
    WITHDRAWN
}

enum class PersonType { STUDENT, PROFESSIONAL }

enum class Shift { MORNING, AFTERNOON, EVENING, FULL_TIME, OTHER }
