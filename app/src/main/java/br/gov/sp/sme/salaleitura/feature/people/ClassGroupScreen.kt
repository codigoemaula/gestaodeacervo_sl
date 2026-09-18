@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.model.Shift
import java.time.Year

@Composable
fun ClassGroupScreen(onBack: () -> Unit, vm: PeopleViewModel = viewModel()) {
    val classes by vm.classes.collectAsStateWithLifecycle(); var name by remember { mutableStateOf("") }; var grade by remember { mutableStateOf("") }; var shift by remember { mutableStateOf(Shift.MORNING) }
    Scaffold(topBar = { TopAppBar(title = { Text("Turmas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Turma") }, modifier = Modifier.weight(1f)); OutlinedTextField(grade, { grade = it }, label = { Text("Ano/série") }, modifier = Modifier.weight(1f)) }
            Text("Turno")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Shift.MORNING to "Manhã", Shift.AFTERNOON to "Tarde", Shift.EVENING to "Noite", Shift.FULL_TIME to "Integral").forEach { (value, label) ->
                    FilterChip(selected = shift == value, onClick = { shift = value }, label = { Text(label) })
                }
            }
            Button(onClick = { vm.addClass(name, grade, shift, Year.now().value) { name = ""; grade = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Adicionar turma") }
            HorizontalDivider(); LazyColumn { items(classes, key = { it.id }) { c -> ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text("${c.grade} • ${c.shift.name}") }) } }
        }
    }
}
