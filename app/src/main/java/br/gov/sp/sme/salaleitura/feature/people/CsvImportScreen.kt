@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.people

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CsvImportScreen(onBack: () -> Unit, vm: PeopleViewModel = viewModel()) {
    val context = LocalContext.current; val result by vm.import.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> vm.previewCsv(r.readText()) } } }
    Scaffold(topBar = { TopAppBar(title = { Text("Importar CSV") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cabeçalhos: nome,tipo,identificador,turma,ano,turno,funcao")
            Button(onClick = { launcher.launch(arrayOf("text/csv", "text/plain", "text/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Selecionar arquivo") }
            result.errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
            if (result.rows.isNotEmpty()) {
                Text("Prévia: ${result.rows.size} registros válidos")
                LazyColumn(Modifier.weight(1f)) { items(result.rows.take(100)) { r -> ListItem(headlineContent = { Text(r.name) }, supportingContent = { Text("${r.type}${r.className?.let { " • $it" } ?: ""}") }) } }
                Button(onClick = { vm.commitCsv { onBack() } }, enabled = result.errors.isEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Importar ${result.rows.size} registros") }
            }
        }
    }
}
