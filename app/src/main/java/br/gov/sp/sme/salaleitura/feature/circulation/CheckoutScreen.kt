@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.circulation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.logic.LoanTimeline
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import java.time.Instant
import java.time.ZoneId
import java.text.DateFormat
import java.util.Date

@Composable
fun CheckoutScreen(onBack: () -> Unit, onScanPerson: () -> Unit, onScanCopy: () -> Unit, vm: CirculationViewModel = viewModel()) {
    val s by vm.checkout.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Empréstimo individual") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } })
    }) { p ->
        Column(Modifier.padding(p).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("1. Identifique a pessoa", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(s.personCode, vm::setPersonCode, label = { Text("Código da pessoa") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onScanPerson) { Text("Câmera · leitor") }
                if (s.person != null) TextButton(onClick = vm::changeReader) { Text("Trocar leitor") }
            }
            s.person?.let { Text("Leitor: ${it.name}", style = MaterialTheme.typography.titleMedium) }
            HorizontalDivider()
            Text("2. Identifique um exemplar", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(s.copyCode, vm::setCopyCode, label = { Text("Código do exemplar") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onScanCopy, modifier = Modifier.fillMaxWidth()) { Text("Câmera · escanear exemplar") }
            Button(onClick = vm::resolveCheckout, modifier = Modifier.fillMaxWidth(), enabled = s.personCode.isNotBlank() && s.copyCode.isNotBlank()) { Text("Conferir empréstimo") }
            s.copy?.let { Text("Exemplar: ${it.internalCode} · ${it.status.name}") }
            Text("3. Prazo deste exemplar", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.period == LoanPeriod.SEVEN, { vm.selectPeriod(LoanPeriod.SEVEN) }, label = { Text("7 dias") })
                FilterChip(s.period == LoanPeriod.FOURTEEN, { vm.selectPeriod(LoanPeriod.FOURTEEN) }, label = { Text("14 dias") })
            }
            if (s.copy != null && s.person != null) {
                val due = LoanTimeline.dueAt(Instant.now(), s.period, ZoneId.of("America/Sao_Paulo"))
                Text("Devolução prevista: ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(due))}")
            }
            if (s.overdue.isNotEmpty()) {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Há ${s.overdue.size} livro(s) em atraso", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        s.overdue.forEach { loan ->
                            Text("${loan.bookTitle} · previsto para ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(loan.dueAt))}", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Row {
                            Checkbox(checked = s.teacherConfirmedException, onCheckedChange = vm::confirmOverdueException)
                            Text("Autorizo excepcionalmente este empréstimo, mesmo com as devoluções em atraso. A decisão será registrada.", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(
                onClick = vm::checkout,
                enabled = s.person != null && s.copy != null && (s.overdue.isEmpty() || s.teacherConfirmedException),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirmar empréstimo deste exemplar") }
            if (s.message != null && s.copy == null && s.person != null) {
                FilledTonalButton(onClick = onScanCopy, modifier = Modifier.fillMaxWidth()) { Text("Escanear próximo exemplar") }
            }
        }
    }
}
