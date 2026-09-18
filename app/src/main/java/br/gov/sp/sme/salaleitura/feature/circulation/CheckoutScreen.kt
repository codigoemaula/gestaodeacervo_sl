@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.circulation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.model.LoanPeriod

@Composable fun CheckoutScreen(onBack:()->Unit,onScanPerson:()->Unit,onScanCopy:()->Unit,vm:CirculationViewModel=viewModel()){
 val s by vm.checkout.collectAsStateWithLifecycle()
 Scaffold(topBar={TopAppBar(title={Text("Empréstimo")},navigationIcon={TextButton(onClick=onBack){Text("Voltar")}})}){p->
  Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   OutlinedTextField(s.personCode,vm::setPersonCode,label={Text("Código da pessoa")},modifier=Modifier.fillMaxWidth()); OutlinedButton(onClick=onScanPerson){Text("Escanear pessoa")}
   OutlinedTextField(s.copyCode,vm::setCopyCode,label={Text("Código do exemplar")},modifier=Modifier.fillMaxWidth()); OutlinedButton(onClick=onScanCopy){Text("Escanear exemplar")}
   Button(onClick=vm::resolveCheckout,modifier=Modifier.fillMaxWidth()){Text("Localizar")}
   s.person?.let{Text("Pessoa: ${it.name}")}; s.copy?.let{Text("Exemplar: ${it.internalCode} • ${it.status.name}")}
   Text("Prazo deste empréstimo"); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(s.period==LoanPeriod.SEVEN,{vm.selectPeriod(LoanPeriod.SEVEN)},label={Text("7 dias")});FilterChip(s.period==LoanPeriod.FOURTEEN,{vm.selectPeriod(LoanPeriod.FOURTEEN)},label={Text("14 dias")})}
   s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}; s.message?.let{Text(it,color=MaterialTheme.colorScheme.primary)}
   Button(onClick=vm::checkout,enabled=s.person!=null&&s.copy!=null,modifier=Modifier.fillMaxWidth()){Text("Confirmar empréstimo")}
  }
 }
}
