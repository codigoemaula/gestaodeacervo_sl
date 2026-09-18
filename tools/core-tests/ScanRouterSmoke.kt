import br.gov.sp.sme.salaleitura.feature.scanner.ScanResult
import br.gov.sp.sme.salaleitura.feature.scanner.ScanRouter
fun main(){
 check(ScanRouter.route("9780306406157") is ScanResult.Isbn)
 check(ScanRouter.route("SL:SL-000123-001") == ScanResult.Copy("SL-000123-001"))
 check(ScanRouter.route("PERSON:P-ABCD1234") == ScanResult.Person("P-ABCD1234"))
 check(ScanRouter.route("SL-000123-001") == ScanResult.Copy("SL-000123-001"))
 check(ScanRouter.route("nada") is ScanResult.Unknown)
 println("Scan router smoke tests: PASS")
}
