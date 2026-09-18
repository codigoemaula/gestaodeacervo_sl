package br.gov.sp.sme.salaleitura.feature.backup

import android.content.Context
import android.net.Uri
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context:Context){
 suspend fun suggestedName():String=withContext(Dispatchers.IO){
  val school=AppDatabase.get(context).schoolDao().get();val name=(school?.name?:"SALA-LEITURA").uppercase().replace(Regex("[^A-Z0-9]+"),"-").trim('-')
  "$name-${java.time.LocalDate.now()}.slbackup"
 }
 suspend fun exportTo(uri:Uri):Result<Unit> = withContext(Dispatchers.IO){runCatching{
  val db=AppDatabase.get(context);db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close();val school=requireNotNull(db.schoolDao().get()){ "Escola não configurada" }
  val dbFile=context.getDatabasePath(AppDatabase.DB_NAME);val hash=sha256File(dbFile);val manifest=BackupManifest(1,school.eolCode,school.name,System.currentTimeMillis(),hash)
  val out=requireNotNull(context.contentResolver.openOutputStream(uri)){"Não foi possível abrir o destino"}
  ZipOutputStream(BufferedOutputStream(out)).use{zip->
   zip.putNextEntry(ZipEntry("manifest.properties"));zip.write(BackupManifestCodec.encode(manifest).toByteArray(Charsets.UTF_8));zip.closeEntry()
   zip.putNextEntry(ZipEntry("database.sqlite"));dbFile.inputStream().use{it.copyTo(zip)};zip.closeEntry()
  }
 }}
 suspend fun inspect(uri:Uri):Result<BackupManifest> = withContext(Dispatchers.IO){runCatching{readArchive(uri,copyDatabase=false).first}}
 suspend fun restore(uri:Uri):Result<BackupManifest> = withContext(Dispatchers.IO){runCatching{
  val (manifest,temp)=readArchive(uri,copyDatabase=true);require(manifest.schemaVersion==1){"Versão de backup incompatível"};requireNotNull(temp)
  require(sha256File(temp).equals(manifest.sha256,true)){"Backup corrompido: checksum inválido"}
  AppDatabase.closeInstance();val target=context.getDatabasePath(AppDatabase.DB_NAME);target.parentFile?.mkdirs();temp.copyTo(target,overwrite=true);File(target.path+"-wal").delete();File(target.path+"-shm").delete();temp.delete();manifest
 }}
 private fun readArchive(uri:Uri,copyDatabase:Boolean):Pair<BackupManifest,File?>{
  var manifest:BackupManifest?=null;var temp:File?=null;val input=requireNotNull(context.contentResolver.openInputStream(uri)){"Arquivo inacessível"}
  ZipInputStream(BufferedInputStream(input)).use { zip ->
   var entry = zip.nextEntry
   while (entry != null) {
    when (entry.name) {
     "manifest.properties" -> manifest = BackupManifestCodec.decode(readCurrentEntry(zip).toString(Charsets.UTF_8))
     "database.sqlite" -> if (copyDatabase) {
      temp = File.createTempFile("restore-", ".sqlite", context.cacheDir)
      temp!!.outputStream().use { output -> zip.copyTo(output) }
     }
    }
    zip.closeEntry()
    entry = zip.nextEntry
   }
  }
  return requireNotNull(manifest){"Manifesto ausente"} to temp
 }
 private fun readCurrentEntry(zip: ZipInputStream): ByteArray {
  val output = ByteArrayOutputStream()
  val buffer = ByteArray(8192)
  while (true) {
   val count = zip.read(buffer)
   if (count < 0) break
   output.write(buffer, 0, count)
  }
  return output.toByteArray()
 }
 private fun sha256File(file:File):String{val d=MessageDigest.getInstance("SHA-256");file.inputStream().use{input->val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;d.update(b,0,n)}};return d.digest().joinToString(""){"%02x".format(it)}}
}
