package br.gov.sp.sme.salaleitura.feature.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.gov.sp.sme.salaleitura.feature.scanner.BarcodeScannerScreen
import br.gov.sp.sme.salaleitura.feature.scanner.ScanResult

/** Camera remains live. InventoryRepository ignores duplicate copies per active session. */
@Composable
fun InventoryContinuousScannerScreen(onBack: () -> Unit, vm: InventoryViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        BarcodeScannerScreen(title = "Inventário · conferência contínua", onBack = onBack, onScanned = { result ->
            val code = when (result) {
                is ScanResult.Copy -> result.code
                is ScanResult.Person -> result.code
                is ScanResult.Isbn -> result.isbn13
                is ScanResult.Unknown -> result.raw
            }
            vm.scanCode(code)
        })
        ElevatedCard(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sessão #${state.sessionId ?: "encerrada"} · câmera contínua", style = MaterialTheme.typography.titleMedium)
                state.summary?.let { Text("${it.found} de ${it.registered} exemplares conferidos", style = MaterialTheme.typography.bodyMedium) }
                state.lastScanned?.let { Text("Última leitura: $it", style = MaterialTheme.typography.bodySmall) }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Aponte para outro exemplar. ISBN comercial não identifica uma cópia específica.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Pausar câmera e voltar ao inventário") }
            }
        }
    }
}
