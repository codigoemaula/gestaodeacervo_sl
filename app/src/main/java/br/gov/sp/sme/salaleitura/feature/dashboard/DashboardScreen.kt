@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val cards = listOf(
        "Obras" to s.editions, "Exemplares" to s.copies, "Disponíveis" to s.available,
        "Empréstimos" to s.activeLoans, "Vencidos" to s.overdueLoans, "Danificados/extraviados" to s.damagedOrLost,
        "Devoluções hoje" to s.returnedToday
    )
    Scaffold(topBar = { TopAppBar(title = { Text("Sala de Leitura") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cards) { (label, value) -> Card { Column(Modifier.padding(16.dp)) { Text(value.toString(), style = MaterialTheme.typography.headlineMedium); Text(label) } } }
            }
            Text("Último inventário: " + (s.lastInventoryAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "nenhum"), style = MaterialTheme.typography.bodySmall)
            val primary = listOf("catalog" to "Acervo", "people" to "Pessoas", "checkout" to "Emprestar", "return" to "Devolver", "inventory" to "Inventário", "loans" to "Empréstimos")
            primary.chunked(3).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (route, label) ->
                        if (rowIndex == 0) FilledTonalButton(onClick = { onNavigate(route) }, modifier = Modifier.weight(1f)) { Text(label) }
                        else OutlinedButton(onClick = { onNavigate(route) }, modifier = Modifier.weight(1f)) { Text(label) }
                    }
                }
            }
            TextButton(onClick = { onNavigate("more") }, modifier = Modifier.fillMaxWidth()) { Text("Relatórios, etiquetas, backup e configurações") }
        }
    }
}
