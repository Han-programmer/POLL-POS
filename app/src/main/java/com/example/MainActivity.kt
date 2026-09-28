package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File
import com.example.ui.components.PinLockDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.cash.CashManagementScreen
import com.example.ui.screens.cashier.CashierScreen
import com.example.ui.screens.categories.CategoryScreen
import com.example.ui.screens.customer.CustomerScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.kitchen.KitchenScreen
import com.example.ui.screens.products.ProductScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.stock.StockScreen
import com.example.ui.screens.transactions.TransactionHistoryScreen
import com.example.ui.theme.PollPosTheme
import com.example.ui.theme.PrimaryRed
import com.example.ui.theme.ThemeToggleButton
import com.example.ui.theme.ThemeToggleSwitch
import com.example.ui.viewmodel.NavScreen
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PosViewModel = viewModel()
            val settings by viewModel.storeSettings.collectAsState()

            val isDark = when (settings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            PollPosTheme(
                darkTheme = isDark,
                onThemeChanged = { newIsDark ->
                    viewModel.updateSettings(
                        settings.copy(themeMode = if (newIsDark) "DARK" else "LIGHT")
                    )
                }
            ) {
                PollPosMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollPosMainApp(viewModel: PosViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentStaff by viewModel.currentStaff.collectAsState()
    val isPinLocked by viewModel.isPinLocked.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val lastCompletedTx by viewModel.lastCompletedTransaction.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()
    val lowStockCount by viewModel.lowStockProducts.collectAsState()
    val activeKitchenOrders by viewModel.activeKitchenOrders.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720

    // Show snackbars reactively
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Handle System Back Button
    BackHandler(enabled = currentScreen != NavScreen.DASHBOARD) {
        viewModel.handleBack()
    }

    // PIN Lock Screen
    if (isPinLocked) {
        PinLockDialog(
            staffName = currentStaff?.name ?: "Kasir",
            onPinSubmit = { pin -> viewModel.loginWithPin(pin) }
        )
    }

    // Thermal Receipt Dialog
    if (lastCompletedTx != null) {
        val (tx, items, payments) = lastCompletedTx!!
        ReceiptDialog(
            transaction = tx,
            items = items,
            payments = payments,
            settings = settings,
            onDismiss = { viewModel.dismissReceipt() },
            onPrintBluetooth = {
                viewModel.printCurrentReceipt(settings.bluetoothPrinterAddress)
            }
        )
    }

    if (isTablet) {
        // Tablet Layout: Navigation Rail on Left + Content on Right
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = PrimaryRed
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("POLL POS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryRed)
                    }
                }
            ) {
                Spacer(modifier = Modifier.weight(1f))
                NavScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    val icon = getIconForScreen(screen)
                    val badgeCount = when (screen) {
                        NavScreen.KITCHEN -> activeKitchenOrders.size
                        NavScreen.STOCK -> lowStockCount.size
                        else -> 0
                    }

                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(screen) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge { Text(badgeCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(icon, contentDescription = screen.title)
                            }
                        },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = PrimaryRed,
                            selectedTextColor = PrimaryRed,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { viewModel.lockApp() }) {
                    Icon(Icons.Default.Lock, contentDescription = "Kunci Aplikasi")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main Content Area for Tablet
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(currentScreen.title, fontWeight = FontWeight.Bold)
                        },
                        actions = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "${currentStaff?.name ?: "Kasir"} (${currentStaff?.role ?: "STAFF"})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            // Quick Dynamic Theme Switcher
                            ThemeToggleButton()
                            IconButton(onClick = { viewModel.lockApp() }) {
                                Icon(Icons.Default.Lock, contentDescription = "Kunci Layar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    ScreenContent(screen = currentScreen, viewModel = viewModel)
                }
            }
        }
    } else {
        // Phone Layout: ModalNavigationDrawer + BottomNavigationBar
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(290.dp)) {
                    // Drawer Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = Color.White
                                ) {
                                    if (!settings.logoUri.isNullOrBlank()) {
                                        val logoData: Any = if (settings.logoUri!!.startsWith("/")) File(settings.logoUri!!) else settings.logoUri!!
                                        AsyncImage(
                                            model = logoData,
                                            contentDescription = "Logo Toko",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                settings.storeName.firstOrNull()?.uppercase() ?: "P",
                                                color = PrimaryRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        settings.storeName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text("100% Offline-First", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Staf: ${currentStaff?.name ?: "Kasir"} (${currentStaff?.role ?: "STAFF"})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Menu items
                    NavScreen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        val icon = getIconForScreen(screen)
                        val badgeCount = when (screen) {
                            NavScreen.KITCHEN -> activeKitchenOrders.size
                            NavScreen.STOCK -> lowStockCount.size
                            else -> 0
                        }

                        NavigationDrawerItem(
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (badgeCount > 0) {
                                            Badge { Text(badgeCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(icon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            selected = isSelected,
                            onClick = {
                                viewModel.navigateTo(screen)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedIconColor = PrimaryRed,
                                selectedTextColor = PrimaryRed,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Theme Quick Selector in Drawer
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Tema Tampilan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    Triple("LIGHT", "Terang", Icons.Default.LightMode),
                                    Triple("DARK", "Gelap", Icons.Default.DarkMode),
                                    Triple("SYSTEM", "Auto", Icons.Default.BrightnessAuto)
                                ).forEach { (mode, label, icon) ->
                                    val isSelected = settings.themeMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateSettings(settings.copy(themeMode = mode)) },
                                        leadingIcon = {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        label = { Text("Kunci Kasir (PIN)") },
                        selected = false,
                        onClick = {
                            viewModel.lockApp()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentScreen.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Buka Menu")
                            }
                        },
                        actions = {
                            ThemeToggleButton()
                            IconButton(onClick = { viewModel.lockApp() }) {
                                Icon(Icons.Default.Lock, contentDescription = "Kunci")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        val bottomScreens = listOf(
                            NavScreen.DASHBOARD,
                            NavScreen.CASHIER,
                            NavScreen.TRANSACTIONS,
                            NavScreen.KITCHEN
                        )

                        bottomScreens.forEach { screen ->
                            val isSelected = currentScreen == screen
                            val icon = getIconForScreen(screen)
                            val badgeCount = when (screen) {
                                NavScreen.KITCHEN -> activeKitchenOrders.size
                                else -> 0
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(screen) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge { Text(badgeCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(icon, contentDescription = screen.title)
                                    }
                                },
                                label = { Text(screen.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryRed,
                                    selectedTextColor = PrimaryRed,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    ScreenContent(screen = currentScreen, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ScreenContent(screen: NavScreen, viewModel: PosViewModel) {
    when (screen) {
        NavScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
        NavScreen.CASHIER -> CashierScreen(viewModel = viewModel)
        NavScreen.PRODUCTS -> ProductScreen(viewModel = viewModel)
        NavScreen.CATEGORIES -> CategoryScreen(viewModel = viewModel)
        NavScreen.STOCK -> StockScreen(viewModel = viewModel)
        NavScreen.CUSTOMERS -> CustomerScreen(viewModel = viewModel)
        NavScreen.TRANSACTIONS -> TransactionHistoryScreen(viewModel = viewModel)
        NavScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
        NavScreen.KITCHEN -> KitchenScreen(viewModel = viewModel)
        NavScreen.CASH -> CashManagementScreen(viewModel = viewModel)
        NavScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
    }
}

fun getIconForScreen(screen: NavScreen) = when (screen) {
    NavScreen.DASHBOARD -> Icons.Default.Dashboard
    NavScreen.CASHIER -> Icons.Default.PointOfSale
    NavScreen.PRODUCTS -> Icons.Default.Inventory2
    NavScreen.CATEGORIES -> Icons.Default.Category
    NavScreen.STOCK -> Icons.Default.Warehouse
    NavScreen.CUSTOMERS -> Icons.Default.People
    NavScreen.TRANSACTIONS -> Icons.Default.ReceiptLong
    NavScreen.REPORTS -> Icons.Default.Analytics
    NavScreen.KITCHEN -> Icons.Default.SoupKitchen
    NavScreen.CASH -> Icons.Default.AccountBalanceWallet
    NavScreen.SETTINGS -> Icons.Default.Settings
}
