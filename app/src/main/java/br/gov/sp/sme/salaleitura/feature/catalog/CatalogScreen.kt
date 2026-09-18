@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.catalog

import androidx.compose.foundation.clickable
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
fun CatalogScreen(onBack: () -> Unit, onAdd: () -> Unit, vm: CatalogViewModel = viewModel()) {
    val editions by vm.editions.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Acervo") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }, actions = { TextButton(onClick = onAdd) { Text("Cadastrar") } }) }) { padding ->
        if (editions.isEmpty()) Box(Modifier.padding(padding).padding(24.dp)) { Text("Nenhuma obra cadastrada.") }
        else LazyColumn(Modifier.padding(padding)) { items(editions, key = { it.id }) { book -> ListItem(headlineContent = { Text(book.title) }, supportingContent = { Text(listOfNotNull(book.authors.takeIf(String::isNotBlank), book.publisher, book.isbn13).joinToString(" • ")) }, modifier = Modifier.clickable { }) ; HorizontalDivider() } }
    }
}
