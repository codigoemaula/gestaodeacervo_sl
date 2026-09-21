@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.setup

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
fun SchoolSetupScreen(onFinished: () -> Unit, vm: SchoolSetupViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.saved) { if (s.saved) onFinished() }
    Scaffold(topBar = { TopAppBar(title = { Text("Configurar Sala de Leitura") }) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Configure sua unidade. Os cadastros e empréstimos permanecem neste dispositivo; somente consultas bibliográficas por ISBN usam a internet.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(s.name, { v -> vm.update { it.copy(name = v) } }, label = { Text("Nome da unidade") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.schoolYear, { v -> vm.update { it.copy(schoolYear = v.filter(Char::isDigit)) } }, label = { Text("Ano letivo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.readingRoomName, { v -> vm.update { it.copy(readingRoomName = v) } }, label = { Text("Identificação da Sala de Leitura") }, modifier = Modifier.fillMaxWidth())
            Text("Prazo padrão de empréstimo")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = s.defaultLoanDays == 7, onClick = { vm.update { it.copy(defaultLoanDays = 7) } }, label = { Text("7 dias") })
                FilterChip(selected = s.defaultLoanDays == 14, onClick = { vm.update { it.copy(defaultLoanDays = 14) } }, label = { Text("14 dias") })
            }
            s.errors.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Text("Salvar e iniciar") }
        }
    }
}
