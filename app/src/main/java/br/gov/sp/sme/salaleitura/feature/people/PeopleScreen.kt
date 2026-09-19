@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.people

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
fun PeopleScreen(onBack: () -> Unit, onAdd: () -> Unit, onClasses: () -> Unit, onImport: () -> Unit,
                 onEdit: (Long) -> Unit, vm: PeopleViewModel = viewModel()) {
    val people by vm.people.collectAsStateWithLifecycle()
    val groups by vm.classes.collectAsStateWithLifecycle()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var confirmExport by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.saveCsv(it, onResult = { result -> exportMessage = result }) }
    }
    if (confirmExport) AlertDialog(
        onDismissRequest = { confirmExport = false },
        title = { Text("Exportar dados pessoais sem criptografia?") },
        text = { Text("O CSV contém nomes e outros dados de estudantes e profissionais. O arquivo NÃO é criptografado: quem tiver acesso ao destino escolhido poderá copiá-lo ou enviá-lo. Utilize apenas quando necessário e selecione um local protegido.") },
        confirmButton = { TextButton(onClick = { confirmExport = false; export.launch("leitores-sala-de-leitura.csv") }) { Text("Exportar CSV") } },
        dismissButton = { TextButton(onClick = { confirmExport = false }) { Text("Cancelar") } }
    )
    Scaffold(topBar = { TopAppBar(title = { Text("Leitores e turmas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Cadastros da Sala de Leitura", style = MaterialTheme.typography.titleLarge)
            Text("Organize salas e estudantes. Fotos são opcionais e permanecem no armazenamento privado do aplicativo.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClasses, modifier = Modifier.weight(1f)) { Text("Salas e turmas") }
                FilledTonalButton(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Novo leitor") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Importar CSV") }
                OutlinedButton(onClick = { confirmExport = true }, modifier = Modifier.weight(1f)) { Text("Exportar CSV") }
            }
            exportMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("${people.size} cadastro(s)", style = MaterialTheme.typography.titleSmall)
            if (people.isEmpty()) Text("Nenhuma pessoa cadastrada. Crie uma turma e adicione seus estudantes.")
            else LazyColumn(Modifier.weight(1f)) {
                items(people, key = { it.id }) { reader ->
                    val room = groups.firstOrNull { it.id == reader.classGroupId }?.name
                    ListItem(
                        modifier = Modifier.clickable { onEdit(reader.id) },
                        leadingContent = { ReaderAvatar(reader.photoFilename) },
                        headlineContent = { Text(reader.name) },
                        supportingContent = { Text("${if (reader.type.name == "STUDENT") "Estudante" else "Profissional"}${room?.let { " · $it" } ?: ""}") },
                        trailingContent = { Text("Editar", style = MaterialTheme.typography.labelMedium) }
                    )
                    HorizontalDivider()
                }
            }
            Text("A exportação CSV contém dados pessoais, mas não inclui fotografias. Salve em um local protegido.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
