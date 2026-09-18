import br.gov.sp.sme.salaleitura.feature.backup.BackupManifest
import br.gov.sp.sme.salaleitura.feature.backup.BackupManifestCodec
import br.gov.sp.sme.salaleitura.feature.backup.Checksum
fun main(){
 val bytes="database".toByteArray();val hash=Checksum.sha256(bytes);check(hash.length==64)
 val m=BackupManifest(1,"123456","EMEF Exemplo",1726570000000,hash)
 val decoded=BackupManifestCodec.decode(BackupManifestCodec.encode(m));check(decoded==m)
 println("Backup manifest smoke tests: PASS")
}
