@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.circulation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod
import java.text.DateFormat
import java.util.Date

@Composable fun LoansScreen(onBack:()->Unit,vm:CirculationViewModel=viewModel()){
 val rows by vm.activeLoans.collectAsStateWithLifecycle(); val now=System.currentTimeMillis()
 Scaffold(topBar={TopAppBar(title={Text("Empréstimos e pendências")},navigationIcon={TextButton(onClick=onBack){Text("Voltar")}})}){p->
  if(rows.isEmpty()) Box(Modifier.padding(p).padding(24.dp)){Text("Nenhum empréstimo ativo.")}
  else LazyColumn(Modifier.padding(p)){items(rows,key={it.loanId}){r->Card(Modifier.padding(8.dp)){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(r.bookTitle,style=MaterialTheme.typography.titleMedium);Text("${r.personName} • ${r.copyCode}");Text("Devolução: ${DateFormat.getDateInstance().format(Date(r.dueAt))}" + if(r.dueAt<now) " • VENCIDO" else "");Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton(onClick={vm.renew(r.loanId,LoanPeriod.SEVEN)}){Text("Renovar 7")};TextButton(onClick={vm.renew(r.loanId,LoanPeriod.FOURTEEN)}){Text("Renovar 14")}}}}}}
 }
}
