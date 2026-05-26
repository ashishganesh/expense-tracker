package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.MonthBalance
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FinFlowApp(viewModel: FinanceViewModel) {
    var isDarkTheme by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        val appLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (appLocked) {
                PinLockScreen(viewModel)
            } else {
                MainAppShell(viewModel, isDarkTheme, onToggleTheme = { isDarkTheme = !isDarkTheme })
            }
        }
    }
}

@Composable
fun PinLockScreen(viewModel: FinanceViewModel) {
    val lockedAccount = viewModel.lockedAccountForSwitch.collectAsStateWithLifecycle().value 
        ?: viewModel.activeAccount.collectAsStateWithLifecycle().value
    
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Account Locked",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Enter 4-digit PIN for profile: ${lockedAccount?.name ?: ""}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                for (i in 1..4) {
                    val active = pinInput.length >= i
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Passcode Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val numList = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )

                for (row in numList) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (cell in row) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = {
                                            errorMessage = ""
                                            if (cell == "⌫") {
                                                if (pinInput.isNotEmpty()) {
                                                    pinInput = pinInput.dropLast(1)
                                                }
                                            } else {
                                                if (pinInput.length < 4) {
                                                    pinInput += cell
                                                }
                                                // Trigger unlock when 4 digits are typed
                                                if (pinInput.length == 4) {
                                                    val success = viewModel.unlockAccount(pinInput)
                                                    if (!success) {
                                                        errorMessage = "Incorrect PIN. Please try again."
                                                        pinInput = ""
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("pin_key_$cell"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(
                                            text = cell,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    viewModel: FinanceViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeAcc by viewModel.activeAccount.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(0) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showAccountMenu = true }
                            .padding(8.dp)
                            .testTag("account_switcher_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = "Wallet",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = activeAcc?.name ?: "No Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Switch account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown"
                        )
                    }
                },
                actions = {
                    // Currency selector or Dark Theme toggle buttons
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                val items = listOf(
                    Triple("Dashboard", Icons.Default.Dashboard, 0),
                    Triple("Transactions", Icons.Default.List, 1),
                    Triple("Budgets / Goals", Icons.Default.TrendingUp, 2),
                    Triple("Recurring / Reminders", Icons.Default.Notifications, 3),
                    Triple("More", Icons.Default.Settings, 4)
                )
                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != 4) {
                FloatingActionButton(
                    onClick = { showAddTransactionDialog = true },
                    modifier = Modifier.testTag("fab_add_transaction"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentTab) {
                0 -> DashboardTab(viewModel)
                1 -> TransactionsTab(viewModel)
                2 -> BudgetsAndGoalsTab(viewModel)
                3 -> RemindersTab(viewModel)
                4 -> SettingsTab(viewModel, onAddAccount = { showAddAccountDialog = true })
            }

            // Dropdown Menu for accounts
            if (showAccountMenu) {
                Dialog(onDismissRequest = { showAccountMenu = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Select Account Profile",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 240.dp)
                            ) {
                                items(accounts) { acc ->
                                    val isCurrent = acc.id == activeAcc?.id
                                    val isLocked = acc.pin != null

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCurrent) MaterialTheme.colorScheme.primaryContainer 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                            .clickable {
                                                viewModel.switchAccount(acc)
                                                showAccountMenu = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Person,
                                            contentDescription = "Profile Type",
                                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = acc.name,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAccountMenu = false }) {
                                    Text("Close")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { 
                                    showAccountMenu = false
                                    showAddAccountDialog = true 
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Account")
                                }
                            }
                        }
                    }
                }
            }

            // Add Account Dialog
            if (showAddAccountDialog) {
                AddAccountDialog(
                    onDismiss = { showAddAccountDialog = false },
                    onConfirm = { name, curr, pin, carryCheck, bAmount ->
                        viewModel.addAccount(name, curr, pin, carryCheck, bAmount)
                        showAddAccountDialog = false
                    }
                )
            }

            // Add Transaction Drawer Dialog
            if (showAddTransactionDialog) {
                AddTransactionDialog(
                    viewModel = viewModel,
                    onDismiss = { showAddTransactionDialog = false },
                    onConfirm = { type, amt, title, time, rems, cat ->
                        viewModel.addTransaction(type, amt, title, time, rems, cat)
                        showAddTransactionDialog = false
                    }
                )
            }
        }
    }
}

// Account Icons & Category Icons mappings
fun getCategoryEmoji(catName: String): String {
    return when (catName.lowercase().trim()) {
        "food" -> "🍔"
        "travel" -> "🚗"
        "bills" -> "🔌"
        "shopping" -> "🛍️"
        "education" -> "🎓"
        "health" -> "🏥"
        "rent" -> "🏠"
        "entertainment" -> "🎬"
        "investment" -> "📈"
        "other" -> "🏷️"
        else -> "🏷️"
    }
}

@Composable
fun DashboardTab(viewModel: FinanceViewModel) {
    val activeAcc by viewModel.activeAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val monthBalances by viewModel.monthBalances.collectAsStateWithLifecycle()
    val budgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val reminders by viewModel.billReminders.collectAsStateWithLifecycle()

    // Find the current active month calculations
    val currentMonthKey = remember {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        sdf.format(Date())
    }
    val currentMonthData = monthBalances.find { it.monthKey == currentMonthKey } 
        ?: monthBalances.lastOrNull() 
        ?: MonthBalance(currentMonthKey, "Current", 0.0, 0.0, 0.0, 0.0)

    val currency = activeAcc?.currency ?: "₹"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Core Balance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_balance_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "TOTAL BALANCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = "$currency ${String.format("%,.2f", currentMonthData.closingBalance)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Month Opening",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$currency ${String.format("%,.0f", currentMonthData.openingBalance)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Carry Forward",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = if (activeAcc?.carryForwardEnabled == true) "Enabled" else "Disabled",
                                fontWeight = FontWeight.Bold,
                                color = if (activeAcc?.carryForwardEnabled == true) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Quick Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$currency ${String.format("%,.0f", currentMonthData.totalIncome)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // Expense card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$currency ${String.format("%,.0f", currentMonthData.totalExpense)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }

        // Financial Coach & System Warning alerts
        val monthlyBudget = activeAcc?.monthlyBudget ?: 0.0
        val spentAmt = currentMonthData.totalExpense
        if (monthlyBudget > 0.0) {
            item {
                val budgetLeft = monthlyBudget - spentAmt
                val limitPercent = (spentAmt / monthlyBudget).coerceIn(0.0, 2.0)
                val alertColor = when {
                    limitPercent >= 1.0 -> MaterialTheme.colorScheme.error
                    limitPercent >= 0.8 -> Color(0xFFFBC02D) // yellow700
                    else -> MaterialTheme.colorScheme.primary
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, alertColor.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (limitPercent >= 1.0) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = alertColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Budget Allowance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = alertColor
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { limitPercent.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = alertColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$currency ${String.format("%,.0f", spentAmt)} spent of $currency ${String.format("%,.0f", monthlyBudget)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (budgetLeft >= 0) "$currency ${String.format("%,.0f", budgetLeft)} left" else "Overspent by $currency ${String.format("%,.0f", -budgetLeft)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = alertColor
                            )
                        }
                    }
                }
            }
        }

        // Actionable reminders
        val pendingReminders = reminders.filter { !it.isPaid }
        if (pendingReminders.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Pending Bills Checklist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingReminders.take(3).forEach { reminder ->
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            val dueStr = sdf.format(Date(reminder.dueDate))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(reminder.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Due: $dueStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        "$currency ${String.format("%,.0f", reminder.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    
                                    Button(
                                        onClick = { viewModel.markReminderPaid(reminder, logExpense = true) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Pay", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cognitive Financial Coach Insights Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💭", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Financial Coach",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val spentRatio = if (currentMonthData.totalIncome > 0) spentAmt / currentMonthData.totalIncome else 0.0
                    val coachInsight = when {
                        spentRatio >= 0.85 -> {
                            "Warning! Your expenses have reached ${String.format("%.0f%%", spentRatio * 100)} of your current month income. Save where possible and limit subscription/entertainment checkouts this week."
                        }
                        spentRatio in 0.5..0.85 -> {
                            "You are maintaining a decent flow, spent ${String.format("%.0f%%", spentRatio * 100)} of income. Allocate at least ${currency}1,000 to your active Savings Goal before the month concludes."
                        }
                        spentAmt == 0.0 -> {
                            "No recorded expenses yet for this month! Great starting block. Configure a Category Budget in Budgets tab to pace yourself."
                        }
                        else -> {
                            "Excellent financial restraint! You are spending only ${String.format("%.0f%%", spentRatio * 100)} of your earnings, keeping a massive chunk of cash for savings goals. Keep it up!"
                        }
                    }

                    Text(
                        text = coachInsight,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Category budgets warning lists (small)
        val overspentBudgets = budgets.filter { b ->
            val totalCatSpent = transactions
                .filter { it.type == "EXPENSE" && it.category == b.category && SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.timestamp)) == currentMonthKey }
                .sumOf { it.amount }
            totalCatSpent > b.amount
        }
        if (overspentBudgets.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        overspentBudgets.forEach { budget ->
                            Text(
                                text = "🚨 Limit exceeded for Category '${budget.category}'. Spent more than allocated $currency ${String.format("%.0f", budget.amount)}!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent 5 Transactions header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded transactions. Tap '+' to add details.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Take recent 5
            val recentTxs = transactions.sortedByDescending { it.timestamp }.take(5)
            items(recentTxs, key = { it.id }) { tx ->
                TransactionListItem(tx, currency) {
                    viewModel.deleteTransaction(tx)
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    tx: Transaction,
    currencyString: String,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val dateStr = sdf.format(Date(tx.timestamp))
    val isExpense = tx.type == "EXPENSE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${tx.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isExpense) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(getCategoryEmoji(tx.category), fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateStr • ${tx.category}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tx.remarks.isNotEmpty()) {
                    Text(
                        text = tx.remarks,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isExpense) "-" else "+"} $currencyString ${String.format("%,.2f", tx.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isExpense) Color(0xFFC62828) else Color(0xFF2E7D32),
                    modifier = Modifier.padding(end = 8.dp)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Transactions Screen Tab with search and filters
@Composable
fun TransactionsTab(viewModel: FinanceViewModel) {
    val activeAcc by viewModel.activeAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currency = activeAcc?.currency ?: "₹"

    var searchText by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedTypeFilter by remember { mutableStateOf("All") } // All, INCOME, EXPENSE

    val defaultCategories = listOf("All", "Food", "Travel", "Bills", "Shopping", "Education", "Health", "Rent", "Entertainment", "Investment", "Other")

    val filteredTransactions = remember(transactions, searchText, selectedCategoryFilter, selectedTypeFilter) {
        transactions.filter { tx ->
            val matchText = tx.title.contains(searchText, ignoreCase = true) || tx.remarks.contains(searchText, ignoreCase = true)
            val matchCategory = selectedCategoryFilter == "All" || tx.category == selectedCategoryFilter
            val matchType = selectedTypeFilter == "All" || tx.type == selectedTypeFilter
            matchText && matchCategory && matchType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Recorded Transactions", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        // Search text field
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_transactions_input"),
            placeholder = { Text("Search transactions...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )

        // Type Filter buttons (All, Income, Expense)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf("All", "INCOME", "EXPENSE")
            types.forEach { type ->
                val isSelected = selectedTypeFilter == type
                Button(
                    onClick = { selectedTypeFilter = type },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (type == "All") "All" else type,
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Horizontal Category Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            defaultCategories.forEach { cat ->
                val isSelected = selectedCategoryFilter == cat
                InputChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(cat) },
                    modifier = Modifier.testTag("filter_chip_$cat")
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions found matching filters.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionListItem(tx, currency) {
                        viewModel.deleteTransaction(tx)
                    }
                }
            }
        }
    }
}

// Goals + Budgets Tab Screen
@Composable
fun BudgetsAndGoalsTab(viewModel: FinanceViewModel) {
    val activeAcc by viewModel.activeAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val currency = activeAcc?.currency ?: "₹"

    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Category Budgets, 1 = Savings Goals

    val currentMonthKey = remember {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        sdf.format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(selectedTabIndex = activeSubTab) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                Text("Category Limits", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                Text("Savings Goals", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        if (activeSubTab == 0) {
            CategoryBudgetsSubSection(viewModel, budgets, transactions, currentMonthKey, currency)
        } else {
            SavingsGoalsSubSection(viewModel, goals, currency)
        }
    }
}

@Composable
fun CategoryBudgetsSubSection(
    viewModel: FinanceViewModel,
    budgets: List<CategoryBudget>,
    transactions: List<Transaction>,
    monthKey: String,
    currency: String
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Food") }
    var budgetAmountInput by remember { mutableStateOf("") }

    val categories = listOf("Food", "Travel", "Bills", "Shopping", "Education", "Health", "Rent", "Entertainment", "Investment", "Other")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Category Budgets", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Set Budget", fontSize = 11.sp)
            }
        }

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No budgets configured. Set category allowance above limit.", textAlign = TextAlign.Center, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(budgets) { budget ->
                    val spent = transactions
                        .filter { it.type == "EXPENSE" && it.category == budget.category && SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.timestamp)) == monthKey }
                        .sumOf { it.amount }

                    val progressFraction = (spent / budget.amount).coerceIn(0.0, 2.0).toFloat()
                    val indicatorColor = when {
                        progressFraction >= 1.0f -> MaterialTheme.colorScheme.error
                        progressFraction >= 0.8f -> Color(0xFFFBC02D)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(getCategoryEmoji(budget.category), fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(budget.category, fontWeight = FontWeight.Bold)
                                }

                                IconButton(onClick = { viewModel.deleteCategoryBudget(budget.category) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                color = indicatorColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Spent: $currency ${String.format("%,.0f", spent)} of $currency ${String.format("%,.0f", budget.amount)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (spent > budget.amount) "Exceeded!" else "${String.format("%.0f%%", progressFraction * 100)} Used",
                                    color = indicatorColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configure Category Limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Selected Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = budgetAmountInput,
                        onValueChange = { budgetAmountInput = it },
                        label = { Text("Budget Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val amt = budgetAmountInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                viewModel.setCategoryBudget(selectedCategory, amt)
                                showAddDialog = false
                            }
                        }) { Text("Save Limit") }
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsGoalsSubSection(
    viewModel: FinanceViewModel,
    goals: List<SavingsGoal>,
    currency: String
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var goalNameInput by remember { mutableStateOf("") }
    var targetAmtInput by remember { mutableStateOf("") }
    var currentAmtInput by remember { mutableStateOf("") }

    var selectedGoalForFundChange by remember { mutableStateOf<SavingsGoal?>(null) }
    var fundChangeInput by remember { mutableStateOf("") }
    var fundAddAction by remember { mutableStateOf(true) } // true=Add, false=Withdraw

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Savings Goal Tracker", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Goal", fontSize = 11.sp)
            }
        }

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No savings goal defined. Motivate yourself by saving today!", textAlign = TextAlign.Center, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(goals) { goal ->
                    val fraction = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
                    val daysLeft = ((goal.targetDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Days remaining: $daysLeft days", fontSize = 11.sp, color = Color.Gray)
                                }

                                Row {
                                    IconButton(onClick = {
                                        selectedGoalForFundChange = goal
                                        fundAddAction = true
                                    }) {
                                        Icon(Icons.Default.AddCircle, contentDescription = "Add Funds", tint = Color(0xFF2E7D32))
                                    }
                                    IconButton(onClick = {
                                        selectedGoalForFundChange = goal
                                        fundAddAction = false
                                    }) {
                                        Icon(Icons.Default.RemoveCircle, contentDescription = "Withdraw", tint = Color(0xFFC62828))
                                    }
                                    IconButton(onClick = { viewModel.deleteSavingsGoal(goal) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = { fraction },
                                color = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Saved: $currency ${String.format("%,.0f", goal.currentAmount)} of $currency ${String.format("%,.0f", goal.targetAmount)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.0f%%", fraction * 100)} Completed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Define New Savings Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = goalNameInput,
                        onValueChange = { goalNameInput = it },
                        label = { Text("Savings Goal Name (e.g., Tesla, Laptop)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetAmtInput,
                        onValueChange = { targetAmtInput = it },
                        label = { Text("Target Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currentAmtInput,
                        onValueChange = { currentAmtInput = it },
                        label = { Text("Initial Saved Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val target = targetAmtInput.toDoubleOrNull() ?: 0.0
                            val initial = currentAmtInput.toDoubleOrNull() ?: 0.0
                            if (goalNameInput.isNotBlank() && target > 0.0) {
                                // Default goal projection to 1 year time
                                val futureYearTime = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
                                viewModel.addSavingsGoal(goalNameInput, target, initial, futureYearTime)
                                showAddDialog = false
                            }
                        }) { Text("Create") }
                    }
                }
            }
        }
    }

    if (selectedGoalForFundChange != null) {
        val changeGoal = selectedGoalForFundChange!!
        Dialog(onDismissRequest = { selectedGoalForFundChange = null }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (fundAddAction) "Deposit Saved Funds" else "Withdraw Saved Funds",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = fundChangeInput,
                        onValueChange = { fundChangeInput = it },
                        label = { Text("Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selectedGoalForFundChange = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val changeVal = fundChangeInput.toDoubleOrNull() ?: 0.0
                            if (changeVal > 0.0) {
                                val currentGoal = changeGoal
                                val updatedSum = if (fundAddAction) {
                                    currentGoal.currentAmount + changeVal
                                } else {
                                    (currentGoal.currentAmount - changeVal).coerceAtLeast(0.0)
                                }
                                viewModel.updateSavingsGoal(currentGoal.copy(currentAmount = updatedSum))
                                selectedGoalForFundChange = null
                                fundChangeInput = ""
                            }
                        }) { Text("Confirm") }
                    }
                }
            }
        }
    }
}

// Reminders + Recurring Panel Screen
@Composable
fun RemindersTab(viewModel: FinanceViewModel) {
    val activeAcc by viewModel.activeAccount.collectAsStateWithLifecycle()
    val reminders by viewModel.billReminders.collectAsStateWithLifecycle()
    val recurringList by viewModel.recurringTransactions.collectAsStateWithLifecycle()
    val currency = activeAcc?.currency ?: "₹"

    var showReminderDialog by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    var showRecurringDialog by remember { mutableStateOf(false) }
    var rTitleInput by remember { mutableStateOf("") }
    var rAmtInput by remember { mutableStateOf("") }
    var rCategorySetting by remember { mutableStateOf("Bills") }
    var rFreqSetting by remember { mutableStateOf("MONTHLY") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unpaid Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Upcoming Bill Checklist", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = { showReminderDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Bill", fontSize = 11.sp)
            }
        }

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No reminders configured.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(reminders) { bill ->
                    val isPaid = bill.isPaid
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    val dueStr = sdf.format(Date(bill.dueDate))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bill.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    style = if (isPaid) MaterialTheme.typography.bodyMedium.copy(color = Color.Gray) else MaterialTheme.typography.bodyMedium
                                )
                                Text("Due date: $dueStr", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                "$currency ${String.format("%,.0f", bill.amount)}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            if (!isPaid) {
                                Button(
                                    onClick = { viewModel.markReminderPaid(bill, logExpense = true) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Pay", fontSize = 11.sp)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Paid", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteReminder(bill) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Divider()

        // Recurring section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recurring Transactions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = { showRecurringDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule", fontSize = 11.sp)
            }
        }

        if (recurringList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No active repeating schedules found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(recurringList) { rec ->
                    val nextStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(rec.nextDueDate))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Repeats: ${rec.frequency} • Next: $nextStr", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text(
                                "${if (rec.type == "INCOME") "+" else "-"} $currency ${String.format("%,.0f", rec.amount)}",
                                fontWeight = FontWeight.Bold,
                                color = if (rec.type == "INCOME") Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { viewModel.deleteRecurringTransaction(rec) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReminderDialog) {
        Dialog(onDismissRequest = { showReminderDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add Upcoming Bill Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Bill Identifier (e.g., Electric Bill)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount Due ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showReminderDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (titleInput.isNotBlank() && amt > 0.0) {
                                // Default due to 1 week from now
                                val dueTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
                                viewModel.addBillReminder(titleInput, amt, dueTime)
                                showReminderDialog = false
                                titleInput = ""
                                amountInput = ""
                            }
                        }) { Text("Confirm") }
                    }
                }
            }
        }
    }

    if (showRecurringDialog) {
        Dialog(onDismissRequest = { showRecurringDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Schedule Recurring Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = rTitleInput,
                        onValueChange = { rTitleInput = it },
                        label = { Text("Transaction Title (e.g., House Rent)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rAmtInput,
                        onValueChange = { rAmtInput = it },
                        label = { Text("Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Frequency:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val freqs = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
                        freqs.forEach { fr ->
                            FilterChip(
                                selected = rFreqSetting == fr,
                                onClick = { rFreqSetting = fr },
                                label = { Text(fr, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showRecurringDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val amt = rAmtInput.toDoubleOrNull() ?: 0.0
                            if (rTitleInput.isNotBlank() && amt > 0.0) {
                                viewModel.addRecurringTransaction(
                                    type = "EXPENSE",
                                    amount = amt,
                                    title = rTitleInput,
                                    category = rCategorySetting,
                                    remarks = "Scheduled repeating payment",
                                    frequency = rFreqSetting,
                                    startDate = System.currentTimeMillis()
                                )
                                showRecurringDialog = false
                                rTitleInput = ""
                                rAmtInput = ""
                            }
                        }) { Text("Schedule") }
                    }
                }
            }
        }
    }
}

// Extra Settings + Reports View
@Composable
fun SettingsTab(viewModel: FinanceViewModel, onAddAccount: () -> Unit) {
    val activeAcc by viewModel.activeAccount.collectAsStateWithLifecycle()
    val monthBalances by viewModel.monthBalances.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currency = activeAcc?.currency ?: "₹"

    var configNameInput by remember(activeAcc) { mutableStateOf(activeAcc?.name ?: "") }
    var configCurrencyInput by remember(activeAcc) { mutableStateOf(activeAcc?.currency ?: "₹") }
    var configPinInput by remember(activeAcc) { mutableStateOf(activeAcc?.pin ?: "") }
    var configCarryForward by remember(activeAcc) { mutableStateOf(activeAcc?.carryForwardEnabled ?: true) }
    var configBudgetInput by remember(activeAcc) { mutableStateOf(activeAcc?.monthlyBudget?.toString() ?: "0") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Carry Forward chronological visual logs
        item {
            Text("Carry-Forward Flow Logs", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Monthly carryovers transfer ending cash values forward. Turn on to rollover balances sequentially.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    monthBalances.sortedBy { it.monthKey }.forEach { mb ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mb.monthName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Opening: $currency ${String.format("%.0f", mb.openingBalance)}", fontSize = 11.sp, color = Color.Gray)
                                Text("Ending: $currency ${String.format("%.0f", mb.closingBalance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            Text("Configure Profile Configuration", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = configNameInput,
                    onValueChange = { configNameInput = it },
                    label = { Text("Profile Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = configCurrencyInput,
                    onValueChange = { configCurrencyInput = it },
                    label = { Text("Preferred Currency Symbol") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = configPinInput,
                    onValueChange = { configPinInput = it },
                    label = { Text("Account Lock PIN (4 digits or blank)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rollover Balance Carry-Forward", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Transfers balance ending this month to opening of next automatically.", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = configCarryForward,
                        onCheckedChange = { configCarryForward = it },
                        modifier = Modifier.testTag("carry_forward_switch")
                    )
                }

                OutlinedTextField(
                    value = configBudgetInput,
                    onValueChange = { configBudgetInput = it },
                    label = { Text("Total Monthly Budget Limit ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = {
                        val limitVal = configBudgetInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateActiveAccountConfig(
                            name = configNameInput,
                            currency = configCurrencyInput,
                            pin = configPinInput,
                            carryForwardEnabled = configCarryForward,
                            monthlyBudget = limitVal
                        )
                        Toast.makeText(context, "Configurations updated successfully!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Apply Configurations")
                    }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        // Tools Backup + Exports
        item {
            Text("Portability, Backups & Tools", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.exportTransactionsToCsv(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Profile Sheet to CSV")
                }

                Button(
                    onClick = { viewModel.exportStateToJson(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Whole Database (JSON State)")
                }

                Text(
                    text = "To Restore previously downloaded database backups (.json), paste the raw JSON text content below and tap restore.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp
                )

                var importJsonInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    label = { Text("Paste Backup JSON Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Button(
                    onClick = {
                        if (importJsonInput.isNotBlank()) {
                            val success = viewModel.importStateFromJson(context, importJsonInput)
                            if (success) {
                                importJsonInput = ""
                            }
                        } else {
                            Toast.makeText(context, "Please paste some content first.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Database from Block")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.deleteActiveAccount() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Destruct / Clear This Account Profile")
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, currency: String, pin: String?, carryJoin: Boolean, budget: Double) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var currencyInput by remember { mutableStateOf("₹") }
    var pinInput by remember { mutableStateOf("") }
    var carryForwardChecked by remember { mutableStateOf(true) }
    var globalBudgetInput by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Establish New Account Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Profile Name (Personal, Business, etc)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = currencyInput,
                    onValueChange = { currencyInput = it },
                    label = { Text("Currency Symbol (e.g. ₹, $, €)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    label = { Text("Security Lock PIN (4 digits or blank)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Carry-Forward Rollovers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(checked = carryForwardChecked, onCheckedChange = { carryForwardChecked = it })
                }

                OutlinedTextField(
                    value = globalBudgetInput,
                    onValueChange = { globalBudgetInput = it },
                    label = { Text("Set Global Budget Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onConfirm(
                                    nameInput,
                                    currencyInput,
                                    if (pinInput.length == 4) pinInput else null,
                                    carryForwardChecked,
                                    globalBudgetInput.toDoubleOrNull() ?: 0.0
                                )
                            }
                        }
                    ) { Text("Create Profile") }
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onConfirm: (type: String, amount: Double, title: String, time: Long, rems: String, cat: String) -> Unit
) {
    var typeSelection by remember { mutableStateOf("EXPENSE") } // INCOME or EXPENSE
    var amountInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Food") }
    var remarksInput by remember { mutableStateOf("") }

    val categories = listOf("Food", "Travel", "Bills", "Shopping", "Education", "Health", "Rent", "Entertainment", "Investment", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Transaction",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Segmented control type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { typeSelection = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (typeSelection == "EXPENSE") Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Expense", color = if (typeSelection == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = { typeSelection = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (typeSelection == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Income", color = if (typeSelection == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Transaction Title (eg groceries)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = categorySelection == cat,
                            onClick = { categorySelection = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = remarksInput,
                    onValueChange = { remarksInput = it },
                    label = { Text("Remarks (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (titleInput.isNotBlank() && amt > 0.0) {
                                onConfirm(
                                    typeSelection,
                                    amt,
                                    titleInput,
                                    System.currentTimeMillis(),
                                    remarksInput,
                                    categorySelection
                                )
                            }
                        }
                    ) { Text("Log Transaction") }
                }
            }
        }
    }
}
