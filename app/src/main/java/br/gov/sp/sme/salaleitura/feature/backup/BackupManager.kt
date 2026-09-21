package br.gov.sp.sme.salaleitura.feature.backup

import android.content.Context
import android.net.Uri
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** All temporary plaintext is confined to the application's private cache and removed in finally. */
class BackupManager(private val context: Context) {
    private val photoName = Regex("[0-9a-f-]{36}\\.jpg")
    private val photoFolder get() = File(context.filesDir, "reader_photos")

    suspend fun suggestedName(): String = withContext(Dispatchers.IO) {
        val school = AppDatabase.get(context).schoolDao().get()
        val name = (school?.name ?: "SALA-LEITURA").uppercase().replace(Regex("[^A-Z0-9]+"), "-").trim('-')
        "$name-${java.time.LocalDate.now()}.slbackup"
    }

    suspend fun exportTo(uri: Uri, password: CharArray): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        require(password.size >= 10) { "Informe uma senha com pelo menos 10 caracteres." }
        val archive = File.createTempFile("sl-export-", ".zip", context.cacheDir)
        try {
            val db = AppDatabase.get(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            val school = requireNotNull(db.schoolDao().get()) { "Unidade não configurada" }
            val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
            require(dbFile.isFile) { "Banco de dados não encontrado." }
            val manifest = BackupManifest(2, school.eolCode, school.name, System.currentTimeMillis(), sha256File(dbFile))
            ZipOutputStream(BufferedOutputStream(archive.outputStream())).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.properties"))
                zip.write(BackupManifestCodec.encode(manifest).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("database.sqlite"))
                dbFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                photoFolder.listFiles()?.filter { it.isFile && photoName.matches(it.name) }?.forEach { photo ->
                    zip.putNextEntry(ZipEntry("reader_photos/${photo.name}"))
                    photo.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            val output = requireNotNull(context.contentResolver.openOutputStream(uri, "w")) { "Destino indisponível" }
            output.use { archive.inputStream().use { input -> EncryptedBackupCodec.encrypt(input, it, password) } }
        } finally {
            archive.delete()
        }
    } }

    suspend fun inspect(uri: Uri, password: CharArray, allowLegacy: Boolean = false): Result<BackupManifest> =
        withContext(Dispatchers.IO) { runCatching {
            withDecryptedArchive(uri, password, allowLegacy) { zip ->
                val stage = newStage()
                try { unpack(zip, stage).first } finally { stage.deleteRecursively() }
            }
        } }

    suspend fun restore(uri: Uri, password: CharArray, allowLegacy: Boolean = false): Result<BackupManifest> =
        withContext(Dispatchers.IO) { runCatching {
            withDecryptedArchive(uri, password, allowLegacy) { zip ->
                val stage = newStage()
                try {
                    val (manifest, database) = unpack(zip, stage)
                    val target = context.getDatabasePath(AppDatabase.DB_NAME)
                    target.parentFile?.mkdirs()
                    val rollback = File(stage, "previous.sqlite")
                    val existingPhotos = photoFolder
                    val rollbackPhotos = File(stage, "previous_photos")
                    val photosFromArchive = File(stage, "reader_photos")
                    val hasCompletePhotos = manifest.schemaVersion >= 2
                    AppDatabase.closeInstance()
                    if (target.isFile) target.copyTo(rollback, overwrite = true)
                    var replacedPhotos = false
                    try {
                        database.copyTo(target, overwrite = true)
                        File(target.path + "-wal").delete()
                        File(target.path + "-shm").delete()
                        if (hasCompletePhotos) {
                            if (existingPhotos.exists()) check(existingPhotos.renameTo(rollbackPhotos)) { "Não foi possível preservar fotos anteriores." }
                            replacedPhotos = true
                            if (photosFromArchive.exists()) {
                                check(photosFromArchive.renameTo(existingPhotos)) { "Não foi possível restaurar fotos." }
                            } else {
                                check(existingPhotos.mkdirs()) { "Não foi possível preparar pasta de fotos." }
                            }
                        }
                    } catch (failure: Exception) {
                        if (rollback.isFile) rollback.copyTo(target, overwrite = true) else target.delete()
                        if (replacedPhotos) {
                            existingPhotos.deleteRecursively()
                            if (rollbackPhotos.exists()) rollbackPhotos.renameTo(existingPhotos)
                        }
                        throw failure
                    }
                    manifest
                } finally { stage.deleteRecursively() }
            }
        } }

    private inline fun <T> withDecryptedArchive(uri: Uri, password: CharArray, allowLegacy: Boolean, block: (File) -> T): T {
        val zip = File.createTempFile("sl-decrypted-", ".zip", context.cacheDir)
        try {
            val source = requireNotNull(context.contentResolver.openInputStream(uri)) { "Arquivo inacessível" }
            source.buffered().use { input ->
                input.mark(4)
                val magic = ByteArray(4)
                val length = input.read(magic)
                input.reset()
                val encrypted = length == 4 && magic.contentEquals(byteArrayOf(0x53, 0x4c, 0x42, 0x32))
                if (encrypted) {
                    zip.outputStream().buffered().use { output -> EncryptedBackupCodec.decrypt(input, output, password) }
                } else {
                    require(allowLegacy) { "Backup antigo sem criptografia: habilite a importação legada conscientemente." }
                    zip.outputStream().use { output -> copyBounded(input, output, 400L * 1024 * 1024) }
                }
            }
            return block(zip)
        } finally { zip.delete() }
    }

    private fun newStage(): File = File(context.cacheDir, "sl-restore-${UUID.randomUUID()}").apply {
        check(mkdirs()) { "Não foi possível preparar restauração." }
    }

    private fun unpack(zipFile: File, stage: File): Pair<BackupManifest, File> {
        var manifest: BackupManifest? = null
        var database: File? = null
        val seen = hashSetOf<String>()
        var total = 0L
        var count = 0
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(seen.add(entry.name)) { "Entrada duplicada no backup." }
                require(++count <= 20_000) { "Backup com entradas excessivas." }
                val destination = when {
                    entry.name == "manifest.properties" -> File(stage, "manifest.properties")
                    entry.name == "database.sqlite" -> File(stage, "database.sqlite")
                    entry.name.startsWith("reader_photos/") && photoName.matches(entry.name.removePrefix("reader_photos/")) ->
                        File(File(stage, "reader_photos").apply { mkdirs() }, entry.name.removePrefix("reader_photos/"))
                    else -> throw IllegalArgumentException("Arquivo inesperado no backup: ${entry.name}")
                }
                val cap = when (entry.name) {
                    "manifest.properties" -> 64L * 1024
                    "database.sqlite" -> 300L * 1024 * 1024
                    else -> 6L * 1024 * 1024
                }
                destination.outputStream().use { output -> total += copyBounded(zip, output, cap) }
                require(total <= 400L * 1024 * 1024) { "Backup excede limite de segurança." }
                when (entry.name) {
                    "manifest.properties" -> manifest = BackupManifestCodec.decode(destination.readText(Charsets.UTF_8))
                    "database.sqlite" -> database = destination
                }
                zip.closeEntry()
            }
        }
        val m = requireNotNull(manifest) { "Manifesto ausente." }
        require(m.schemaVersion in 1..2) { "Versão incompatível de backup." }
        val db = requireNotNull(database) { "Banco de dados ausente." }
        require(sha256File(db).equals(m.sha256, ignoreCase = true)) { "Backup corrompido: checksum inválido." }
        if (m.schemaVersion == 1) require(!File(stage, "reader_photos").exists()) { "Fotos inesperadas em backup legado." }
        return m to db
    }

    private fun copyBounded(input: InputStream, output: OutputStream, cap: Long): Long {
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= cap) { "Arquivo excede o tamanho permitido." }
            output.write(buffer, 0, count)
        }
        return total
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
