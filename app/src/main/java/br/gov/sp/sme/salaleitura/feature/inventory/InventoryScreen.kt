@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.inventory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
@Composable fun InventoryScreen(onBack:()->Unit,onScan:()->Unit,vm:InventoryViewModel=viewModel()){
 val s by vm.state.collectAsStateWithLifecycle()
 Scaffold(topBar={TopAppBar(title={Text("Inventário")},navigationIcon={TextButton(onClick=onBack){Text("Voltar")}})}){p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  if(s.sessionId==null) Button(onClick=vm::start,modifier=Modifier.fillMaxWidth()){Text("Iniciar inventário")} else {Text("Sessão #${s.sessionId}");OutlinedTextField(s.code,vm::setCode,label={Text("Código do exemplar")},modifier=Modifier.fillMaxWidth());Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=vm::scan,modifier=Modifier.weight(1f)){Text("Conferir")};OutlinedButton(onClick=onScan,modifier=Modifier.weight(1f)){Text("Câmera")}};Button(onClick=vm::close,modifier=Modifier.fillMaxWidth()){Text("Encerrar inventário")}}
  s.summary?.let{r->Card{Column(Modifier.padding(16.dp)){Text("Cadastrados: ${r.registered}");Text("Localizados: ${r.found}");Text("Emprestados: ${r.loaned}");Text("Não localizados: ${r.missing}");Text("Danificados: ${r.damaged}");Text("Baixados: ${r.withdrawn}")}}};s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)};s.message?.let{Text(it,color=MaterialTheme.colorScheme.primary)}
 }}
}
