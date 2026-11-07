package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.CooperativeRepository
import com.example.ui.AdminConsoleScreen
import com.example.ui.AuthScreen
import com.example.ui.CooperativeViewModel
import com.example.ui.MemberDashboardScreen
import com.example.ui.PortalMode
import com.example.ui.PortalUnlockScreen
import com.example.ui.UiState
import com.example.ui.signOutUser
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FriendshipZoneApp()
            }
        }
    }
}

@Composable
fun FriendshipZoneApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedCooperativeRoot(
                currentUserId = uid,
                userEmail = user.email,
                userDisplayName = user.displayName,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedCooperativeRoot(
    currentUserId: String,
    userEmail: String?,
    userDisplayName: String?,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: CooperativeViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CooperativeViewModel(CooperativeRepository(db), currentUserId)
            }
        }
    )

    val portalMode by viewModel.portalMode.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val boundMembersState by viewModel.boundMembersState.collectAsStateWithLifecycle()
    val boundTransactionsState by viewModel.boundTransactionsState.collectAsStateWithLifecycle()
    val allMembersState by viewModel.allMembersState.collectAsStateWithLifecycle()
    val allTransactionsState by viewModel.allTransactionsState.collectAsStateWithLifecycle()
    val selectedMemberDocId by viewModel.selectedMemberDocId.collectAsStateWithLifecycle()
    val showRawCiphertext by viewModel.showRawCiphertext.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    when (portalMode) {
        PortalMode.PORTAL_GATE -> {
            PortalUnlockScreen(
                userEmail = userEmail,
                userDisplayName = userDisplayName,
                isAdminVerified = isAdmin,
                boundMembersState = boundMembersState,
                isBusy = isBusy,
                statusMessage = statusMessage,
                onSelectBoundMember = { docId ->
                    viewModel.clearStatusMessage()
                    viewModel.selectMemberAccount(docId)
                },
                onUnlockMemberWithPin = { identifier, pin ->
                    viewModel.unlockMemberWithPin(identifier, pin)
                },
                onUnlockAdminConsole = { displayName, adminPin ->
                    viewModel.unlockAdminConsole(displayName, adminPin)
                },
                onEnterAdminDirectly = {
                    viewModel.clearStatusMessage()
                    viewModel.switchPortalMode(PortalMode.ADMIN_PORTAL)
                },
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = onSignedOut
                    )
                }
            )
        }

        PortalMode.MEMBER_PORTAL -> {
            val candidateMembers = buildList {
                if (boundMembersState is UiState.Success) {
                    addAll((boundMembersState as UiState.Success).data)
                }
                if (isAdmin && allMembersState is UiState.Success) {
                    addAll((allMembersState as UiState.Success).data)
                }
            }.distinctBy { it.docId }

            val activeMember = candidateMembers.find { it.docId == selectedMemberDocId }
                ?: candidateMembers.firstOrNull()

            val candidateTransactions = buildList {
                if (boundTransactionsState is UiState.Success) {
                    addAll((boundTransactionsState as UiState.Success).data)
                }
                if (isAdmin && allTransactionsState is UiState.Success) {
                    addAll((allTransactionsState as UiState.Success).data)
                }
            }.distinctBy { it.id }

            if (activeMember == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                MemberDashboardScreen(
                    member = activeMember,
                    transactions = candidateTransactions,
                    showRawCiphertext = showRawCiphertext,
                    statusMessage = statusMessage,
                    onToggleCiphertext = { viewModel.toggleCiphertextInspector() },
                    onChangePin = { currentPin, newPin ->
                        viewModel.changeMemberPin(activeMember, currentPin, newPin)
                    },
                    onBackToPortalGate = {
                        viewModel.clearStatusMessage()
                        viewModel.switchPortalMode(PortalMode.PORTAL_GATE)
                    }
                )
            }
        }

        PortalMode.ADMIN_PORTAL -> {
            val membersList = (allMembersState as? UiState.Success)?.data ?: emptyList()
            val txList = (allTransactionsState as? UiState.Success)?.data ?: emptyList()

            AdminConsoleScreen(
                members = membersList,
                transactions = txList,
                showRawCiphertext = showRawCiphertext,
                statusMessage = statusMessage,
                onToggleCiphertext = { viewModel.toggleCiphertextInspector() },
                onCreateMember = { mId, name, addr, mob, nid, nomName, nomAddr, nomNid, pin ->
                    viewModel.createMember(mId, name, addr, mob, nid, nomName, nomAddr, nomNid, pin)
                },
                onUpdateMember = { existing, mId, name, addr, mob, nid, nomName, nomAddr, nomNid, pinOpt ->
                    viewModel.updateMemberProfile(existing, mId, name, addr, mob, nid, nomName, nomAddr, nomNid, pinOpt)
                },
                onAddTransaction = { member, type, amount, monthYear, note, receipt ->
                    viewModel.addTransaction(member, type, amount, monthYear, note, receipt)
                },
                onUpdateTransaction = { existingTx, member, newType, newAmount, newMonth, newNote, newReceipt ->
                    viewModel.updateTransaction(existingTx, member, newType, newAmount, newMonth, newNote, newReceipt)
                },
                onDeleteTransaction = { existingTx, member ->
                    viewModel.deleteTransaction(existingTx, member)
                },
                onOpenMemberDashboardPreview = { docId ->
                    viewModel.selectMemberAccount(docId)
                },
                onBackToPortalGate = {
                    viewModel.clearStatusMessage()
                    viewModel.switchPortalMode(PortalMode.PORTAL_GATE)
                }
            )
        }
    }
}
