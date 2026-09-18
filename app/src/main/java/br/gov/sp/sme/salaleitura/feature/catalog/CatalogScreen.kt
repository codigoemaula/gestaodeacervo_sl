@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.data.local.entity.BookCopyEntity

@Composable
fun CatalogScreen(onBack: () -> Unit, onAdd: () -> Unit, vm: CatalogViewModel = viewModel()) {
    val editions by vm.editions.collectAsStateWithLifecycle()
    val selected by vm.selectedEdition.collectAsStateWithLifecycle()
    val copies by vm.selectedCopies.collectAsStateWithLifecycle()
    val aliases by vm.selectedAliases.collectAsStateWithLifecycle()
    val feedback by vm.catalogMessage.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    val matches = editions.filter { book ->
        search.isBlank() || listOfNotNull(book.title, book.authors, book.publisher, book.isbn13, book.isbn10)
            .any { it.contains(search.trim(), ignoreCase = true) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Acervo da Sala de Leitura") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
            actions = { TextButton(onClick = onAdd) { Text("Cadastrar obra") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Pesquise obras e confira seus exemplares físicos", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Título, autoria, ISBN ou código do exemplar") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.findCopy(search) }, enabled = search.isNotBlank()) { Text("Localizar exemplar/patrimônio") }
                    TextButton(onClick = { search = "" }) { Text("Limpar") }
                }
                feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Text("${matches.size} de ${editions.size} obra(s)", style = MaterialTheme.typography.bodySmall)
            }
            if (editions.isEmpty()) item { Text("Nenhuma obra cadastrada. Escaneie um ISBN ou toque em Cadastrar obra.") }
            else if (matches.isEmpty()) item { Text("Nenhuma obra corresponde à busca. Se digitou um código patrimonial, use Localizar exemplar/patrimônio.") }
            items(matches, key = { it.id }) { book ->
                ElevatedCard(onClick = { vm.selectEdition(book.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(book.title, style = MaterialTheme.typography.titleLarge)
                        if (book.authors.isNotBlank()) Text(book.authors)
                        Text(listOfNotNull(book.publisher, book.isbn13).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        Text(if (selected == book.id) "Ocultar exemplares" else "Consultar exemplares e patrimônios",
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        if (selected == book.id) {
                            HorizontalDivider()
                            Text("${copies.count { it.status == CopyStatus.AVAILABLE }} disponível(is) de ${copies.size} exemplar(es)",
                                style = MaterialTheme.typography.titleMedium)
                            if (copies.isEmpty()) Text("Carregando exemplares…")
                            copies.forEach { copy ->
                                key(copy.id) {
                                    val alias = aliases.firstOrNull { it.copyId == copy.id }?.normalizedCode
                                    CopyPatrimonyRow(copy, alias, onBind = { entered -> vm.bindPatrimony(copy.id, entered) },
                                        onRemove = { vm.removePatrimony(copy.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyPatrimonyRow(copy: BookCopyEntity, alias: String?, onBind: (String) -> Unit, onRemove: () -> Unit) {
    var patrimony by remember(copy.id, alias) { mutableStateOf(alias.orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Exemplar ${copy.sequence} · ${copy.internalCode}", style = MaterialTheme.typography.titleSmall)
        Text("${copy.status.portuguese()}${copy.location?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(patrimony, { patrimony = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Patrimônio existente da escola (opcional)") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onBind(patrimony) }, enabled = patrimony.isNotBlank() && patrimony.trim().uppercase() != alias) { Text(if (alias == null) "Vincular" else "Atualizar") }
            if (alias != null) TextButton(onClick = onRemove) { Text("Desvincular") }
        }
        HorizontalDivider()
    }
}

private fun CopyStatus.portuguese(): String = when(this) {
    CopyStatus.AVAILABLE -> "Disponível"
    CopyStatus.LOANED -> "Emprestado"
    CopyStatus.DAMAGED -> "Danificado"
    CopyStatus.LOST -> "Extraviado"
    CopyStatus.MAINTENANCE -> "Em manutenção"
    CopyStatus.WITHDRAWN -> "Baixado"
}
