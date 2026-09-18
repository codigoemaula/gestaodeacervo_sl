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
    val context = LocalContext.current
    val result by vm.import.collectAsStateWithLifecycle()
    val plan by vm.importPlan.collectAsStateWithLifecycle()
    val status by vm.csvMessage.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader(Charsets.UTF_8)?.use { reader -> vm.previewCsv(reader.readText()) } }
    }
    val template = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.saveCsv(it, model = true) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Importar registros") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Como preparar sua planilha", style = MaterialTheme.typography.titleLarge)
            Text("Cada coluna é uma informação; cada linha, uma pessoa. A primeira linha contém os cabeçalhos. Nome e tipo são obrigatórios; 'ano' significa ano letivo.")
            Text("Colunas: nome, tipo, identificador, turma, ano, turno, funcao e codigo_interno (opcional). Use ESTUDANTE ou PROFISSIONAL. Salve em CSV UTF-8; vírgula e ponto e vírgula são aceitos.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { template.launch("modelo-sala-leitura.csv") }, modifier = Modifier.weight(1f)) { Text("Baixar modelo") }
                Button(onClick = { picker.launch(arrayOf("text/csv", "text/plain", "text/*", "application/vnd.ms-excel")) }, modifier = Modifier.weight(1f)) { Text("Importar CSV") }
            }
            Text("Prévia: ${result.rows.size} linha(s) válida(s) · ${plan.inserts.size} novo(s) · ${plan.updates.size} atualização(ões)", style = MaterialTheme.typography.titleSmall)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(result.errors + plan.errors) { Text(it, color = MaterialTheme.colorScheme.error) }
                items(result.rows.take(200), key = { it.line }) { row ->
                    ListItem(headlineContent = { Text("Linha ${row.line} · ${row.name}") }, supportingContent = { Text("${row.type} · ${row.className ?: "Sem turma"} · ${row.internalCode ?: "Código novo ou identificado por matrícula"}") })
                    HorizontalDivider()
                }
            }
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("Conflitos impedem a importação. Identificadores internos conhecidos preservam empréstimos e histórico. Nenhum cadastro será alterado antes de confirmar.", style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { vm.commitCsv { onBack() } },
                enabled = result.rows.isNotEmpty() && result.errors.isEmpty() && plan.errors.isEmpty() && plan.inserts.size + plan.updates.size == result.rows.size,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirmar importação · ${result.rows.size} registros") }
        }
    }
}
