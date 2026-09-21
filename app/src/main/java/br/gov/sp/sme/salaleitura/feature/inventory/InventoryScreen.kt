@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun InventoryScreen(onBack: () -> Unit, onScan: () -> Unit, vm: InventoryViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var cameraOpen by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    if (cameraOpen && state.sessionId != null) {
        InventoryContinuousScannerScreen(onBack = { cameraOpen = false }, vm = vm)
        return
    }
    if (confirmClose) {
        AlertDialog(onDismissRequest = { confirmClose = false }, title = { Text("Encerrar inventário?") },
            text = { Text("Esta sessão será concluída. Exemplares emprestados não serão classificados automaticamente como desaparecidos.") },
            confirmButton = { Button(onClick = { confirmClose = false; vm.close() }) { Text("Encerrar sessão") } },
            dismissButton = { TextButton(onClick = { confirmClose = false }) { Text("Continuar conferência") } })
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Inventário do acervo") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } })
    }) { p ->
        Column(Modifier.padding(p).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.sessionId == null) {
                Text("Inicie uma sessão para conferir os exemplares das estantes usando a câmera. Você poderá pausá-la e continuar depois.")
                Button(onClick = vm::start, modifier = Modifier.fillMaxWidth()) { Text("Iniciar novo inventário") }
            } else {
                Text("Sessão #${state.sessionId} em andamento", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { cameraOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Abrir câmera · conferência contínua") }
                Text("Ou informe o código manualmente:")
                OutlinedTextField(state.code, vm::setCode, label = { Text("Etiqueta SL ou patrimônio") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = vm::scan, enabled = state.code.isNotBlank() && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Conferir código digitado") }
                TextButton(onClick = { confirmClose = true }, modifier = Modifier.fillMaxWidth()) { Text("Encerrar e gerar resumo") }
            }
            state.summary?.let { summary ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Conferência", style = MaterialTheme.typography.titleLarge)
                        Text("Localizados: ${summary.found} / ${summary.registered}")
                        LinearProgressIndicator(progress = { if (summary.registered == 0) 0f else summary.found.toFloat() / summary.registered }, modifier = Modifier.fillMaxWidth())
                        Text("Emprestados: ${summary.loaned} · Não localizados: ${summary.missing}")
                        Text("Danificados: ${summary.damaged} · Baixados: ${summary.withdrawn}")
                        Text("Empréstimos não são tratados como extravio.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}
