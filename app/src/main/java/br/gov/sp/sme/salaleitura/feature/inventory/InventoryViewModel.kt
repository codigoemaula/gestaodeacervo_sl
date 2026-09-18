package br.gov.sp.sme.salaleitura.feature.inventory
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gov.sp.sme.salaleitura.core.logic.InventorySummary
import br.gov.sp.sme.salaleitura.data.local.AppDatabase
import br.gov.sp.sme.salaleitura.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryUiState(val sessionId:Long?=null,val code:String="",val summary:InventorySummary?=null,val message:String?=null,val error:String?=null)
class InventoryViewModel(app:Application):AndroidViewModel(app){
 private val db=AppDatabase.get(app);private val repo=InventoryRepository(db);private val _state=MutableStateFlow(InventoryUiState());val state:StateFlow<InventoryUiState> = _state.asStateFlow()
 init{viewModelScope.launch{db.inventoryDao().activeSession()?.let{_state.value=_state.value.copy(sessionId=it.id,summary=repo.summary(it.id))}}}
 fun setCode(v:String){_state.value=_state.value.copy(code=v,error=null,message=null)}
 fun start()=viewModelScope.launch{runCatching{repo.start()}.onSuccess{_state.value=InventoryUiState(sessionId=it)}.onFailure{_state.value=_state.value.copy(error=it.message)}}
 fun scan()=viewModelScope.launch{val s=_state.value;val id=s.sessionId?:return@launch;val ok=repo.scan(id,s.code.trim());_state.value=s.copy(code="",summary=repo.summary(id),message=if(ok)"Exemplar conferido" else null,error=if(!ok)"Exemplar não encontrado ou já conferido" else null)}
 fun close()=viewModelScope.launch{val id=_state.value.sessionId?:return@launch;val summary=repo.close(id);_state.value=_state.value.copy(sessionId=null,summary=summary,message="Inventário encerrado")}
}
