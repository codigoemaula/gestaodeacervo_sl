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

@Composable
fun PeopleScreen(onBack: () -> Unit, onAdd: () -> Unit, onClasses: () -> Unit, onImport: () -> Unit, vm: PeopleViewModel = viewModel()) {
    val people by vm.people.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Pessoas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }, actions = { TextButton(onClick = onImport) { Text("CSV") }; TextButton(onClick = onClasses) { Text("Turmas") }; TextButton(onClick = onAdd) { Text("Novo") } }) }) { padding ->
        if (people.isEmpty()) Box(Modifier.padding(padding).padding(24.dp)) { Text("Nenhuma pessoa cadastrada.") }
        else LazyColumn(Modifier.padding(padding)) { items(people, key = { it.id }) { p -> ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${if (p.type.name == "STUDENT") "Estudante" else "Profissional"} • ${p.internalCode}${p.institutionalId?.let { " • $it" } ?: ""}") }); HorizontalDivider() } }
    }
}
