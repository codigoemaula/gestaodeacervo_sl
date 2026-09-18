@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.gov.sp.sme.salaleitura.feature.backup.BackupScreen
import br.gov.sp.sme.salaleitura.feature.catalog.BookFormScreen
import br.gov.sp.sme.salaleitura.feature.catalog.CatalogScreen
import br.gov.sp.sme.salaleitura.feature.catalog.CatalogViewModel
import br.gov.sp.sme.salaleitura.feature.circulation.CheckoutScreen
import br.gov.sp.sme.salaleitura.feature.circulation.CirculationViewModel
import br.gov.sp.sme.salaleitura.feature.circulation.LoansScreen
import br.gov.sp.sme.salaleitura.feature.circulation.ReturnScreen
import br.gov.sp.sme.salaleitura.feature.dashboard.DashboardScreen
import br.gov.sp.sme.salaleitura.feature.inventory.InventoryScreen
import br.gov.sp.sme.salaleitura.feature.inventory.InventoryViewModel
import br.gov.sp.sme.salaleitura.feature.labels.LabelsScreen
import br.gov.sp.sme.salaleitura.feature.people.ClassGroupScreen
import br.gov.sp.sme.salaleitura.feature.people.CsvImportScreen
import br.gov.sp.sme.salaleitura.feature.people.PeopleScreen
import br.gov.sp.sme.salaleitura.feature.people.PersonFormScreen
import br.gov.sp.sme.salaleitura.feature.reports.ReportsScreen
import br.gov.sp.sme.salaleitura.feature.scanner.BarcodeScannerScreen
import br.gov.sp.sme.salaleitura.feature.scanner.ScanResult
import br.gov.sp.sme.salaleitura.feature.settings.SettingsScreen
import br.gov.sp.sme.salaleitura.feature.setup.SchoolSetupScreen

@Composable
fun AppNav(hasSchool: Boolean) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (hasSchool) "dashboard" else "setup") {
        composable("setup") {
            SchoolSetupScreen(
                onFinished = {
                    nav.navigate("dashboard") { popUpTo("setup") { inclusive = true } }
                }
            )
        }
        composable("dashboard") { DashboardScreen(onNavigate = nav::navigate) }

        composable("catalog") {
            CatalogScreen(onBack = { nav.popBackStack() }, onAdd = { nav.navigate("book/new") })
        }
        composable("book/new") { entry ->
            val vm: CatalogViewModel = viewModel(viewModelStoreOwner = entry)
            ConsumeScan(entry, "scan_book") { vm.applyScannedIsbn(it) }
            BookFormScreen(
                onBack = { nav.popBackStack() },
                onScan = { nav.navigate("scanner/book") },
                vm = vm
            )
        }

        composable("people") {
            PeopleScreen(
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate("people/new") },
                onClasses = { nav.navigate("classes") },
                onImport = { nav.navigate("people/import") }
            )
        }
        composable("people/new") { PersonFormScreen(onBack = { nav.popBackStack() }) }
        composable("classes") { ClassGroupScreen(onBack = { nav.popBackStack() }) }
        composable("people/import") { CsvImportScreen(onBack = { nav.popBackStack() }) }

        composable("checkout") { entry ->
            val vm: CirculationViewModel = viewModel(viewModelStoreOwner = entry)
            ConsumeScan(entry, "scan_person") { vm.setPersonCode(it) }
            ConsumeScan(entry, "scan_checkout_copy") { vm.setCopyCode(it) }
            CheckoutScreen(
                onBack = { nav.popBackStack() },
                onScanPerson = { nav.navigate("scanner/person") },
                onScanCopy = { nav.navigate("scanner/checkout_copy") },
                vm = vm
            )
        }
        composable("return") { entry ->
            val vm: CirculationViewModel = viewModel(viewModelStoreOwner = entry)
            ConsumeScan(entry, "scan_return_copy") {
                vm.setReturnCopyCode(it)
                vm.resolveReturn()
            }
            ReturnScreen(onBack = { nav.popBackStack() }, onScanCopy = { nav.navigate("scanner/return_copy") }, vm = vm)
        }
        composable("loans") { LoansScreen(onBack = { nav.popBackStack() }) }

        composable("inventory") { entry ->
            val vm: InventoryViewModel = viewModel(viewModelStoreOwner = entry)
            ConsumeScan(entry, "scan_inventory") {
                vm.setCode(it)
                vm.scan()
            }
            InventoryScreen(onBack = { nav.popBackStack() }, onScan = { nav.navigate("scanner/inventory") }, vm = vm)
        }

        composable("reports") { ReportsScreen(onBack = { nav.popBackStack() }) }
        composable("labels") { LabelsScreen(onBack = { nav.popBackStack() }) }
        composable("backup") { BackupScreen(onBack = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable("more") {
            MoreMenuScreen(
                onBack = { nav.popBackStack() },
                onNavigate = nav::navigate
            )
        }

        composable(
            route = "scanner/{target}",
            arguments = listOf(navArgument("target") { type = NavType.StringType })
        ) { entry ->
            val target = entry.arguments?.getString("target").orEmpty()
            BarcodeScannerScreen(
                title = scannerTitle(target),
                onBack = { nav.popBackStack() },
                onScanned = { result ->
                    val value = when (result) {
                        is ScanResult.Isbn -> result.isbn13
                        is ScanResult.Copy -> result.code
                        is ScanResult.Person -> result.code
                        is ScanResult.Unknown -> result.raw
                    }
                    nav.previousBackStackEntry?.savedStateHandle?.set("scan_$target", value)
                    nav.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun ConsumeScan(entry: NavBackStackEntry, key: String, onValue: (String) -> Unit) {
    val value by entry.savedStateHandle.getStateFlow<String?>(key, null).collectAsStateWithLifecycle()
    LaunchedEffect(value) {
        value?.let {
            onValue(it)
            entry.savedStateHandle[key] = null
        }
    }
}

@Composable
private fun MoreMenuScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mais ferramentas") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = { onNavigate("reports") }, modifier = Modifier.fillMaxWidth()) { Text("Relatórios locais") }
            OutlinedButton(onClick = { onNavigate("labels") }, modifier = Modifier.fillMaxWidth()) { Text("Etiquetas e QR Codes") }
            OutlinedButton(onClick = { onNavigate("backup") }, modifier = Modifier.fillMaxWidth()) { Text("Backup e restauração") }
            OutlinedButton(onClick = { onNavigate("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Configurações e atualização ISBN") }
        }
    }
}

private fun scannerTitle(target: String) = when (target) {
    "book" -> "Escanear ISBN/EAN"
    "person" -> "Escanear pessoa"
    "checkout_copy", "return_copy", "inventory" -> "Escanear exemplar"
    else -> "Escanear código"
}
