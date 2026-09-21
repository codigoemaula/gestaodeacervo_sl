import br.gov.sp.sme.salaleitura.core.logic.SchoolSetupData
import br.gov.sp.sme.salaleitura.core.logic.SchoolSetupValidator

fun main() {
    val ok = SchoolSetupValidator.validate(SchoolSetupData("EMEF Exemplo", "123456", "DRE", 2026, "Sala de Leitura", 7))
    check(ok.isEmpty())
    val bad = SchoolSetupValidator.validate(SchoolSetupData("", "", "", 1900, "", 10))
    check(bad.size >= 5)
    check(bad.any { it.contains("7 ou 14") })
    println("School setup validator smoke tests: PASS")
}
