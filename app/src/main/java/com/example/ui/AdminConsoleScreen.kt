package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong as OutlinedReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DecryptedMember
import com.example.data.model.DecryptedTransaction
import com.example.data.model.TransactionType
import com.example.data.model.buildMonthlySummaries
import com.example.data.model.currentMonthYearString
import com.example.ui.theme.AppTheme
import com.example.ui.theme.Champagne500
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Cyan300
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Sapphire100
import com.example.ui.theme.Sapphire700
import com.example.ui.theme.Sapphire800
import com.example.ui.theme.Sapphire900
import com.example.ui.theme.Sapphire950
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScreen(
    members: List<DecryptedMember>,
    transactions: List<DecryptedTransaction>,
    showRawCiphertext: Boolean,
    statusMessage: String?,
    onToggleCiphertext: () -> Unit,
    onCreateMember: (
        memberId: String,
        name: String,
        address: String,
        mobile: String,
        nid: String,
        nomineeName: String,
        nomineeAddress: String,
        nomineeNid: String,
        initialPin: String
    ) -> Unit,
    onUpdateMember: (
        existing: DecryptedMember,
        memberId: String,
        name: String,
        address: String,
        mobile: String,
        nid: String,
        nomineeName: String,
        nomineeAddress: String,
        nomineeNid: String,
        newPinOptional: String?
    ) -> Unit,
    onAddTransaction: (
        member: DecryptedMember,
        type: TransactionType,
        amount: Long,
        monthYear: String,
        note: String,
        receiptNumber: String
    ) -> Unit,
    onUpdateTransaction: (
        existingTx: DecryptedTransaction,
        member: DecryptedMember,
        newType: TransactionType,
        newAmount: Long,
        newMonthYear: String,
        newNote: String,
        newReceiptNumber: String
    ) -> Unit,
    onDeleteTransaction: (existingTx: DecryptedTransaction, member: DecryptedMember) -> Unit,
    onOpenMemberDashboardPreview: (String) -> Unit,
    onBackToPortalGate: () -> Unit
) {
    BackHandler { onBackToPortalGate() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateMemberDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<DecryptedMember?>(null) }
    var memberForQuickTx by remember { mutableStateOf<DecryptedMember?>(null) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<DecryptedTransaction?>(null) }

    val totalCooperativeDeposits = remember(members) { members.sumOf { it.totalDeposited } }
    val totalCooperativeWithdrawals = remember(members) { members.sumOf { it.totalWithdrawn } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Executive Admin Console",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Friendship Zone (2005) • ${members.size} Members",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan300
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToPortalGate,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                            .testTag("admin_encryption_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = "Toggle Encryption View",
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
        floatingActionButton = {
            when (selectedTab) {
                0 -> ExtendedFloatingActionButton(
                    onClick = { showCreateMemberDialog = true },
                    modifier = Modifier.testTag("create_member_fab"),
                    shape = RoundedCornerShape(50),
                    containerColor = Cyan400,
                    contentColor = Sapphire950,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("New Member", fontWeight = FontWeight.Bold) }
                )

                1 -> if (members.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddTxDialog = true },
                        modifier = Modifier.testTag("add_transaction_fab"),
                        shape = RoundedCornerShape(50),
                        containerColor = Champagne500,
                        contentColor = Sapphire950,
                        icon = { Icon(Icons.Default.AddCard, contentDescription = null) },
                        text = { Text("Add Entry", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = AppTheme.elevation.level2
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("admin_nav_members"),
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Groups else Icons.Outlined.Groups,
                            contentDescription = "Members"
                        )
                    },
                    label = { Text("Directory (${members.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("admin_nav_transactions"),
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.OutlinedReceiptLong,
                            contentDescription = "Transactions"
                        )
                    },
                    label = { Text("Ledger (${transactions.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("admin_nav_monthly_audit"),
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                            contentDescription = "Monthly Audit"
                        )
                    },
                    label = { Text("Monthly Audit") }
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
            when (selectedTab) {
                0 -> AdminMembersTab(
                    members = members,
                    totalDeposits = totalCooperativeDeposits,
                    totalWithdrawals = totalCooperativeWithdrawals,
                    showRawCiphertext = showRawCiphertext,
                    statusMessage = statusMessage,
                    onEditMember = { memberToEdit = it },
                    onQuickAddTx = { memberForQuickTx = it },
                    onOpenMemberDashboard = onOpenMemberDashboardPreview
                )

                1 -> AdminTransactionsTab(
                    transactions = transactions,
                    members = members,
                    showRawCiphertext = showRawCiphertext,
                    statusMessage = statusMessage,
                    onEditTransaction = { transactionToEdit = it }
                )

                2 -> AdminMonthlyLedgerTab(
                    transactions = transactions,
                    totalDeposits = totalCooperativeDeposits,
                    totalWithdrawals = totalCooperativeWithdrawals,
                    memberCount = members.size,
                    showRawCiphertext = showRawCiphertext
                )
            }
        }
    }

    if (showCreateMemberDialog) {
        MemberFormDialog(
            title = "Create New Member Account",
            existingMember = null,
            nextSuggestedId = "FZ-2005-${String.format(Locale.US, "%03d", members.size + 1)}",
            onDismiss = { showCreateMemberDialog = false },
            onSubmit = { mId, name, addr, mob, nid, nomName, nomAddr, nomNid, pin ->
                onCreateMember(mId, name, addr, mob, nid, nomName, nomAddr, nomNid, pin ?: "1234")
                showCreateMemberDialog = false
            }
        )
    }

    if (memberToEdit != null) {
        val target = memberToEdit!!
        MemberFormDialog(
            title = "Edit Member Profile (${target.memberId})",
            existingMember = target,
            nextSuggestedId = target.memberId,
            onDismiss = { memberToEdit = null },
            onSubmit = { mId, name, addr, mob, nid, nomName, nomAddr, nomNid, pinOptional ->
                onUpdateMember(target, mId, name, addr, mob, nid, nomName, nomAddr, nomNid, pinOptional)
                memberToEdit = null
            }
        )
    }

    if (memberForQuickTx != null) {
        val targetMember = memberForQuickTx!!
        TransactionFormDialog(
            title = "Record Transaction for ${targetMember.name}",
            members = members,
            preselectedMember = targetMember,
            existingTransaction = null,
            onDismiss = { memberForQuickTx = null },
            onSubmit = { chosenMember, type, amount, monthYear, note, receipt ->
                onAddTransaction(chosenMember, type, amount, monthYear, note, receipt)
                memberForQuickTx = null
            },
            onDelete = null
        )
    }

    if (showAddTxDialog && members.isNotEmpty()) {
        TransactionFormDialog(
            title = "Record New Financial Transaction",
            members = members,
            preselectedMember = members.first(),
            existingTransaction = null,
            onDismiss = { showAddTxDialog = false },
            onSubmit = { chosenMember, type, amount, monthYear, note, receipt ->
                onAddTransaction(chosenMember, type, amount, monthYear, note, receipt)
                showAddTxDialog = false
            },
            onDelete = null
        )
    }

    if (transactionToEdit != null) {
        val tx = transactionToEdit!!
        val owningMember = members.find { it.docId == tx.memberDocId }
        if (owningMember != null) {
            TransactionFormDialog(
                title = "Edit Transaction (${tx.receiptNumber})",
                members = members,
                preselectedMember = owningMember,
                existingTransaction = tx,
                onDismiss = { transactionToEdit = null },
                onSubmit = { chosenMember, type, amount, monthYear, note, receipt ->
                    onUpdateTransaction(tx, chosenMember, type, amount, monthYear, note, receipt)
                    transactionToEdit = null
                },
                onDelete = {
                    onDeleteTransaction(tx, owningMember)
                    transactionToEdit = null
                }
            )
        }
    }
}

@Composable
private fun AdminMembersTab(
    members: List<DecryptedMember>,
    totalDeposits: Long,
    totalWithdrawals: Long,
    showRawCiphertext: Boolean,
    statusMessage: String?,
    onEditMember: (DecryptedMember) -> Unit,
    onQuickAddTx: (DecryptedMember) -> Unit,
    onOpenMemberDashboard: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) members
        else members.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.memberId.contains(searchQuery, ignoreCase = true) ||
                it.mobileNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Executive Treasury KPI Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.elevation.level3)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Sapphire950, Sapphire900, Sapphire800)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Text(
                        text = "KALIR BAZAR • NET COOPERATIVE TREASURY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cyan300
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${formatCurrency(totalDeposits - totalWithdrawals)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Total Deposits",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "+৳${formatCurrency(totalDeposits)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Cyan300,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Withdrawals",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "-৳${formatCurrency(totalWithdrawals)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFDA4AF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Members by Name, Member ID, or Mobile") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_member_search_input")
            )
        }

        if (filteredMembers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Member Profiles Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'New Member' below to register a member profile & account with AES-256-GCM encryption.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredMembers, key = { it.docId }) { member ->
                AdminMemberCard(
                    member = member,
                    showRawCiphertext = showRawCiphertext,
                    onEdit = { onEditMember(member) },
                    onAddTransaction = { onQuickAddTx(member) },
                    onViewDashboard = { onOpenMemberDashboard(member.docId) }
                )
            }
        }
    }
}

@Composable
private fun AdminMemberCard(
    member: DecryptedMember,
    showRawCiphertext: Boolean,
    onEdit: () -> Unit,
    onAddTransaction: () -> Unit,
    onViewDashboard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_member_card_${member.memberId}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.elevation.level1),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Sapphire900),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Cyan300
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Surface(
                            color = Sapphire100,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = member.memberId,
                                style = MaterialTheme.typography.labelSmall,
                                color = Sapphire800,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (showRawCiphertext) member.encryptedNameRaw else member.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Mobile: ${if (showRawCiphertext) member.encryptedMobileRaw else member.mobileNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "NID: ${if (showRawCiphertext) member.encryptedNidRaw else member.nidNumber} • Nominee: ${member.nomineeName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳${formatCurrency(member.totalDeposited)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Sapphire700,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Deposited",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onViewDashboard,
                    modifier = Modifier.testTag("preview_member_dashboard_${member.memberId}")
                ) {
                    Text("Passbook")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("edit_member_${member.memberId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Member",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = onAddTransaction,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("quick_tx_${member.memberId}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Sapphire800,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Deposit or Withdrawal",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deposit")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTransactionsTab(
    transactions: List<DecryptedTransaction>,
    members: List<DecryptedMember>,
    showRawCiphertext: Boolean,
    statusMessage: String?,
    onEditTransaction: (DecryptedTransaction) -> Unit
) {
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(transactions, filterType, searchQuery) {
        transactions.filter { tx ->
            val typeMatch = filterType == null || tx.type == filterType
            val searchMatch = searchQuery.isBlank() ||
                tx.memberId.contains(searchQuery, ignoreCase = true) ||
                tx.memberName.contains(searchQuery, ignoreCase = true) ||
                tx.monthYear.contains(searchQuery, ignoreCase = true) ||
                tx.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true)
            typeMatch && searchMatch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        item {
            Text(
                text = "Tap any ledger entry below to edit its amount, type, accounting month, or remarks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Filter by Member ID, Name, Month (YYYY-MM), or Receipt") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_tx_search_input")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == null,
                    onClick = { filterType = null },
                    label = { Text("All (${transactions.size})") }
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
                    text = if (members.isEmpty()) {
                        "Create a Member Profile first, then add deposits or withdrawals."
                    } else {
                        "No transactions match your filter."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(filtered, key = { it.id }) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    showRawCiphertext = showRawCiphertext,
                    onEditClick = { onEditTransaction(tx) }
                )
            }
        }
    }
}

@Composable
private fun AdminMonthlyLedgerTab(
    transactions: List<DecryptedTransaction>,
    totalDeposits: Long,
    totalWithdrawals: Long,
    memberCount: Int,
    showRawCiphertext: Boolean
) {
    val monthlySummaries = remember(transactions) {
        buildMonthlySummaries(transactions)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cooperative Monthly Audit & Vault Status",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Registered Members: $memberCount • Total Entries: ${transactions.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "All member PII and transaction notes are encrypted at rest with AES-256-GCM (ENCv1) and synchronized via Firebase Firestore.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sapphire700
                    )
                }
            }
        }

        items(monthlySummaries, key = { it.monthYear }) { summary ->
            MonthlySummaryExpandableCard(
                summary = summary,
                showRawCiphertext = showRawCiphertext
            )
        }
    }
}

@Composable
private fun MemberFormDialog(
    title: String,
    existingMember: DecryptedMember?,
    nextSuggestedId: String,
    onDismiss: () -> Unit,
    onSubmit: (
        memberId: String,
        name: String,
        address: String,
        mobile: String,
        nid: String,
        nomineeName: String,
        nomineeAddress: String,
        nomineeNid: String,
        pinOptional: String?
    ) -> Unit
) {
    var memberId by remember { mutableStateOf(existingMember?.memberId ?: nextSuggestedId) }
    var name by remember { mutableStateOf(existingMember?.name ?: "") }
    var address by remember { mutableStateOf(existingMember?.address ?: "Kalir Bazar") }
    var mobile by remember { mutableStateOf(existingMember?.mobileNumber ?: "") }
    var nid by remember { mutableStateOf(existingMember?.nidNumber ?: "") }
    var nomineeName by remember { mutableStateOf(existingMember?.nomineeName ?: "") }
    var nomineeAddress by remember { mutableStateOf(existingMember?.nomineeAddress ?: "Kalir Bazar") }
    var nomineeNid by remember { mutableStateOf(existingMember?.nomineeNidNumber ?: "") }
    var loginPin by remember { mutableStateOf(if (existingMember == null) "1234" else "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = memberId,
                    onValueChange = { memberId = it },
                    label = { Text("Member ID *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_member_id")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Member Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_member_name")
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_member_mobile")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Member Address *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_member_address")
                )
                OutlinedTextField(
                    value = nid,
                    onValueChange = { nid = it },
                    label = { Text("Member NID Number *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_member_nid")
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                OutlinedTextField(
                    value = nomineeName,
                    onValueChange = { nomineeName = it },
                    label = { Text("Nominee Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_nominee_name")
                )
                OutlinedTextField(
                    value = nomineeAddress,
                    onValueChange = { nomineeAddress = it },
                    label = { Text("Nominee Address *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_nominee_address")
                )
                OutlinedTextField(
                    value = nomineeNid,
                    onValueChange = { nomineeNid = it },
                    label = { Text("Nominee NID Number *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_nominee_nid")
                )
                OutlinedTextField(
                    value = loginPin,
                    onValueChange = { loginPin = it },
                    label = {
                        Text(
                            if (existingMember == null) "Initial Login PIN (4+ digits) *"
                            else "Reset Login PIN (Leave blank to keep current)"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_member_pin")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        memberId.isBlank() || name.isBlank() || mobile.isBlank() ->
                            errorText = "Member ID, Name, and Mobile Number are required."
                        existingMember == null && loginPin.trim().length < 4 ->
                            errorText = "Initial PIN must be at least 4 characters."
                        else -> onSubmit(
                            memberId.trim(),
                            name.trim(),
                            address.trim().ifBlank { "Kalir Bazar" },
                            mobile.trim(),
                            nid.trim().ifBlank { "N/A" },
                            nomineeName.trim().ifBlank { "N/A" },
                            nomineeAddress.trim().ifBlank { "Kalir Bazar" },
                            nomineeNid.trim().ifBlank { "N/A" },
                            loginPin.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                },
                modifier = Modifier.testTag("form_member_save_button")
            ) {
                Text("Save & Encrypt")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFormDialog(
    title: String,
    members: List<DecryptedMember>,
    preselectedMember: DecryptedMember,
    existingTransaction: DecryptedTransaction?,
    onDismiss: () -> Unit,
    onSubmit: (
        member: DecryptedMember,
        type: TransactionType,
        amount: Long,
        monthYear: String,
        note: String,
        receiptNumber: String
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    var selectedMember by remember { mutableStateOf(preselectedMember) }
    var memberDropdownExpanded by remember { mutableStateOf(false) }
    var txType by remember { mutableStateOf(existingTransaction?.type ?: TransactionType.DEPOSIT) }
    var amountText by remember { mutableStateOf(existingTransaction?.amount?.toString() ?: "") }
    var monthYear by remember {
        mutableStateOf(existingTransaction?.monthYear ?: currentMonthYearString())
    }
    var receiptNumber by remember {
        mutableStateOf(existingTransaction?.receiptNumber ?: "")
    }
    var note by remember {
        mutableStateOf(existingTransaction?.note ?: "Monthly savings installment")
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (existingTransaction == null && members.size > 1) {
                    ExposedDropdownMenuBox(
                        expanded = memberDropdownExpanded,
                        onExpandedChange = { memberDropdownExpanded = !memberDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${selectedMember.name} (${selectedMember.memberId})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Member") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = memberDropdownExpanded,
                            onDismissRequest = { memberDropdownExpanded = false }
                        ) {
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("${m.name} (${m.memberId})") },
                                    onClick = {
                                        selectedMember = m
                                        memberDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Member: ${selectedMember.name} (${selectedMember.memberId})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = txType == TransactionType.DEPOSIT,
                        onClick = { txType = TransactionType.DEPOSIT },
                        label = { Text("Deposit (+)") },
                        modifier = Modifier.testTag("tx_type_deposit")
                    )
                    FilterChip(
                        selected = txType == TransactionType.WITHDRAWAL,
                        onClick = { txType = TransactionType.WITHDRAWAL },
                        label = { Text("Withdrawal (-)") },
                        modifier = Modifier.testTag("tx_type_withdrawal")
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Amount in BDT (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_tx_amount")
                )

                OutlinedTextField(
                    value = monthYear,
                    onValueChange = { monthYear = it },
                    label = { Text("Accounting Month (YYYY-MM) *") },
                    placeholder = { Text("2026-11") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_tx_month")
                )

                OutlinedTextField(
                    value = receiptNumber,
                    onValueChange = { receiptNumber = it },
                    label = { Text("Receipt / Voucher No. (Auto if blank)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_tx_receipt")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Remarks / Particulars (Encrypted)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_tx_note")
                )

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_tx_delete_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Crimson600)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Transaction")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amountText.toLongOrNull() ?: 0L
                    val validMonth = monthYear.trim().matches(Regex("^[0-9]{4}-[0-9]{2}$"))
                    when {
                        parsedAmount <= 0L -> errorText = "Enter a valid amount greater than 0."
                        !validMonth -> errorText = "Month must be in YYYY-MM format (e.g. 2026-11)."
                        else -> onSubmit(
                            selectedMember,
                            txType,
                            parsedAmount,
                            monthYear.trim(),
                            note.trim(),
                            receiptNumber.trim()
                        )
                    }
                },
                modifier = Modifier.testTag("form_tx_save_button")
            ) {
                Text("Save Transaction")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
