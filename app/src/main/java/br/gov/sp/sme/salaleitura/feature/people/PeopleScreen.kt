@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.people

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PeopleScreen(onBack: () -> Unit, onAdd: () -> Unit, onClasses: () -> Unit, onImport: () -> Unit, vm: PeopleViewModel = viewModel()) {
    val people by vm.people.collectAsStateWithLifecycle()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.saveCsv(it, onResult = { result -> exportMessage = result }) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Pessoas e turmas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Leitores da Sala de Leitura", style = MaterialTheme.typography.titleLarge)
            Text("Cadastre, importe ou exporte estudantes e profissionais. Os arquivos permanecem no local escolhido no dispositivo.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Importar CSV") }
                OutlinedButton(onClick = { export.launch("leitores-sala-de-leitura.csv") }, modifier = Modifier.weight(1f)) { Text("Exportar CSV") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Nova pessoa") }
                OutlinedButton(onClick = onClasses, modifier = Modifier.weight(1f)) { Text("Turmas") }
            }
            exportMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("${people.size} cadastro(s)", style = MaterialTheme.typography.titleSmall)
            if (people.isEmpty()) Text("Nenhuma pessoa cadastrada. Cadastre individualmente ou importe uma planilha CSV.")
            else LazyColumn(Modifier.weight(1f)) {
                items(people, key = { it.id }) { p ->
                    ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${if (p.type.name == "STUDENT") "Estudante" else "Profissional"} · ${p.internalCode}${p.institutionalId?.let { " · $it" } ?: ""}") })
                    HorizontalDivider()
                }
            }
            Text("A exportação contém dados pessoais. Salve o arquivo em um local protegido e compartilhe somente quando necessário.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
