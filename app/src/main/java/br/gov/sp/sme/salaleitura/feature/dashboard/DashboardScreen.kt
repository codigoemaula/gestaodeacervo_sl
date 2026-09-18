@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.ui.theme.ReadingRoomColors
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit, vm: DashboardViewModel = viewModel()) {
    val stats by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Sala de Leitura", style = MaterialTheme.typography.titleMedium) },
            actions = { TextButton(onClick = { onNavigate("more") }) { Text("Gestão") } })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("REDE MUNICIPAL DE SÃO PAULO", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Text(stats.readingRoomName, style = MaterialTheme.typography.headlineLarge)
                    Text(stats.schoolName.ifBlank { "Unidade educacional" },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Card(onClick = { onNavigate("scanner/universal") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ReadingRoomColors.Wine)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(58.dp), tint = ReadingRoomColors.Ivory)
                            Column(Modifier.weight(1f)) {
                                Text("Escanear", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                Text("A câmera é seu balcão de atendimento", style = MaterialTheme.typography.bodyMedium, color = ReadingRoomColors.Ivory)
                            }
                        }
                        Text("Cadastre, empreste, devolva ou confira exemplares usando códigos de barras e QR Codes.",
                            style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Surface(shape = RoundedCornerShape(14.dp), color = ReadingRoomColors.Ivory, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Abrir câmera", color = ReadingRoomColors.Wine, style = MaterialTheme.typography.labelLarge)
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = ReadingRoomColors.Wine)
                            }
                        }
                    }
                }
            }
            item { Text("Atendimento rápido", style = MaterialTheme.typography.titleLarge) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction("Emprestar", "Ler leitor e livro", Icons.Default.Book, Modifier.weight(1f)) { onNavigate("checkout/scan") }
                    QuickAction("Devolver", "Ler exemplar", Icons.Default.AssignmentReturn, Modifier.weight(1f)) { onNavigate("return/scan") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction("Cadastrar", "Identificar pelo ISBN", Icons.Default.LibraryBooks, Modifier.weight(1f)) { onNavigate("book/scan") }
                    QuickAction("Inventário", "Conferir estantes", Icons.Default.Inventory2, Modifier.weight(1f)) { onNavigate("inventory/scan") }
                }
            }
            item { Text("Situação do acervo", style = MaterialTheme.typography.titleLarge) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric("Emprestados", stats.activeLoans, ReadingRoomColors.Blue, Modifier.weight(1f))
                    Metric("Previstas hoje", stats.dueToday, ReadingRoomColors.Blue, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric("Em atraso", stats.overdueLoans, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    Metric("Disponíveis", stats.available, ReadingRoomColors.Green, Modifier.weight(1f))
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Organização da Sala de Leitura", style = MaterialTheme.typography.titleMedium)
                        Text("${stats.editions} obras · ${stats.copies} exemplares registrados",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text("Último inventário: " + (stats.lastInventoryAt?.let {
                            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))
                        } ?: "ainda não realizado"), style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                        TextButton(onClick = { onNavigate("catalog") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.LibraryBooks, contentDescription = null)
                            Spacer(Modifier.width(8.dp)); Text("Consultar acervo")
                        }
                        TextButton(onClick = { onNavigate("people") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.People, contentDescription = null)
                            Spacer(Modifier.width(8.dp)); Text("Pessoas e turmas")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(title: String, description: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier.heightIn(min = 124.dp), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = ReadingRoomColors.Wine, modifier = Modifier.size(29.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Metric(label: String, value: Int, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
