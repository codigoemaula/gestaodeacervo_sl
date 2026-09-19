@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.people

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.model.PersonType

@Composable
fun PersonFormScreen(
    onBack: () -> Unit,
    personId: Long? = null,
    initialClassId: Long? = null,
    vm: PeopleViewModel = viewModel()
) {
    val classes by vm.classes.collectAsStateWithLifecycle()
    val error by vm.personError.collectAsStateWithLifecycle()
    var name by remember(personId) { mutableStateOf("") }
    var institutionalId by remember(personId) { mutableStateOf("") }
    var role by remember(personId) { mutableStateOf("") }
    var type by remember(personId) { mutableStateOf(PersonType.STUDENT) }
    var classId by remember(personId, initialClassId) { mutableStateOf(initialClassId) }
    var existingPhoto by remember(personId) { mutableStateOf<String?>(null) }
    var selectedPhoto by remember(personId) { mutableStateOf<android.net.Uri?>(null) }
    var removePhoto by remember(personId) { mutableStateOf(false) }
    var confirmRemoval by remember(personId) { mutableStateOf(false) }
    var loaded by remember(personId) { mutableStateOf(personId == null) }
    LaunchedEffect(personId) {
        if (personId != null) vm.loadPerson(personId) { person ->
            if (person != null) {
                name = person.name
                institutionalId = person.institutionalId.orEmpty()
                role = person.role.orEmpty()
                type = person.type
                classId = person.classGroupId
                existingPhoto = person.photoFilename
            }
            loaded = true
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { selectedPhoto = uri; removePhoto = false }
    }
    if (confirmRemoval && personId != null) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text("Remover dados pessoais") },
            text = { Text("Confirma a remoção definitiva do nome, identificador, turma e foto? Se houver empréstimos em aberto, a operação será recusada. Empréstimos já devolvidos permanecerão sem identificação pessoal para preservar a contagem do acervo. A operação não altera cópias exportadas anteriormente.") },
            confirmButton = { TextButton(onClick = { confirmRemoval = false; vm.removePersonalData(personId, onBack) }) { Text("Confirmar remoção") } },
            dismissButton = { TextButton(onClick = { confirmRemoval = false }) { Text("Cancelar") } }
        )
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (personId == null) "Cadastrar leitor" else "Editar cadastro") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!loaded) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Identificação por nome, sem QR Code pessoal ou câmera.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == PersonType.STUDENT, { type = PersonType.STUDENT }, label = { Text("Estudante") })
                FilterChip(type == PersonType.PROFESSIONAL, { type = PersonType.PROFESSIONAL }, label = { Text("Profissional") })
            }
            OutlinedTextField(name, { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(institutionalId, { institutionalId = it },
                label = { Text(if (type == PersonType.STUDENT) "Identificador institucional (opcional)" else "Identificador profissional (opcional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (type == PersonType.PROFESSIONAL) OutlinedTextField(role, { role = it }, label = { Text("Função") }, modifier = Modifier.fillMaxWidth())
            if (type == PersonType.STUDENT) {
                Text("Sala ou turma", style = MaterialTheme.typography.titleSmall)
                if (classes.isEmpty()) Text("Cadastre uma turma em Salas e turmas para organizar os estudantes.")
                classes.forEach { group ->
                    FilterChip(classId == group.id, { classId = group.id }, label = { Text("${group.name} · ${group.schoolYear}") })
                }
            }
            HorizontalDivider()
            Text("Foto do cadastro (opcional)", style = MaterialTheme.typography.titleMedium)
            if (existingPhoto != null && !removePhoto && selectedPhoto == null) ReaderAvatar(existingPhoto)
            if (selectedPhoto != null) Text("Nova foto escolhida. Será copiada para o armazenamento privado ao salvar.")
            if ((existingPhoto == null && selectedPhoto == null) || removePhoto) Text("Sem foto: o aplicativo utilizará um ícone padrão.")
            OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()) { Text("Escolher foto na galeria") }
            if (existingPhoto != null || selectedPhoto != null) TextButton(onClick = { selectedPhoto = null; removePhoto = true }) { Text("Remover foto") }
            Text("A foto não será usada para reconhecimento facial nem incluída nas exportações CSV.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                vm.savePerson(personId, name, type, institutionalId, classId, null, null, role, selectedPhoto, removePhoto, onBack)
            }, enabled = loaded && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(if (personId == null) "Cadastrar leitor" else "Salvar alterações")
            }
            if (personId != null) {
                HorizontalDivider()
                OutlinedButton(onClick = { confirmRemoval = true }, enabled = loaded, modifier = Modifier.fillMaxWidth()) { Text("Remover dados pessoais deste leitor") }
                Text("A remoção é irreversível; devolva os livros antes de confirmá-la.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
