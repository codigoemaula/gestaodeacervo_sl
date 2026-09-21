@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.settings
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.repository.MetadataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsState(val schoolName:String="",val defaultDays:Int=7,val syncing:Boolean=false,val message:String?=null)
class SettingsViewModel(app:Application):AndroidViewModel(app){private val db=AppDatabase.get(app);private val metadata=MetadataRepository(db);private val _s=MutableStateFlow(SettingsState());val state=_s.asStateFlow();init{viewModelScope.launch{db.schoolDao().get()?.let{_s.value=SettingsState(it.name,it.defaultLoanDays)}}};fun setDays(d:Int)=viewModelScope.launch{require(d==7||d==14){"Prazo deve ser 7 ou 14 dias"};val school=db.schoolDao().get()?:return@launch;db.schoolDao().upsert(school.copy(defaultLoanDays=d));_s.value=_s.value.copy(defaultDays=d,message="Prazo padrão atualizado")};fun syncIsbn()=viewModelScope.launch{_s.value=_s.value.copy(syncing=true,message=null);val ok=metadata.refreshIsbnRanges();_s.value=_s.value.copy(syncing=false,message=if(ok)"Faixas ISBN atualizadas" else "Não foi possível atualizar; a base local continua disponível")}}
@Composable fun SettingsScreen(onBack:()->Unit,vm:SettingsViewModel=viewModel()){val s by vm.state.collectAsState();Scaffold(topBar={TopAppBar(title={Text("Configurações")},navigationIcon={TextButton(onClick=onBack){Text("Voltar")}})}){p->Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(s.schoolName,style=MaterialTheme.typography.titleLarge);Text("Prazo padrão");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(s.defaultDays==7,{vm.setDays(7)},label={Text("7 dias")});FilterChip(s.defaultDays==14,{vm.setDays(14)},label={Text("14 dias")})};HorizontalDivider();Button(onClick=vm::syncIsbn,enabled=!s.syncing,modifier=Modifier.fillMaxWidth()){Text(if(s.syncing)"Atualizando..." else "Atualizar faixas ISBN")};s.message?.let{Text(it)}}}}
