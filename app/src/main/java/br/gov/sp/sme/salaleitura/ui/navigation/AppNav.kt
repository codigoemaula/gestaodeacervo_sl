@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.gov.sp.sme.salaleitura.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.gov.sp.sme.salaleitura.feature.backup.BackupScreen
import br.gov.sp.sme.salaleitura.feature.catalog.BookFormScreen
import br.gov.sp.sme.salaleitura.feature.catalog.BookScanRoute
import br.gov.sp.sme.salaleitura.feature.catalog.CatalogScreen
import br.gov.sp.sme.salaleitura.feature.catalog.CatalogViewModel
import br.gov.sp.sme.salaleitura.feature.circulation.CheckoutScreen
import br.gov.sp.sme.salaleitura.feature.circulation.CirculationViewModel
import br.gov.sp.sme.salaleitura.feature.circulation.LoansScreen
import br.gov.sp.sme.salaleitura.feature.circulation.ReturnScreen
import br.gov.sp.sme.salaleitura.feature.dashboard.DashboardScreen
import br.gov.sp.sme.salaleitura.feature.dashboard.HomeRoutes
import br.gov.sp.sme.salaleitura.feature.inventory.InventoryScreen
import br.gov.sp.sme.salaleitura.feature.inventory.InventoryViewModel
import br.gov.sp.sme.salaleitura.feature.labels.LabelsScreen
import br.gov.sp.sme.salaleitura.feature.legal.AboutScreen
import br.gov.sp.sme.salaleitura.feature.legal.TermsPrivacyScreen
import br.gov.sp.sme.salaleitura.feature.people.ClassGroupScreen
import br.gov.sp.sme.salaleitura.feature.people.CsvImportScreen
import br.gov.sp.sme.salaleitura.feature.people.PeopleScreen
import br.gov.sp.sme.salaleitura.feature.people.PersonFormScreen
import br.gov.sp.sme.salaleitura.feature.reports.ReportsScreen
import br.gov.sp.sme.salaleitura.feature.scanner.BarcodeScannerScreen
import br.gov.sp.sme.salaleitura.feature.scanner.ScanResult
import br.gov.sp.sme.salaleitura.feature.scanner.UniversalScanAction
import br.gov.sp.sme.salaleitura.feature.scanner.UniversalScannerScreen
import br.gov.sp.sme.salaleitura.feature.settings.SettingsScreen
import br.gov.sp.sme.salaleitura.feature.setup.SchoolSetupScreen

@Composable
fun AppNav(hasSchool: Boolean) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route.orEmpty()
    Scaffold(bottomBar = {
        if (route in setOf("dashboard", "loans", "catalog", "more", "people")) {
            ReadingRoomBottomBar(route) { destination ->
                nav.navigate(destination) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo("dashboard") { saveState = true }
                }
            }
        }
    }) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = if (hasSchool) "dashboard" else "setup",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("setup") {
                SchoolSetupScreen(onFinished = { nav.navigate("dashboard") { popUpTo("setup") { inclusive = true } } })
            }
            composable("dashboard") {
                DashboardScreen(onNavigate = { action -> HomeRoutes.destinations(action).forEach { nav.navigate(it) } })
            }
            composable("catalog") { CatalogScreen(onBack = { nav.popBackStack() }, onAdd = { nav.navigate("book/new") }) }
            composable("book/new?isbn={isbn}", arguments = listOf(navArgument("isbn") {
                type = NavType.StringType; defaultValue = ""
            })) { entry ->
                val vm: CatalogViewModel = viewModel(viewModelStoreOwner = entry)
                ConsumeScan(entry, "scan_book") { vm.applyScannedIsbn(it) }
                val entryIsbn = entry.arguments?.getString("isbn").orEmpty()
                LaunchedEffect(entryIsbn) { if (entryIsbn.isNotEmpty()) vm.applyScannedIsbn(entryIsbn) }
                BookFormScreen(onBack = { nav.popBackStack() }, onScan = { nav.navigate("scanner/book") }, vm = vm)
            }
            composable("people") {
                PeopleScreen(onBack = { nav.popBackStack() }, onAdd = { nav.navigate("people/new") },
                    onClasses = { nav.navigate("classes") }, onImport = { nav.navigate("people/import") },
                    onEdit = { nav.navigate("people/edit/$it") })
            }
            composable("people/new?classId={classId}", arguments = listOf(navArgument("classId") {
                type = NavType.LongType; defaultValue = -1L
            })) { entry ->
                val classId = entry.arguments?.getLong("classId")?.takeIf { it > 0 }
                PersonFormScreen(onBack = { nav.popBackStack() }, initialClassId = classId)
            }
            composable("people/edit/{personId}", arguments = listOf(navArgument("personId") { type = NavType.LongType })) { entry ->
                PersonFormScreen(onBack = { nav.popBackStack() }, personId = entry.arguments?.getLong("personId"))
            }
            composable("classes") {
                ClassGroupScreen(onBack = { nav.popBackStack() }, onAddToClass = { nav.navigate("people/new?classId=$it") })
            }
            composable("people/import") { CsvImportScreen(onBack = { nav.popBackStack() }) }
            composable("checkout") { entry ->
                val vm: CirculationViewModel = viewModel(viewModelStoreOwner = entry)
                ConsumeScan(entry, "scan_checkout_copy") { vm.setCopyCode(it) }
                CheckoutScreen(onBack = { nav.popBackStack() }, onScanCopy = { nav.navigate("scanner/checkout_copy") },
                    onManageReaders = { nav.navigate("classes") }, vm = vm)
            }
            composable("return") { entry ->
                val vm: CirculationViewModel = viewModel(viewModelStoreOwner = entry)
                ConsumeScan(entry, "scan_return_copy") { vm.setReturnCopyCode(it); vm.resolveReturn() }
                ReturnScreen(onBack = { nav.popBackStack() }, onScanCopy = { nav.navigate("scanner/return_copy") }, vm = vm)
            }
            composable("loans") { LoansScreen(onBack = { nav.popBackStack() }) }
            composable("inventory") { entry ->
                val vm: InventoryViewModel = viewModel(viewModelStoreOwner = entry)
                ConsumeScan(entry, "scan_inventory") { vm.setCode(it); vm.scan() }
                InventoryScreen(onBack = { nav.popBackStack() }, onScan = { nav.navigate("scanner/inventory") }, vm = vm)
            }
            composable("reports") { ReportsScreen(onBack = { nav.popBackStack() }) }
            composable("labels") { LabelsScreen(onBack = { nav.popBackStack() }) }
            composable("backup") { BackupScreen(onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable("terms-privacy") { TermsPrivacyScreen(onBack = { nav.popBackStack() }) }
            composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
            composable("more") { MoreMenuScreen(onBack = { nav.popBackStack() }, onNavigate = nav::navigate) }
            composable(route = "scanner/{target}", arguments = listOf(navArgument("target") { type = NavType.StringType })) { entry ->
                val target = entry.arguments?.getString("target").orEmpty()
                when (target) {
                    "universal" -> UniversalScannerScreen(
                        onBack = { nav.popBackStack() },
                        onAction = { action, code ->
                            nav.popBackStack()
                            when (action) {
                                UniversalScanAction.CHECKOUT_COPY -> {
                                    nav.navigate("checkout")
                                    nav.currentBackStackEntry?.savedStateHandle?.set("scan_checkout_copy", code)
                                }
                                UniversalScanAction.RETURN_COPY -> {
                                    nav.navigate("return")
                                    nav.currentBackStackEntry?.savedStateHandle?.set("scan_return_copy", code)
                                }
                                UniversalScanAction.REGISTER_ISBN -> nav.navigate(BookScanRoute.forIsbn(code))
                                UniversalScanAction.CATALOG, UniversalScanAction.MANUAL_SEARCH -> nav.navigate("catalog")
                            }
                        }
                    )
                    // No route may launch a camera to identify a student or professional.
                    "person" -> MoreMenuScreen(onBack = { nav.popBackStack() }, onNavigate = nav::navigate)
                    else -> BarcodeScannerScreen(
                        title = scannerTitle(target), onBack = { nav.popBackStack() },
                        onScanned = { result ->
                            val value = when (result) {
                                is ScanResult.Isbn -> result.isbn13
                                is ScanResult.Copy -> result.code
                                is ScanResult.Person -> null
                                is ScanResult.Unknown -> result.raw.takeUnless { it.startsWith("PERSON:", true) || it.startsWith("P-", true) }
                            }
                            if (value != null) {
                                nav.previousBackStackEntry?.savedStateHandle?.set("scan_$target", value)
                                nav.popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun ReadingRoomBottomBar(current: String, navigate: (String) -> Unit) {
    val destinations = listOf(
        BottomDestination("dashboard", "Início", Icons.Default.Home),
        BottomDestination("loans", "Circulação", Icons.Default.SwapHoriz),
        BottomDestination("scanner/universal", "Câmera", Icons.Default.QrCodeScanner),
        BottomDestination("catalog", "Acervo", Icons.Default.LibraryBooks),
        BottomDestination("more", "Mais", Icons.Default.MoreHoriz)
    )
    NavigationBar {
        destinations.forEach { item ->
            NavigationBarItem(selected = current == item.route, onClick = { navigate(item.route) },
                icon = { Icon(item.icon, contentDescription = null) }, label = { Text(item.label) }, alwaysShowLabel = true)
        }
    }
}

@Composable
private fun ConsumeScan(entry: NavBackStackEntry, key: String, onValue: (String) -> Unit) {
    val value by entry.savedStateHandle.getStateFlow<String?>(key, null).collectAsStateWithLifecycle()
    LaunchedEffect(value) {
        value?.let { onValue(it); entry.savedStateHandle[key] = null }
    }
}

@Composable
private fun MoreMenuScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Mais ferramentas") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onNavigate("reports") }, modifier = Modifier.fillMaxWidth()) { Text("Relatórios locais") }
            OutlinedButton(onClick = { onNavigate("labels") }, modifier = Modifier.fillMaxWidth()) { Text("Etiquetas de exemplares") }
            OutlinedButton(onClick = { onNavigate("backup") }, modifier = Modifier.fillMaxWidth()) { Text("Backup e restauração") }
            OutlinedButton(onClick = { onNavigate("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Configurações e atualização ISBN") }
            OutlinedButton(onClick = { onNavigate("terms-privacy") }, modifier = Modifier.fillMaxWidth()) { Text("Termos de uso e privacidade") }
            OutlinedButton(onClick = { onNavigate("about") }, modifier = Modifier.fillMaxWidth()) { Text("Sobre · Código em Aula") }
        }
    }
}

private fun scannerTitle(target: String) = when (target) {
    "book" -> "Escanear ISBN/EAN"
    "checkout_copy", "return_copy", "inventory" -> "Escanear exemplar"
    else -> "Escanear código de livro"
}
