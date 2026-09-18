@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.backup

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable fun BackupScreen(onBack:()->Unit){
 val context=LocalContext.current;val manager=remember{BackupManager(context)};val scope=rememberCoroutineScope();var message by remember{mutableStateOf<String?>(null)};var pending by remember{mutableStateOf<Pair<Uri,BackupManifest>?>(null)}
 val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->uri?.let{scope.launch{message=manager.exportTo(it).fold({"Backup criado com sucesso"},{"Falha: ${it.message}"})}}}
 val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{scope.launch{manager.inspect(it).onSuccess{m->pending=it to m}.onFailure{e->message="Backup inválido: ${e.message}"}}}}
 if(pending!=null){val (uri,m)=pending!!;AlertDialog(onDismissRequest={pending=null},title={Text("Confirmar restauração")},text={Text("Substituir todos os dados atuais por '${m.schoolName}' (EOL ${m.schoolEol}), backup de ${DateFormat.getDateTimeInstance().format(Date(m.createdAt))}?")},confirmButton={TextButton(onClick={pending=null;scope.launch{manager.restore(uri).onSuccess{message="Restauração concluída";(context as? Activity)?.recreate()}.onFailure{message="Falha: ${it.message}"}}}){Text("Restaurar")}},dismissButton={TextButton(onClick={pending=null}){Text("Cancelar")}})}
 Scaffold(topBar={TopAppBar(title={Text("Backup e restauração")},navigationIcon={TextButton(onClick=onBack){Text("Voltar")}})}){p->Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("O backup permanece sob seu controle e não é enviado automaticamente à nuvem.");Button(onClick={scope.launch{create.launch(manager.suggestedName())}},modifier=Modifier.fillMaxWidth()){Text("Exportar .slbackup")};OutlinedButton(onClick={open.launch(arrayOf("application/zip","application/octet-stream","*/*"))},modifier=Modifier.fillMaxWidth()){Text("Restaurar backup")};message?.let{Text(it)}}}
}
