import br.gov.sp.sme.salaleitura.core.logic.Isbn
import br.gov.sp.sme.salaleitura.core.logic.IsbnResult

fun main() {
    val from13 = Isbn.normalize("ISBN 978-0-306-40615-7")
    check(from13 is IsbnResult.Valid && from13.isbn13 == "9780306406157")

    val from10 = Isbn.normalize("0-306-40615-2")
    check(from10 is IsbnResult.Valid && from10.isbn10 == "0306406152" && from10.isbn13 == "9780306406157")

    check(Isbn.normalize("9780306406158") is IsbnResult.Invalid)
    check(Isbn.normalize("123") is IsbnResult.Invalid)
    println("ISBN smoke tests: PASS")
}
