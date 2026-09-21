package br.gov.sp.sme.salaleitura.core.logic

sealed interface IsbnResult {
    data class Valid(val isbn13: String, val isbn10: String? = null) : IsbnResult
    data class Invalid(val reason: String) : IsbnResult
}

object Isbn {
    fun normalize(raw: String): IsbnResult {
        val cleaned = raw.uppercase()
            .replace("ISBN", "")
            .filter { it.isDigit() || it == 'X' }
        return when (cleaned.length) {
            10 -> normalize10(cleaned)
            13 -> normalize13(cleaned)
            else -> IsbnResult.Invalid("ISBN deve conter 10 ou 13 caracteres")
        }
    }

    private fun normalize10(value: String): IsbnResult {
        if (!value.take(9).all(Char::isDigit) || !(value.last().isDigit() || value.last() == 'X')) {
            return IsbnResult.Invalid("ISBN-10 contém caracteres inválidos")
        }
        val sum = value.mapIndexed { index, c ->
            val digit = if (c == 'X') 10 else c.digitToInt()
            (10 - index) * digit
        }.sum()
        if (sum % 11 != 0) return IsbnResult.Invalid("Dígito verificador ISBN-10 inválido")
        val base = "978${value.take(9)}"
        val check = checkDigit13(base)
        return IsbnResult.Valid(isbn13 = base + check, isbn10 = value)
    }

    private fun normalize13(value: String): IsbnResult {
        if (!value.all(Char::isDigit)) return IsbnResult.Invalid("ISBN-13 contém caracteres inválidos")
        if (!value.startsWith("978") && !value.startsWith("979")) return IsbnResult.Invalid("Prefixo ISBN-13 deve ser 978 ou 979")
        if (checkDigit13(value.take(12)) != value.last().digitToInt()) return IsbnResult.Invalid("Dígito verificador ISBN-13 inválido")
        val equivalent10 = if (value.startsWith("978")) {
            val first9 = value.substring(3, 12)
            val weighted = first9.mapIndexed { index, digit -> digit.digitToInt() * (10 - index) }.sum()
            val check = (11 - weighted % 11) % 11
            first9 + if (check == 10) "X" else check.toString()
        } else null
        return IsbnResult.Valid(isbn13 = value, isbn10 = equivalent10)
    }

    private fun checkDigit13(first12: String): Int {
        val sum = first12.mapIndexed { index, c -> c.digitToInt() * if (index % 2 == 0) 1 else 3 }.sum()
        return (10 - (sum % 10)) % 10
    }
}
