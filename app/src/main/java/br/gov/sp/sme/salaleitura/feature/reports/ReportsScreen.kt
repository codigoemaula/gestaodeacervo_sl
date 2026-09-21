@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.reports
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.core.model.CopyStatus
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReportState(val editions:Int=0,val copies:Int=0,val active:Int=0,val overdue:Int=0,val available:Int=0,val damaged:Int=0,val lost:Int=0)
class ReportsViewModel(app:Application):AndroidViewModel(app){private val db=AppDatabase.get(app);private val _s=MutableStateFlow(ReportState());val state=_s.asStateFlow();init{viewModelScope.launch{val c=db.catalogDao();val l=db.loanDao();_s.value=ReportState(c.editionCount(),c.copyCount(),l.activeCount(),l.overdueCount(System.currentTimeMillis()),c.countByStatus(CopyStatus.AVAILABLE),c.countByStatus(CopyStatus.DAMAGED),c.countByStatus(CopyStatus.LOST))}}}
@Composable fun ReportsScreen(onBack:()->Unit,vm:ReportsViewModel=viewModel()){val s by vm.state.collectAsStateWithLifecycle();Scaffold(topBar={TopAppBar(title={Text("Relatórios locais")},navigationIcon={TextButton(onClick=onBack){Text("Voltar")}})}){p->Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Obras: ${s.editions}");Text("Exemplares: ${s.copies}");Text("Disponíveis: ${s.available}");Text("Empréstimos ativos: ${s.active}");Text("Vencidos: ${s.overdue}");Text("Danificados: ${s.damaged}");Text("Extraviados: ${s.lost}")}}}
