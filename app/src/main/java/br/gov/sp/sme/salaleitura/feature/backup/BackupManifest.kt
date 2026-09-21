package br.gov.sp.sme.salaleitura.feature.backup

import java.io.StringReader
import java.io.StringWriter
import java.security.MessageDigest
import java.util.Properties

data class BackupManifest(val schemaVersion:Int,val schoolEol:String,val schoolName:String,val createdAt:Long,val sha256:String)
object Checksum{
 fun sha256(bytes:ByteArray):String=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
}
object BackupManifestCodec{
 fun encode(v:BackupManifest):String=Properties().apply{setProperty("schemaVersion",v.schemaVersion.toString());setProperty("schoolEol",v.schoolEol);setProperty("schoolName",v.schoolName);setProperty("createdAt",v.createdAt.toString());setProperty("sha256",v.sha256)}.let{p->StringWriter().also{p.store(it,"Sala de Leitura backup")}.toString()}
 fun decode(text:String):BackupManifest=Properties().apply{load(StringReader(text))}.let{p->BackupManifest(p.getProperty("schemaVersion").toInt(),p.getProperty("schoolEol"),p.getProperty("schoolName"),p.getProperty("createdAt").toLong(),p.getProperty("sha256"))}
}
