package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.scale

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.view.HapticFeedbackConstants
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.BudgetViewModel
import com.example.ui.BudgetViewModelFactory
import com.example.ui.theme.MyApplicationTheme

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "budget-database"
        )
            .fallbackToDestructiveMigration(true)
            .build()
        val repository = AppRepository(db)
        val factory = BudgetViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[BudgetViewModel::class.java]

        val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val initialThemeIndex = sharedPref.getInt("theme_index", 0)
        val initialTheme = com.example.ui.theme.AppThemeInfo.entries.getOrElse(initialThemeIndex) { com.example.ui.theme.AppThemeInfo.MULBERRY_MINT }
        val initialLanguage = sharedPref.getString("language", "en") ?: "en"
        val initialThemeModeName = sharedPref.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        val initialThemeMode = try { ThemeMode.valueOf(initialThemeModeName) } catch (e: Exception) { ThemeMode.SYSTEM }

        setContent {
            var currentTheme by remember { mutableStateOf(initialTheme) }
            var currentLanguage by remember { mutableStateOf(initialLanguage) }
            var currentThemeMode by remember { mutableStateOf(initialThemeMode) }
            
            val isDarkTheme = when(currentThemeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            val appStrings = when(currentLanguage) {
                "ru" -> RuStrings
                "uk" -> UkStrings
                else -> EnStrings
            }

            CompositionLocalProvider(LocalAppStrings provides appStrings) {
                MyApplicationTheme(appThemeInfo = currentTheme, darkTheme = isDarkTheme) {
                    BudgetScreen(
                        viewModel = viewModel,
                        currentThemeMode = currentThemeMode,
                        onThemeChange = { newTheme ->
                            currentTheme = newTheme
                            sharedPref.edit().putInt("theme_index", newTheme.ordinal).apply()
                        },
                        onThemeModeChange = { newMode ->
                            currentThemeMode = newMode
                            sharedPref.edit().putString("theme_mode", newMode.name).apply()
                        },
                        onLanguageChange = { newLang ->
                            currentLanguage = newLang
                            sharedPref.edit().putString("language", newLang).apply()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (com.example.ui.theme.AppThemeInfo) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val view = LocalView.current
    val expenses by viewModel.expenses.collectAsState()
    val budget by viewModel.budgetAmount.collectAsState()
    val totalSpent = expenses.sumOf { it.amount }
    val remaining = budget - totalSpent

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("Summary") }
    
    var quickAddName by remember { mutableStateOf("") }
    var quickAddCategory by remember { mutableStateOf("") }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    val warningLimitStr = LocalAppStrings.current.warningLimit
    val warningLimit100Str = LocalAppStrings.current.warningLimit100
    
    androidx.compose.runtime.LaunchedEffect(totalSpent, budget) {
        if (budget > 0) {
            if (totalSpent >= budget) {
                snackbarHostState.showSnackbar(
                    message = warningLimit100Str,
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
            } else if (totalSpent >= budget * 0.8) {
                snackbarHostState.showSnackbar(
                    message = warningLimitStr,
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    showAddExpenseDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp).size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(32.dp))
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                tonalElevation = 0.dp,
                modifier = Modifier.height(80.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { 
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            currentTab = "Summary" 
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 6.dp)
                                .background(
                                    if (currentTab == "Summary") MaterialTheme.colorScheme.onBackground else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(50)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(LocalAppStrings.current.summary, style = MaterialTheme.typography.labelSmall, color = if (currentTab == "Summary") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { 
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            currentTab = "Report" 
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 6.dp)
                                .background(
                                    if (currentTab == "Report") MaterialTheme.colorScheme.onBackground else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(50)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(LocalAppStrings.current.report, style = MaterialTheme.typography.labelSmall, color = if (currentTab == "Report") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MY LEDGER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Finance.",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            showSettingsDialog = true 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = LocalAppStrings.current.settings,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            var searchQuery by remember { mutableStateOf("") }
            val filteredExpenses = if (searchQuery.isBlank()) expenses else expenses.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                placeholder = { Text(LocalAppStrings.current.searchExpenses) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                )
            )

            if (budget > 0 && totalSpent >= budget * 0.8) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (totalSpent >= budget) LocalAppStrings.current.warningLimit100 else LocalAppStrings.current.warningLimit,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (currentTab == "Summary") {
                // Main Balance Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditBudgetDialog = true }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = LocalAppStrings.current.totalBalance,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = LocalAppStrings.current.editBudget,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("₴%.2f", remaining),
                        style = MaterialTheme.typography.displayLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Chart
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 5 })
                LaunchedEffect(pagerState.currentPage) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val pageAlpha = 1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                        val pageScale = 0.85f + (pageAlpha * 0.15f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = pageAlpha
                                    scaleX = pageScale
                                    scaleY = pageScale
                                }, 
                            contentAlignment = Alignment.Center
                        ) {
                            when (page) {
                                0 -> BudgetChart(budget = budget, spent = totalSpent)
                                1 -> ExpensePieChart(expenses = filteredExpenses)
                                2 -> ExpenseBarChart(expenses = filteredExpenses)
                                3 -> PreviousMonthChart(expenses = filteredExpenses)
                                4 -> SixMonthTrendChart(expenses = filteredExpenses)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Quick Add Row
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Text(
                        text = LocalAppStrings.current.quickAdd,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val quickItems = listOf(
                        LocalAppStrings.current.qGroceries to LocalAppStrings.current.catFood,
                        LocalAppStrings.current.qTransport to LocalAppStrings.current.catTransport,
                        LocalAppStrings.current.qRent to LocalAppStrings.current.catHousing,
                        LocalAppStrings.current.qCoffee to LocalAppStrings.current.catFood,
                        LocalAppStrings.current.qMovies to LocalAppStrings.current.catEntertainment
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(quickItems) { item ->
                            Button(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    quickAddName = item.first
                                    quickAddCategory = item.second
                                    showQuickAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(item.first, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Multi-Entry Split View (Expenses & Budget Status)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left: Expenses List
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = LocalAppStrings.current.costs,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredExpenses, key = { it.id }) { expense ->
                                var isVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { isVisible = true }
                                
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isVisible,
                                    enter = androidx.compose.animation.slideInHorizontally(
                                        initialOffsetX = { it }, 
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) + androidx.compose.animation.fadeIn(animationSpec = tween(300)),
                                    exit = androidx.compose.animation.slideOutHorizontally(
                                        targetOffsetX = { it },
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) + androidx.compose.animation.fadeOut(animationSpec = tween(300)),
                                    modifier = Modifier.animateItem()
                                ) {
                                    ExpenseItem(
                                        expense = expense,
                                        onDelete = { 
                                            isVisible = false
                                            viewModel.deleteExpense(it) 
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Right: Remaining Status
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = LocalAppStrings.current.remaining,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format("₴%.0f", remaining),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = LocalAppStrings.current.of + String.format("₴%.0f", budget),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                MonthlyReportView(expenses = filteredExpenses)
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { name, amount, category, currency ->
                viewModel.addExpense(name, amount, category, currency)
                showAddExpenseDialog = false
            }
        )
    }

    if (showQuickAddDialog) {
        AddExpenseDialog(
            initialName = quickAddName,
            initialCategory = quickAddCategory,
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { name, amount, category, currency ->
                viewModel.addExpense(name, amount, category, currency)
                showQuickAddDialog = false
            }
        )
    }

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudget = budget,
            onDismiss = { showEditBudgetDialog = false },
            onConfirm = { amount ->
                viewModel.updateBudget(amount)
                showEditBudgetDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentThemeMode = currentThemeMode,
            onDismiss = { showSettingsDialog = false },
            onThemeSelect = {
                onThemeChange(it)
                showSettingsDialog = false
            },
            onThemeModeSelect = {
                onThemeModeChange(it)
                showSettingsDialog = false
            },
            onLanguageSelect = {
                onLanguageChange(it)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun ExpenseItem(expense: com.example.data.Expense, onDelete: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val amountText = if (expense.currency == "UAH") {
                String.format("-₴%.2f", expense.amount)
            } else {
                String.format("-₴%.2f (-%.2f %s)", expense.amount, expense.originalAmount, expense.currency)
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onDelete(expense.id) },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun BudgetChart(budget: Double, spent: Double) {
    val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val size = size.minDimension - strokeWidth
            
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size, size),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            )

            drawArc(
                color = primaryColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size, size),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "SPENT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                String.format("₴%.0f", spent),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit,
    initialName: String = "",
    initialCategory: String? = null
) {
    val view = LocalView.current
    var name by remember { mutableStateOf(initialName) }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("UAH") }
    var expandedCurrency by remember { mutableStateOf(false) }
    val categories = listOf(LocalAppStrings.current.catFood, LocalAppStrings.current.catTransport, LocalAppStrings.current.catEntertainment, LocalAppStrings.current.catUtilities, LocalAppStrings.current.catHousing, LocalAppStrings.current.catOther)
    var category by remember { mutableStateOf(initialCategory ?: categories[0]) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LocalAppStrings.current.addExpense) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(LocalAppStrings.current.name) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text(LocalAppStrings.current.amount) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedCurrency,
                        onExpandedChange = { expandedCurrency = !expandedCurrency },
                        modifier = Modifier.weight(0.6f)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrency) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            CurrencyService.supportedCurrencies.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        currency = selectionOption
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        readOnly = false,
                        label = { Text(LocalAppStrings.current.category) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    category = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                if (name.isNotBlank() && amt != null) {
                    onConfirm(name, amt, category, currency)
                }
            }) {
                Text(LocalAppStrings.current.add)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(LocalAppStrings.current.cancel) }
        }
    )
}

@Composable
fun MonthlyReportView(expenses: List<com.example.data.Expense>) {
    val groupedExpenses = expenses.groupBy { it.category }
    val categoryTotals = groupedExpenses.mapValues { it.value.sumOf { e -> e.amount } }.toList().sortedByDescending { it.second }
    val maxTotal = categoryTotals.maxOfOrNull { it.second } ?: 1.0

    val context = androidx.compose.ui.platform.LocalContext.current
    val exportPdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) {
                generatePdf(context, uri, expenses)
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONTHLY REPORT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                IconButton(onClick = { exportPdfLauncher.launch("monthly_report.pdf") }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Export PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                val shareReportString = LocalAppStrings.current.shareReport
                IconButton(onClick = {
                    val uri = generateShareableImage(context, expenses)
                    if (uri != null) {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            type = "image/png"
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, shareReportString)
                        context.startActivity(shareIntent)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = shareReportString,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categoryTotals) { (category, total) ->
                val progress = (total / maxTotal).toFloat()
                var expanded by remember { mutableStateOf(false) }
                val view = LocalView.current
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    expanded = !expanded
                                },
                                onLongPress = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    expanded = !expanded
                                }
                            )
                        }
                        .padding(16.dp)
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = String.format("₴%.2f", total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        )
                    }
                    
                    if (expanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val categoryExpenses = groupedExpenses[category] ?: emptyList()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoryExpenses.forEach { expense ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = expense.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(expense.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = String.format("₴%.2f", expense.amount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun generatePdf(context: android.content.Context, uri: android.net.Uri, expenses: List<com.example.data.Expense>) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        paint.textSize = 24f
        paint.color = android.graphics.Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("Monthly Expense Report", 50f, 50f, paint)

        paint.textSize = 16f
        paint.isFakeBoldText = false
        var yPosition = 100f
        
        val groupedExpenses = expenses.groupBy { it.category }
        val categoryTotals = groupedExpenses.mapValues { it.value.sumOf { e -> e.amount } }.toList().sortedByDescending { it.second }
        val totalSpent = categoryTotals.sumOf { it.second }

        categoryTotals.forEach { (category, total) ->
            canvas.drawText(category, 50f, yPosition, paint)
            canvas.drawText(String.format("₴%.2f", total), 400f, yPosition, paint)
            yPosition += 30f
        }

        paint.isFakeBoldText = true
        yPosition += 20f
        canvas.drawText("Total:", 50f, yPosition, paint)
        canvas.drawText(String.format("₴%.2f", totalSpent), 400f, yPosition, paint)

        pdfDocument.finishPage(page)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun EditBudgetDialog(currentBudget: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LocalAppStrings.current.editBudget) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(LocalAppStrings.current.amount) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt != null) {
                    onConfirm(amt)
                }
            }) {
                Text(LocalAppStrings.current.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(LocalAppStrings.current.cancel) }
        }
    )
}

@Composable
fun SettingsDialog(
    currentThemeMode: ThemeMode,
    onDismiss: () -> Unit,
    onThemeSelect: (com.example.ui.theme.AppThemeInfo) -> Unit,
    onThemeModeSelect: (ThemeMode) -> Unit,
    onLanguageSelect: (String) -> Unit
) {
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LocalAppStrings.current.settings) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(LocalAppStrings.current.selectLanguage, style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { 
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    onLanguageSelect("en") 
}) { Text("EN") }
                    Button(onClick = { 
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    onLanguageSelect("ru") 
}) { Text("RU") }
                    Button(onClick = { 
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    onLanguageSelect("uk") 
}) { Text("UK") }
                }
                
                Divider()

                Text(LocalAppStrings.current.selectTheme, style = MaterialTheme.typography.titleSmall)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val modes = listOf(
                        ThemeMode.SYSTEM to LocalAppStrings.current.themeSystem,
                        ThemeMode.LIGHT to LocalAppStrings.current.themeLight,
                        ThemeMode.DARK to LocalAppStrings.current.themeDark
                    )
                    modes.forEach { (mode, label) ->
                        TextButton(
                            onClick = { 
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onThemeModeSelect(mode) 
                            },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                containerColor = if (currentThemeMode == mode) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (currentThemeMode == mode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                com.example.ui.theme.AppThemeInfo.entries.forEach { themeInfo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onThemeSelect(themeInfo)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(themeInfo.darkColor, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(themeInfo.lightColor, CircleShape)
                        )
                        Text(themeInfo.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(LocalAppStrings.current.close) }
        }
    )
}

@Composable
fun ExpensePieChart(expenses: List<com.example.data.Expense>) {
    if (expenses.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Text(LocalAppStrings.current.noExpenses, style = MaterialTheme.typography.labelMedium)
        }
        return
    }

    val groupedExpenses = expenses.groupBy { it.category }
    val categoryTotals = groupedExpenses.mapValues { it.value.sumOf { e -> e.amount } }.toList()
    val total = categoryTotals.sumOf { it.second }
    val colors = listOf(
        androidx.compose.ui.graphics.Color(0xFFE53935), 
        androidx.compose.ui.graphics.Color(0xFFD81B60), 
        androidx.compose.ui.graphics.Color(0xFF8E24AA), 
        androidx.compose.ui.graphics.Color(0xFF5E35B1),
        androidx.compose.ui.graphics.Color(0xFF3949AB)
    )

    var pressedCategory by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current

    val sliceAnimations = categoryTotals.map { pair ->
        val isPressed = pressedCategory == pair.first
        val scale by animateFloatAsState(targetValue = if (isPressed) 1.05f else (if (pressedCategory == null) 1f else 0.95f), animationSpec = tween(300))
        val alpha by animateFloatAsState(targetValue = if (pressedCategory == null || isPressed) 1f else 0.4f, animationSpec = tween(300))
        Triple(pair.first, scale, alpha)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val touchAngle = (kotlin.math.atan2(dy.toDouble(), dx.toDouble()) * 180 / kotlin.math.PI).toFloat()
                        val normalizedTouch = (touchAngle + 90f + 360f) % 360f
                        
                        var currentStartAngle = 0f
                        var foundCategory: String? = null
                        for (pair in categoryTotals) {
                            val sweepAngle = (pair.second / total).toFloat() * 360f
                            if (normalizedTouch >= currentStartAngle && normalizedTouch < currentStartAngle + sweepAngle) {
                                foundCategory = pair.first
                                break
                            }
                            currentStartAngle += sweepAngle
                        }
                        
                        if (foundCategory != null) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            pressedCategory = foundCategory
                            tryAwaitRelease()
                            pressedCategory = null
                        }
                    }
                )
            }
        ) {
            var startAngle = -90f
            categoryTotals.forEachIndexed { index, pair ->
                val sweepAngle = (pair.second / total).toFloat() * 360f
                val anim = sliceAnimations[index]
                
                scale(scale = anim.second, pivot = Offset(size.width / 2, size.height / 2)) {
                    drawArc(
                        color = colors[index % colors.size].copy(alpha = anim.third),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = size
                    )
                }
                startAngle += sweepAngle
            }
        }
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val displayCategory = categoryTotals.find { it.first == pressedCategory }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (displayCategory != null) {
                    Text(
                        displayCategory.first.take(6).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        String.format("₴%.0f", displayCategory.second),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        LocalAppStrings.current.total,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        String.format("₴%.0f", total),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseBarChart(expenses: List<com.example.data.Expense>) {
    if (expenses.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Text(LocalAppStrings.current.noExpenses, style = MaterialTheme.typography.labelMedium)
        }
        return
    }

    val groupedExpenses = expenses.groupBy { it.category }
    val categoryTotals = groupedExpenses.mapValues { it.value.sumOf { e -> e.amount } }.toList().sortedByDescending { it.second }
    val maxTotal = categoryTotals.maxOfOrNull { it.second } ?: 1.0

    var pressedCategory by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        categoryTotals.take(5).forEachIndexed { index, (category, total) ->
            val heightFraction = (total / maxTotal).toFloat()
            val isPressed = pressedCategory == category
            val barColor = colors[index % colors.size]
            
            val scale by animateFloatAsState(if (isPressed) 1.15f else 1f, animationSpec = tween(300))
            val alpha by animateFloatAsState(if (pressedCategory == null || isPressed) 1f else 0.4f, animationSpec = tween(300))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .pointerInput(category) {
                        detectTapGestures(
                            onPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                pressedCategory = category
                                tryAwaitRelease()
                                pressedCategory = null
                            }
                        )
                    }
            ) {
                if (isPressed) {
                    Text(
                        text = String.format("₴%.0f", total),
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxHeight(heightFraction)
                        .width(28.dp)
                        .background(barColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = category.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun PreviousMonthChart(expenses: List<com.example.data.Expense>) {
    val currentCal = java.util.Calendar.getInstance()
    val currentMonth = currentCal.get(java.util.Calendar.MONTH)
    val currentYear = currentCal.get(java.util.Calendar.YEAR)
    
    val prevCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
    val prevMonth = prevCal.get(java.util.Calendar.MONTH)
    val prevYear = prevCal.get(java.util.Calendar.YEAR)

    var currentTotal = 0.0
    var prevTotal = 0.0

    expenses.forEach {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
        val month = cal.get(java.util.Calendar.MONTH)
        val year = cal.get(java.util.Calendar.YEAR)
        if (month == currentMonth && year == currentYear) currentTotal += it.amount
        if (month == prevMonth && year == prevYear) prevTotal += it.amount
    }
    
    val maxTotal = maxOf(currentTotal, prevTotal, 1.0)
    val currentFraction = (currentTotal / maxTotal).toFloat()
    val prevFraction = (prevTotal / maxTotal).toFloat()

    val currentAnimFraction by animateFloatAsState(targetValue = currentFraction, animationSpec = tween(1000))
    val prevAnimFraction by animateFloatAsState(targetValue = prevFraction, animationSpec = tween(1000))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Previous Month
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = String.format("₴%.0f", prevTotal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxHeight(prevAnimFraction)
                    .width(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PREV",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Current Month
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = String.format("₴%.0f", currentTotal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxHeight(currentAnimFraction)
                    .width(32.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CURR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SixMonthTrendChart(expenses: List<com.example.data.Expense>) {
    val months = mutableListOf<Pair<String, Double>>()
    val format = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
    val cal = java.util.Calendar.getInstance()
    
    for (i in 5 downTo 0) {
        val c = java.util.Calendar.getInstance()
        c.add(java.util.Calendar.MONTH, -i)
        val monthLabel = format.format(c.time)
        val targetMonth = c.get(java.util.Calendar.MONTH)
        val targetYear = c.get(java.util.Calendar.YEAR)
        
        val total = expenses.filter {
            val ec = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
            ec.get(java.util.Calendar.MONTH) == targetMonth && ec.get(java.util.Calendar.YEAR) == targetYear
        }.sumOf { it.amount }
        months.add(monthLabel to total)
    }

    val maxAmount = months.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    val pathPoints = remember(months) { months.map { it.second } }
    
    // Animate the path drawing by animating a float from 0 to 1
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }
    val transitionFraction by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp)
    ) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val width = size.width
            val height = size.height
            val stepX = width / 5f
            
            val path = androidx.compose.ui.graphics.Path()
            val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
            
            months.forEachIndexed { index, data ->
                val x = index * stepX
                val y = height - ((data.second / maxAmount) * height).toFloat()
                val point = androidx.compose.ui.geometry.Offset(x, y)
                points.add(point)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // To animate the line drawing, we could use PathMeasure, but to keep it simple and avoid 
            // complicated path trimming, we just draw the line up to the current progress point.
            if (transitionFraction > 0f) {
                drawPath(
                    path = path,
                    color = primaryColor.copy(alpha = transitionFraction),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
            
            points.forEach { point ->
                drawCircle(
                    color = primaryColor.copy(alpha = transitionFraction),
                    radius = 5.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = transitionFraction),
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            months.forEach {
                Text(
                    text = it.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceColor
                )
            }
        }
    }
}


fun generateShareableImage(context: android.content.Context, expenses: List<com.example.data.Expense>): android.net.Uri? {
    try {
        val width = 800
        val height = 1200
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Background
        canvas.drawColor(android.graphics.Color.parseColor("#F5F5F7"))
        
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        
        // Title
        paint.color = android.graphics.Color.parseColor("#1D1D1F")
        paint.textSize = 64f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("Financial Report", width / 2f, 100f, paint)
        
        val groupedExpenses = expenses.groupBy { it.category }
        val categoryTotals = groupedExpenses.mapValues { it.value.sumOf { e -> e.amount } }.toList().sortedByDescending { it.second }
        val totalSpent = categoryTotals.sumOf { it.second }
        
        // Total Spent
        paint.textSize = 48f
        paint.color = android.graphics.Color.parseColor("#86868B")
        canvas.drawText("Total Spent", width / 2f, 180f, paint)
        
        paint.textSize = 80f
        paint.color = android.graphics.Color.parseColor("#007AFF")
        canvas.drawText(String.format("₴%.2f", totalSpent), width / 2f, 280f, paint)
        
        // Chart
        if (categoryTotals.isNotEmpty()) {
            val chartSize = 400f
            val chartLeft = (width - chartSize) / 2f
            val chartTop = 350f
            val rectF = android.graphics.RectF(chartLeft, chartTop, chartLeft + chartSize, chartTop + chartSize)
            
            val colors = listOf(
                android.graphics.Color.parseColor("#FF3B30"),
                android.graphics.Color.parseColor("#FF9500"),
                android.graphics.Color.parseColor("#FFCC00"),
                android.graphics.Color.parseColor("#34C759"),
                android.graphics.Color.parseColor("#5AC8FA"),
                android.graphics.Color.parseColor("#007AFF"),
                android.graphics.Color.parseColor("#5856D6"),
                android.graphics.Color.parseColor("#FF2D55")
            )
            
            var currentAngle = -90f
            categoryTotals.forEachIndexed { index, pair ->
                val sweepAngle = (pair.second / totalSpent).toFloat() * 360f
                paint.color = colors[index % colors.size]
                paint.style = android.graphics.Paint.Style.FILL
                canvas.drawArc(rectF, currentAngle, sweepAngle, true, paint)
                
                currentAngle += sweepAngle
            }
            
            // Draw hole for Donut Chart
            paint.color = android.graphics.Color.parseColor("#F5F5F7")
            canvas.drawCircle(width / 2f, chartTop + chartSize / 2f, chartSize / 3f, paint)
            
            // Legend
            var legendY = chartTop + chartSize + 80f
            val legendLeft = 100f
            
            paint.textAlign = android.graphics.Paint.Align.LEFT
            categoryTotals.forEachIndexed { index, pair ->
                paint.color = colors[index % colors.size]
                canvas.drawRoundRect(android.graphics.RectF(legendLeft, legendY - 24f, legendLeft + 32f, legendY + 8f), 8f, 8f, paint)
                
                paint.color = android.graphics.Color.parseColor("#1D1D1F")
                paint.textSize = 32f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText(pair.first, legendLeft + 50f, legendY, paint)
                
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText(String.format("₴%.2f", pair.second), width - legendLeft, legendY, paint)
                paint.textAlign = android.graphics.Paint.Align.LEFT
                
                legendY += 60f
            }
        }
        
        val file = java.io.File(context.cacheDir, "shared_images")
        file.mkdirs()
        val imageFile = java.io.File(file, "report.png")
        val out = java.io.FileOutputStream(imageFile)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
