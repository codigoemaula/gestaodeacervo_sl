@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.people

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.model.PersonType

@Composable
fun PersonFormScreen(onBack: () -> Unit, vm: PeopleViewModel = viewModel()) {
    val classes by vm.classes.collectAsStateWithLifecycle(); var name by remember { mutableStateOf("") }; var id by remember { mutableStateOf("") }; var role by remember { mutableStateOf("") }; var type by remember { mutableStateOf(PersonType.STUDENT) }; var classId by remember { mutableStateOf<Long?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Cadastrar pessoa") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(type == PersonType.STUDENT, { type = PersonType.STUDENT }, label = { Text("Estudante") }); FilterChip(type == PersonType.PROFESSIONAL, { type = PersonType.PROFESSIONAL }, label = { Text("Profissional") }) }
            OutlinedTextField(name, { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(id, { id = it }, label = { Text(if (type == PersonType.STUDENT) "Matrícula/identificador (opcional)" else "RF/identificador (opcional)") }, modifier = Modifier.fillMaxWidth())
            if (type == PersonType.PROFESSIONAL) OutlinedTextField(role, { role = it }, label = { Text("Função") }, modifier = Modifier.fillMaxWidth())
            if (type == PersonType.STUDENT && classes.isNotEmpty()) {
                Text("Turma"); classes.forEach { c -> FilterChip(classId == c.id, { classId = c.id }, label = { Text(c.name) }) }
            }
            Button(onClick = { vm.addPerson(name, type, id, classId, null, null, role, onBack) }, modifier = Modifier.fillMaxWidth()) { Text("Salvar") }
        }
    }
}
