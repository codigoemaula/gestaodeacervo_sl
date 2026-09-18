@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.catalog

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
fun BookFormScreen(onBack: () -> Unit, onScan: () -> Unit, vm: CatalogViewModel = viewModel()) {
    val s by vm.form.collectAsStateWithLifecycle()
    LaunchedEffect(s.saved) { if (s.saved) onBack() }
    Scaffold(topBar = { TopAppBar(title = { Text("Cadastrar obra") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(s.isbn, { v -> vm.update { it.copy(isbn = v, existingEditionId = null) } }, label = { Text("ISBN/EAN") }, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = onScan) { Text("Câmera") }
            }
            OutlinedButton(onClick = vm::lookupMetadata, enabled = !s.loadingMetadata, modifier = Modifier.fillMaxWidth()) { Text(if (s.loadingMetadata) "Consultando..." else "Buscar dados pelo ISBN") }
            if (s.existingEditionId != null) Text("Edição já cadastrada: a quantidade informada será adicionada como novos exemplares.", color = MaterialTheme.colorScheme.primary)
            Field("Título", s.title) { v -> vm.update { it.copy(title = v) } }
            Field("Subtítulo", s.subtitle) { v -> vm.update { it.copy(subtitle = v) } }
            Field("Autoria", s.authors) { v -> vm.update { it.copy(authors = v) } }
            Field("Editora", s.publisher) { v -> vm.update { it.copy(publisher = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Field("Ano", s.publicationYear) { v -> vm.update { it.copy(publicationYear = v.filter(Char::isDigit)) } } }; Box(Modifier.weight(1f)) { Field("Páginas", s.pageCount) { v -> vm.update { it.copy(pageCount = v.filter(Char::isDigit)) } } } }
            Field("Idioma", s.language) { v -> vm.update { it.copy(language = v) } }
            Field("Assuntos/categorias", s.subjects) { v -> vm.update { it.copy(subjects = v) } }
            Field("Coleção/série", s.series) { v -> vm.update { it.copy(series = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Field("CDD", s.cdd) { v -> vm.update { it.copy(cdd = v) } } }; Box(Modifier.weight(1f)) { Field("CDU", s.cdu) { v -> vm.update { it.copy(cdu = v) } } } }
            Field("Localização física", s.location) { v -> vm.update { it.copy(location = v) } }
            Field("Quantidade de exemplares", s.quantity) { v -> vm.update { it.copy(quantity = v.filter(Char::isDigit)) } }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Text("Cadastrar obra e exemplares") }
        }
    }
}

@Composable private fun Field(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
