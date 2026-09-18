package br.gov.sp.sme.salaleitura.feature.people

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** No camera, external storage permission, remote upload or public student-photo URI. */
class ReaderPhotoStore(private val context: Context) {
    private val folder get() = File(context.filesDir, "reader_photos").apply { mkdirs() }

    fun photoFile(name: String?): File? {
        if (name == null || !Regex("[0-9a-f-]{36}\.jpg").matches(name)) return null
        return File(folder, name)
    }

    suspend fun importPhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val type = context.contentResolver.getType(uri)
        require(type == null || type.startsWith("image/")) { "Selecione um arquivo de imagem" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("Não foi possível abrir a imagem")
        require(bounds.outWidth in 1..12000 && bounds.outHeight in 1..12000) { "Imagem inválida ou excessivamente grande" }
        var sample = 1
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Não foi possível ler a imagem")
        val name = "${UUID.randomUUID()}.jpg"
        val target = requireNotNull(photoFile(name))
        try {
            target.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)) { "Não foi possível gravar a foto" }
            }
            name
        } catch (ex: Exception) {
            target.delete()
            throw ex
        } finally { bitmap.recycle() }
    }

    fun remove(name: String?) { photoFile(name)?.delete() }
    fun read(name: String?): Bitmap? = photoFile(name)?.takeIf(File::isFile)?.let { BitmapFactory.decodeFile(it.absolutePath) }
}
