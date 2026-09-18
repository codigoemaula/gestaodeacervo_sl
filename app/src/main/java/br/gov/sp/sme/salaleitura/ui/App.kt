package br.gov.sp.sme.salaleitura.ui

import android.content.Context
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.ui.navigation.AppNav
import br.gov.sp.sme.salaleitura.ui.theme.SalaLeituraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SalaLeituraApp(context: Context = LocalContext.current) {
    var hasSchool by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { hasSchool = withContext(Dispatchers.IO) { AppDatabase.get(context).schoolDao().get() != null } }
    SalaLeituraTheme { if (hasSchool == null) CircularProgressIndicator() else AppNav(hasSchool = hasSchool == true) }
}
