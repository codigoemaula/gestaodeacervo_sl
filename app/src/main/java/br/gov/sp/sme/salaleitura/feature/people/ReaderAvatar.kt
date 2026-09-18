package br.gov.sp.sme.salaleitura.feature.people

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ReaderAvatar(photoFilename: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val image = remember(photoFilename) { ReaderPhotoStore(context).read(photoFilename)?.asImageBitmap() }
    Surface(modifier = modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        if (image != null) Image(bitmap = image, contentDescription = "Foto opcional do cadastro", contentScale = ContentScale.Crop,
            modifier = Modifier.clip(CircleShape))
        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, contentDescription = null) }
    }
}
