@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.feature.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** No online account or separate password; re-prompt only after 5 minutes away. */
@Composable
fun OperatorGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val keyguard = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    var unlocked by remember { mutableStateOf(false) }
    var backgroundAt by remember { mutableStateOf<Long?>(null) }
    var automaticPrompted by remember { mutableStateOf(false) }
    var prompting by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    val authentication = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        prompting = false
        if (result.resultCode == Activity.RESULT_OK) {
            unlocked = true
            backgroundAt = null
            message = null
        } else message = "Acesso não autorizado. Use o bloqueio de tela do aparelho para continuar."
    }
    val securitySettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh++
        automaticPrompted = false
    }
    val secure = keyguard.isDeviceSecure
    fun askDeviceCredential() {
        if (prompting || !keyguard.isDeviceSecure) return
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            "Sala de Leitura", "Confirme o desbloqueio do aparelho para acessar os cadastros."
        )
        if (intent == null) {
            message = "Não foi possível solicitar o desbloqueio. Verifique a segurança do aparelho."
            return
        }
        prompting = true
        authentication.launch(intent)
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (unlocked) backgroundAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> {
                    refresh++
                    if (LockTimeoutPolicy.requiresUnlock(unlocked, backgroundAt, SystemClock.elapsedRealtime())) {
                        unlocked = false
                        automaticPrompted = false
                    }
                    backgroundAt = null
                }
                else -> Unit
            }
        }
        (activity as androidx.lifecycle.LifecycleOwner).lifecycle.addObserver(observer)
        onDispose { (activity as androidx.lifecycle.LifecycleOwner).lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(secure, unlocked, refresh) {
        if (!unlocked && secure && !automaticPrompted && !prompting) {
            automaticPrompted = true
            askDeviceCredential()
        }
    }

    if (unlocked) {
        content()
    } else {
        Scaffold(topBar = { TopAppBar(title = { Text("Sala de Leitura · acesso do educador") }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Os cadastros ficam neste aparelho. O desbloqueio do Android protege o acesso sem criar uma conta ou depender de internet.")
                if (!secure) {
                    Text("Configure uma senha, PIN ou outro bloqueio de tela seguro no Android para utilizar os dados pessoais.", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { securitySettings.launch(Intent(Settings.ACTION_SECURITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Configurar proteção do aparelho") }
                } else {
                    Button(onClick = { automaticPrompted = true; askDeviceCredential() }, enabled = !prompting, modifier = Modifier.fillMaxWidth()) { Text("Desbloquear e continuar") }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Após cinco minutos fora do aplicativo, será solicitada uma nova confirmação. A navegação entre telas não pede senha novamente.")
            }
        }
    }
}
