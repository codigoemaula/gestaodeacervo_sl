@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { BackupManager(context) }
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var exportPassword by remember { mutableStateOf("") }
    var exportConfirmation by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var allowLegacy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pair<Uri, BackupManifest>?>(null) }
    var busy by remember { mutableStateOf(false) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            val secret = exportPassword.toCharArray()
            exportPassword = ""
            exportConfirmation = ""
            busy = true
            scope.launch {
                try {
                    message = manager.exportTo(uri, secret).fold({ "Backup criptografado criado, incluindo fotos opcionais." }, { "Falha: ${it.message}" })
                } finally { secret.fill('\u0000'); busy = false }
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val secret = restorePassword.toCharArray()
            busy = true
            scope.launch {
                try {
                    manager.inspect(uri, secret, allowLegacy).onSuccess { pending = uri to it }
                        .onFailure { message = "Backup inválido ou senha incorreta: ${it.message}" }
                } finally { secret.fill('\u0000'); busy = false }
            }
        }
    }
    if (pending != null) {
        val (uri, manifest) = pending!!
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Confirmar restauração") },
            text = { Text("Substituir TODOS os dados atuais por '${manifest.schoolName}', backup de ${DateFormat.getDateTimeInstance().format(Date(manifest.createdAt))}? ${if (manifest.schemaVersion >= 2) "As fotografias também serão restauradas." else "ATENÇÃO: backup antigo sem fotos e sem criptografia; fotografias anteriores podem ficar inconsistentes."} Faça uma cópia de segurança atual antes de continuar.") },
            confirmButton = { TextButton(onClick = {
                pending = null
                val secret = restorePassword.toCharArray()
                restorePassword = ""
                busy = true
                scope.launch {
                    try {
                        manager.restore(uri, secret, allowLegacy).onSuccess {
                            message = "Restauração concluída."
                            // Destroy the old Activity and retained ViewModels, which reference the closed database.
                            val entry = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            if (entry != null) {
                                entry.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(entry)
                                (context as? Activity)?.finish()
                            } else (context as? Activity)?.recreate()
                        }.onFailure { message = "Falha: ${it.message}" }
                    } finally { secret.fill('\u0000'); busy = false }
                }
            }) { Text("Restaurar") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } }
        )
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Backup e restauração") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Backup manual completo: banco de dados e fotografias opcionais. O arquivo é protegido com senha e criptografia; não há envio automático para a nuvem.")
            Text("Guarde a senha separadamente. Sem ela, não é possível recuperar um backup criptografado.")
            OutlinedTextField(exportPassword, { exportPassword = it }, label = { Text("Criar senha do backup (mínimo de 10 caracteres)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(exportConfirmation, { exportConfirmation = it }, label = { Text("Confirmar senha") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { scope.launch { create.launch(manager.suggestedName()) } }, enabled = !busy && exportPassword.length >= 10 && exportPassword == exportConfirmation, modifier = Modifier.fillMaxWidth()) { Text("Exportar backup criptografado") }
            HorizontalDivider()
            Text("Restaurar um backup", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(restorePassword, { restorePassword = it }, label = { Text("Senha do backup") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            Row { Checkbox(checked = allowLegacy, onCheckedChange = { allowLegacy = it }); Text("Importar backup antigo SEM criptografia (somente para migrar arquivos anteriores)", modifier = Modifier.padding(top = 10.dp)) }
            if (allowLegacy) Text("Arquivo antigo pode conter dados pessoais legíveis e não inclui fotos. Após importar, crie imediatamente um novo backup criptografado.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { open.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, enabled = !busy && (restorePassword.length >= 10 || allowLegacy), modifier = Modifier.fillMaxWidth()) { Text("Selecionar arquivo para restauração") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it) }
        }
    }
}
