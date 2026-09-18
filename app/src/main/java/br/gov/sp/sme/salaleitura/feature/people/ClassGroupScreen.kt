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
fun ClassGroupScreen(onBack: () -> Unit, onAddToClass: (Long) -> Unit, vm: PeopleViewModel = viewModel()) {
    val classes by vm.classes.collectAsStateWithLifecycle()
    val error by vm.personError.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(Year.now().value.toString()) }
    var shift by remember { mutableStateOf(Shift.MORNING) }
    Scaffold(topBar = { TopAppBar(title = { Text("Salas e turmas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Criar sala ou turma", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Nome da sala ou turma") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(grade, { grade = it }, label = { Text("Ano/série") }, modifier = Modifier.weight(1f))
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Ano letivo") }, modifier = Modifier.weight(1f))
            }
            Text("Turno")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Shift.MORNING to "Manhã", Shift.AFTERNOON to "Tarde", Shift.EVENING to "Noite", Shift.FULL_TIME to "Integral").forEach { (value, label) ->
                    FilterChip(selected = shift == value, onClick = { shift = value }, label = { Text(label) })
                }
            }
            Button(onClick = { year.toIntOrNull()?.let { y -> vm.addClass(name, grade, shift, y) { name = ""; grade = "" } } },
                enabled = name.isNotBlank() && (year.toIntOrNull() ?: 0) in 2000..2100,
                modifier = Modifier.fillMaxWidth()) { Text("Criar turma") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            HorizontalDivider()
            Text("Turmas cadastradas", style = MaterialTheme.typography.titleMedium)
            if (classes.isEmpty()) Text("Crie uma turma para organizar seus estudantes.")
            LazyColumn(modifier = Modifier.weight(1f, fill = true)) {
                items(classes, key = { it.id }) { group ->
                    ListItem(headlineContent = { Text(group.name) }, supportingContent = { Text("${group.grade} · ${group.schoolYear} · ${group.shift.name}") },
                        trailingContent = { TextButton(onClick = { onAddToClass(group.id) }) { Text("Incluir nome") } })
                    HorizontalDivider()
                }
            }
        }
    }
}
