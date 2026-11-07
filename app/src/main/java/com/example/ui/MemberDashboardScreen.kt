package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DecryptedMember
import com.example.data.model.DecryptedTransaction
import com.example.data.model.MonthlySummary
import com.example.data.model.TransactionType
import com.example.data.model.buildMonthlySummaries
import com.example.data.model.currentMonthYearString
import com.example.data.model.formatMonthYearLabel
import com.example.ui.theme.AppTheme
import com.example.ui.theme.Champagne400
import com.example.ui.theme.Champagne500
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Cyan300
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Sapphire100
import com.example.ui.theme.Sapphire600
import com.example.ui.theme.Sapphire700
import com.example.ui.theme.Sapphire800
import com.example.ui.theme.Sapphire900
import com.example.ui.theme.Sapphire950
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    member: DecryptedMember,
    transactions: List<DecryptedTransaction>,
    showRawCiphertext: Boolean,
    statusMessage: String?,
    onToggleCiphertext: () -> Unit,
    onChangePin: (currentPin: String, newPin: String) -> Unit,
    onBackToPortalGate: () -> Unit
) {
    BackHandler { onBackToPortalGate() }

    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    val memberTransactions = remember(transactions, member.docId) {
        transactions.filter { it.memberDocId == member.docId }
            .sortedByDescending { it.transactionDate }
    }
    val monthlySummaries = remember(memberTransactions) {
        buildMonthlySummaries(memberTransactions)
    }
    val currentMonthKey = remember { currentMonthYearString() }
    val currentMonthSummary = remember(monthlySummaries, currentMonthKey) {
        monthlySummaries.find { it.monthYear == currentMonthKey }
            ?: MonthlySummary(
                monthYear = currentMonthKey,
                displayTitle = formatMonthYearLabel(currentMonthKey),
                totalDeposited = 0L,
                totalWithdrawn = 0L,
                netFlow = 0L,
                transactionCount = 0,
                transactions = emptyList()
            )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Friendship Zone Passbook",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Kalir Bazar • ID: ${member.memberId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan300
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToPortalGate,
                        modifier = Modifier.testTag("member_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Portal Selection",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Surface(
                        color = Cyan400.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onToggleCiphertext() }
                            .testTag("toggle_encryption_inspector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = "Toggle Encryption Inspector",
                                tint = Cyan300,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (showRawCiphertext) "ENCv1" else "AES-256",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Sapphire950
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = AppTheme.elevation.level2
            ) {
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    modifier = Modifier.testTag("member_nav_dashboard"),
                    icon = {
                        Icon(
                            imageVector = if (selectedBottomTab == 0) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Passbook"
                        )
                    },
                    label = { Text("Passbook") }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    modifier = Modifier.testTag("member_nav_transactions"),
                    icon = {
                        Icon(
                            imageVector = if (selectedBottomTab == 1) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Ledger"
                        )
                    },
                    label = { Text("Ledger") }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    modifier = Modifier.testTag("member_nav_profile"),
                    icon = {
                        Icon(
                            imageVector = if (selectedBottomTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Vault Profile"
                        )
                    },
                    label = { Text("Vault & PIN") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedBottomTab) {
                0 -> MemberOverviewTab(
                    member = member,
                    currentMonthSummary = currentMonthSummary,
                    monthlySummaries = monthlySummaries,
                    recentTransactions = memberTransactions.take(5),
                    showRawCiphertext = showRawCiphertext,
                    statusMessage = statusMessage,
                    onViewAllTransactions = { selectedBottomTab = 1 }
                )

                1 -> MemberTransactionsTab(
                    memberTransactions = memberTransactions,
                    showRawCiphertext = showRawCiphertext
                )

                2 -> MemberProfileAndPinTab(
                    member = member,
                    showRawCiphertext = showRawCiphertext,
                    onToggleCiphertext = onToggleCiphertext,
                    onOpenChangePinDialog = { showChangePinDialog = true }
                )
            }
        }
    }

    if (showChangePinDialog) {
        ChangeMemberPinDialog(
            memberName = member.name,
            onDismiss = { showChangePinDialog = false },
            onSubmit = { currentPin, newPin ->
                onChangePin(currentPin, newPin)
                showChangePinDialog = false
            }
        )
    }
}

@Composable
private fun MemberOverviewTab(
    member: DecryptedMember,
    currentMonthSummary: MonthlySummary,
    monthlySummaries: List<MonthlySummary>,
    recentTransactions: List<DecryptedTransaction>,
    showRawCiphertext: Boolean,
    statusMessage: String?,
    onViewAllTransactions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (statusMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // Neo-Banking Holographic Total Deposited Money Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_total_deposit_card"),
                shape = RoundedCornerShape(30.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.elevation.level4)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Sapphire950, Sapphire800, Color(0xFF1E1B4B))
                            )
                        )
                        .drawBehind {
                            drawCircle(
                                color = Cyan400.copy(alpha = 0.14f),
                                radius = size.width * 0.45f,
                                center = Offset(size.width * 0.92f, size.height * 0.18f)
                            )
                            drawCircle(
                                color = Champagne400.copy(alpha = 0.08f),
                                radius = size.width * 0.35f,
                                center = Offset(size.width * 0.1f, size.height * 0.9f)
                            )
                        }
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "ID • ${member.memberId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Cyan300,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            color = Cyan400.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Cyan300,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "REAL-TIME SYNC",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (showRawCiphertext) member.encryptedNameRaw else member.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "TOTAL DEPOSITED MONEY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan300
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "৳ ${formatCurrency(member.totalDeposited)}",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Side-by-side Frosted Glass Stat Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.09f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Net Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${formatCurrency(member.netBalance)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Cyan300
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.09f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Total Withdrawn",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${formatCurrency(member.totalWithdrawn)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFFDA4AF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Current Month Cashflow Pulse Card
        item {
            val totalVolume = (currentMonthSummary.totalDeposited + currentMonthSummary.totalWithdrawn).coerceAtLeast(1L)
            val depositRatio = currentMonthSummary.totalDeposited.toFloat() / totalVolume.toFloat()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("current_month_summary_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.elevation.level1),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Sapphire100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Sapphire700
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Current Month • ${currentMonthSummary.displayTitle}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentMonthSummary.transactionCount} ledger entry(s) this month",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { if (currentMonthSummary.transactionCount == 0) 0.5f else depositRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Sapphire600,
                        trackColor = Color(0xFFFFE4E6)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Sapphire100.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Deposited",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Sapphire800
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+৳${formatCurrency(currentMonthSummary.totalDeposited)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Sapphire700
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFFFE4E6).copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Withdrawn",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Crimson600
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "-৳${formatCurrency(currentMonthSummary.totalWithdrawn)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Crimson600
                                )
                            }
                        }
                    }
                }
            }
        }

        // Monthly Transaction Summary Header (Current & Previous Months)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Statements",
                    style = MaterialTheme.typography.titleLarge
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${monthlySummaries.size} Month(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (monthlySummaries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Monthly Statements Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Deposits and withdrawals recorded by the Kalir Bazar Admin will sync here automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(monthlySummaries, key = { it.monthYear }) { summary ->
                MonthlySummaryExpandableCard(
                    summary = summary,
                    showRawCiphertext = showRawCiphertext
                )
            }
        }

        // Recent Transactions Quick View
        if (recentTransactions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Passbook Activity",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = onViewAllTransactions,
                        modifier = Modifier.testTag("view_all_transactions_button")
                    ) {
                        Text("Full Ledger")
                    }
                }
            }

            items(recentTransactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    showRawCiphertext = showRawCiphertext
                )
            }
        }
    }
}

@Composable
fun MonthlySummaryExpandableCard(
    summary: MonthlySummary,
    showRawCiphertext: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("monthly_summary_card_${summary.monthYear}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.elevation.level1),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Sapphire100,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = summary.monthYear,
                            style = MaterialTheme.typography.labelSmall,
                            color = Sapphire800,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = summary.displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${summary.transactionCount} entries • Net: ৳${formatCurrency(summary.netFlow)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+৳${formatCurrency(summary.totalDeposited)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Sapphire700,
                            fontWeight = FontWeight.Bold
                        )
                        if (summary.totalWithdrawn > 0) {
                            Text(
                                text = "-৳${formatCurrency(summary.totalWithdrawn)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Crimson600
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse month details" else "Expand month details"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    summary.transactions.forEach { tx ->
                        TransactionRowCard(
                            transaction = tx,
                            showRawCiphertext = showRawCiphertext
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberTransactionsTab(
    memberTransactions: List<DecryptedTransaction>,
    showRawCiphertext: Boolean
) {
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(memberTransactions, filterType, searchQuery) {
        memberTransactions.filter { tx ->
            val matchesType = filterType == null || tx.type == filterType
            val matchesSearch = searchQuery.isBlank() ||
                tx.monthYear.contains(searchQuery, ignoreCase = true) ||
                tx.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Sapphire800
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Verified Passbook: Only Kalir Bazar Admins can add or edit transaction entries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Month (YYYY-MM), Receipt #, or Note") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_tx_search_input")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == null,
                    onClick = { filterType = null },
                    label = { Text("All (${memberTransactions.size})") }
                )
                FilterChip(
                    selected = filterType == TransactionType.DEPOSIT,
                    onClick = { filterType = TransactionType.DEPOSIT },
                    label = { Text("Deposits") }
                )
                FilterChip(
                    selected = filterType == TransactionType.WITHDRAWAL,
                    onClick = { filterType = TransactionType.WITHDRAWAL },
                    label = { Text("Withdrawals") }
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No matching transactions found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    showRawCiphertext = showRawCiphertext
                )
            }
        }
    }
}

@Composable
fun TransactionRowCard(
    transaction: DecryptedTransaction,
    showRawCiphertext: Boolean,
    onEditClick: (() -> Unit)? = null
) {
    val isDeposit = transaction.type == TransactionType.DEPOSIT
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    val formattedDate = remember(transaction.transactionDate) {
        dateFormatter.format(Date(transaction.transactionDate))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onEditClick != null) Modifier.clickable { onEditClick() } else Modifier
            )
            .testTag("transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.elevation.level1),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isDeposit) Sapphire100 else Color(0xFFFFE4E6)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = transaction.type.label,
                    tint = if (isDeposit) Sapphire700 else Crimson600
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (showRawCiphertext) transaction.encryptedNoteRaw else transaction.note,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.memberId} • ${transaction.receiptNumber} • $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isDeposit) "+" else "-"}৳${formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDeposit) Sapphire700 else Crimson600,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (isDeposit) Sapphire100.copy(alpha = 0.6f) else Color(0xFFFFE4E6),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = transaction.monthYear,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDeposit) Sapphire800 else Crimson600,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberProfileAndPinTab(
    member: DecryptedMember,
    showRawCiphertext: Boolean,
    onToggleCiphertext: () -> Unit,
    onOpenChangePinDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security & Encryption Inspector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Sapphire950
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Cyan300
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AES-256-GCM Vault Inspector",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${member.authorizedUids.size} device(s) synced with this passbook",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }
                        Switch(
                            checked = showRawCiphertext,
                            onCheckedChange = { onToggleCiphertext() },
                            modifier = Modifier.testTag("profile_ciphertext_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showRawCiphertext) {
                            "Inspecting raw AES-256-GCM ciphertext (ENCv1:...) stored at rest in Firebase Firestore."
                        } else {
                            "All personal information, NID numbers, and nominee records are decrypted locally on-device."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan300
                    )
                }
            }
        }

        // Change Login PIN Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.elevation.level1),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Multi-Device Login PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Change your secret PIN anytime. Immediately updates across all connected devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onOpenChangePinDialog,
                        modifier = Modifier.testTag("change_pin_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Champagne500,
                            contentColor = Sapphire950
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change PIN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Member Personal Information (Admin-managed)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.elevation.level1),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Member Identity & Nominee Record",
                        style = MaterialTheme.typography.titleLarge
                    )
                    ProfileFieldRow("Member ID", member.memberId)
                    ProfileFieldRow(
                        "Full Name",
                        if (showRawCiphertext) member.encryptedNameRaw else member.name
                    )
                    ProfileFieldRow(
                        "Mobile Number",
                        if (showRawCiphertext) member.encryptedMobileRaw else member.mobileNumber
                    )
                    ProfileFieldRow("Address", member.address)
                    ProfileFieldRow(
                        "NID Number",
                        if (showRawCiphertext) member.encryptedNidRaw else member.nidNumber
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    Text(
                        text = "Designated Nominee",
                        style = MaterialTheme.typography.titleLarge
                    )
                    ProfileFieldRow("Nominee Name", member.nomineeName)
                    ProfileFieldRow("Nominee Address", member.nomineeAddress)
                    ProfileFieldRow("Nominee NID Number", member.nomineeNidNumber)
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldRow(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifBlank { "Not specified" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ChangeMemberPinDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onSubmit: (currentPin: String, newPin: String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Login PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Update multi-device login PIN for $memberName.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = it },
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_current_pin_input")
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN (4+ digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_new_pin_input")
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_confirm_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPin.isBlank() -> validationError = "Enter your current PIN."
                        newPin.trim().length < 4 -> validationError = "New PIN must be at least 4 characters."
                        newPin.trim() != confirmPin.trim() -> validationError = "New PIN and confirmation do not match."
                        else -> onSubmit(currentPin.trim(), newPin.trim())
                    }
                },
                modifier = Modifier.testTag("dialog_save_pin_button")
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
