@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.circulation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.logic.LoanTimeline
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import br.gov.sp.sme.salaleitura.core.model.PersonType
import br.gov.sp.sme.salaleitura.feature.people.ReaderAvatar
import java.time.Instant
import java.time.ZoneId
import java.text.DateFormat
import java.util.Date

@Composable
fun CheckoutScreen(onBack: () -> Unit, onScanCopy: () -> Unit, onManageReaders: () -> Unit, vm: CirculationViewModel = viewModel()) {
    val s by vm.checkout.collectAsStateWithLifecycle()
    val groups by vm.classes.collectAsStateWithLifecycle()
    val people by vm.readers.collectAsStateWithLifecycle()
    val editions by vm.editions.collectAsStateWithLifecycle()
    var search by remember(s.selectedClassId) { mutableStateOf("") }
    var bookSearch by remember { mutableStateOf("") }
    var showCatalog by remember { mutableStateOf(false) }
    val matching = people.filter { person ->
        person.active && when (s.selectedClassId) {
            ReaderGroup.PROFESSIONALS -> person.type == PersonType.PROFESSIONAL
            ReaderGroup.WITHOUT_CLASS -> person.type == PersonType.STUDENT && person.classGroupId == null
            null -> false
            else -> person.type == PersonType.STUDENT && person.classGroupId == s.selectedClassId
        } && person.name.contains(search.trim(), ignoreCase = true)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Empréstimo de obra") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { p ->
        Column(Modifier.padding(p).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("1. Selecione a sala ou turma", style = MaterialTheme.typography.titleMedium)
            if (groups.isEmpty()) Text("Nenhuma turma cadastrada. Crie uma turma em Salas e turmas ou escolha Profissionais.")
            groups.forEach { group ->
                FilterChip(selected = s.selectedClassId == group.id, onClick = { vm.selectGroup(group.id) },
                    label = { Text("${group.name} · ${group.schoolYear}") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = s.selectedClassId == ReaderGroup.PROFESSIONALS,
                    onClick = { vm.selectGroup(ReaderGroup.PROFESSIONALS) }, label = { Text("Profissionais") })
                if (people.any { it.active && it.type == PersonType.STUDENT && it.classGroupId == null }) {
                    FilterChip(selected = s.selectedClassId == ReaderGroup.WITHOUT_CLASS,
                        onClick = { vm.selectGroup(ReaderGroup.WITHOUT_CLASS) }, label = { Text("Sem turma") })
                }
            }
            TextButton(onClick = onManageReaders) { Text("Criar turma ou cadastrar estudante") }
            if (s.selectedClassId != null) {
                Text("2. Selecione o leitor", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Buscar nome nesta turma") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (matching.isEmpty()) Text("Nenhum cadastro nesta lista. Você pode adicionar estudantes em Salas e turmas.")
                matching.take(60).forEach { reader ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { vm.selectReader(reader) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (s.person?.id == reader.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ReaderAvatar(reader.photoFilename)
                            Text(reader.name, modifier = Modifier.weight(1f))
                            if (s.person?.id == reader.id) Text("Selecionado", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (matching.size > 60) Text("Refine a busca para encontrar mais nomes.", style = MaterialTheme.typography.bodySmall)
            }
            s.person?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderAvatar(it.photoFilename)
                    Text("Leitor: ${it.name}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = vm::changeReader) { Text("Trocar") }
                }
            }
            HorizontalDivider()
            Text("3. Identifique o exemplar", style = MaterialTheme.typography.titleMedium)
            Text("O código interno ou patrimônio identifica um exemplar. O ISBN identifica a edição; se houver vários exemplares, escolha o volume físico abaixo.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(s.copyCode, vm::setCopyCode, label = { Text("Código do exemplar, patrimônio ou ISBN") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onScanCopy, modifier = Modifier.fillMaxWidth()) { Text("Câmera · ler somente exemplar ou ISBN") }
            Button(onClick = vm::resolveCheckout, modifier = Modifier.fillMaxWidth(), enabled = s.person != null && s.copyCode.isNotBlank()) {
                Text("Localizar exemplar no acervo")
            }
            OutlinedButton(onClick = { showCatalog = !showCatalog }, enabled = s.person != null, modifier = Modifier.fillMaxWidth()) {
                Text(if (showCatalog) "Ocultar obras do acervo" else "Selecionar obra cadastrada no acervo")
            }
            if (showCatalog && s.person != null) {
                OutlinedTextField(bookSearch, { bookSearch = it }, label = { Text("Buscar por título, autoria ou ISBN") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                val found = editions.filter { edition ->
                    bookSearch.isBlank() || listOfNotNull(edition.title, edition.authors, edition.isbn13, edition.isbn10)
                        .any { it.contains(bookSearch.trim(), ignoreCase = true) }
                }
                if (found.isEmpty()) Text("Nenhuma obra cadastrada corresponde à busca.")
                found.take(20).forEach { edition ->
                    OutlinedButton(onClick = { vm.selectEditionForCheckout(edition.id); showCatalog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("${edition.title}${edition.isbn13?.let { " · $it" }.orEmpty()}")
                    }
                }
                if (found.size > 20) Text("Refine a busca para localizar a obra.", style = MaterialTheme.typography.bodySmall)
            }
            if (s.candidateCopies.isNotEmpty()) {
                Text("Escolha o exemplar físico: ${s.candidateTitle.orEmpty()}", style = MaterialTheme.typography.titleMedium)
                s.candidateCopies.forEach { candidate ->
                    OutlinedButton(onClick = { vm.chooseCopy(candidate.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Exemplar ${candidate.sequence} · ${candidate.internalCode}${candidate.location?.let { " · $it" }.orEmpty()}")
                    }
                }
            }
            s.copy?.let { Text("Exemplar selecionado: ${it.internalCode} · ${it.status.name}") }
            Text("4. Prazo deste exemplar", style = MaterialTheme.typography.titleMedium)
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
                            Text("Autorizo excepcionalmente este empréstimo, mesmo com devoluções em atraso. A decisão será registrada.", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(onClick = vm::checkout,
                enabled = s.person != null && s.copy != null && s.error == null && (s.overdue.isEmpty() || s.teacherConfirmedException),
                modifier = Modifier.fillMaxWidth()) {
                Text("Confirmar empréstimo deste exemplar")
            }
            if (s.message != null && s.copy == null && s.person != null && s.candidateCopies.isEmpty()) {
                FilledTonalButton(onClick = onScanCopy, modifier = Modifier.fillMaxWidth()) { Text("Escanear próximo exemplar") }
            }
        }
    }
}
