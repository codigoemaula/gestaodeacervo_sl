package br.gov.sp.sme.salaleitura.feature.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Continuous camera with a decision panel; choosing an action never commits circulation. */
@Composable
fun UniversalScannerScreen(
    onBack: () -> Unit,
    onAction: (UniversalScanAction, String) -> Unit,
    vm: UniversalScannerViewModel = viewModel()
) {
    val state by vm.scan.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        BarcodeScannerScreen(title = "Escanear · Sala de Leitura", onBack = onBack, onScanned = vm::accept)
        state?.let { result ->
            ElevatedCard(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(result.title, style = MaterialTheme.typography.titleLarge)
                    Text(result.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (result.resolving) LinearProgressIndicator(Modifier.fillMaxWidth())
                    result.actions.forEach { action ->
                        Button(onClick = { onAction(action, result.code) }, modifier = Modifier.fillMaxWidth()) {
                            Text(action.label())
                        }
                    }
                    OutlinedButton(onClick = vm::dismiss, enabled = !result.resolving, modifier = Modifier.fillMaxWidth()) {
                        Text("Ler outro código")
                    }
                }
            }
        }
    }
}

private fun UniversalScanAction.label(): String = when (this) {
    UniversalScanAction.CHECKOUT_COPY -> "Emprestar este exemplar"
    UniversalScanAction.RETURN_COPY -> "Devolver este exemplar"
    UniversalScanAction.CHECKOUT_PERSON -> "Emprestar para este leitor"
    UniversalScanAction.REGISTER_ISBN -> "Abrir cadastro da obra"
    UniversalScanAction.CATALOG -> "Consultar acervo"
    UniversalScanAction.MANUAL_SEARCH -> "Buscar manualmente"
}
