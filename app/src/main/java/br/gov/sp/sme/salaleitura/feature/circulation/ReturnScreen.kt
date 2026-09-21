@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.circulation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.model.CopyStatus

@Composable fun ReturnScreen(onBack:()->Unit,onScanCopy:()->Unit,vm:CirculationViewModel=viewModel()){
 val s by vm.returns.collectAsStateWithLifecycle(); var status by remember{mutableStateOf(CopyStatus.AVAILABLE)}
 Scaffold(topBar={TopAppBar(title={Text("Devolução")},navigationIcon={TextButton(onClick=onBack){Text("Voltar")}})}){p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  OutlinedTextField(s.copyCode,vm::setReturnCopyCode,label={Text("Código do exemplar")},modifier=Modifier.fillMaxWidth());OutlinedButton(onClick=onScanCopy){Text("Escanear exemplar")};Button(onClick=vm::resolveReturn,modifier=Modifier.fillMaxWidth()){Text("Localizar empréstimo")}
  s.person?.let{Text("Emprestado para: ${it.name}")}; s.copy?.let{Text("Exemplar: ${it.internalCode}")}
  if(s.loanId!=null){Text("Condição após devolução"); listOf(CopyStatus.AVAILABLE to "Disponível",CopyStatus.DAMAGED to "Danificado",CopyStatus.MAINTENANCE to "Manutenção",CopyStatus.WITHDRAWN to "Baixado").forEach{(v,l)->FilterChip(status==v,{status=v},label={Text(l)})}}
  s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)};s.message?.let{Text(it,color=MaterialTheme.colorScheme.primary)}
  Button(onClick={vm.returnCopy(status)},enabled=s.loanId!=null,modifier=Modifier.fillMaxWidth()){Text("Confirmar devolução")}
 }}
}
