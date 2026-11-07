package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DecryptedMember
import com.example.data.model.DecryptedTransaction
import com.example.data.model.TransactionType
import com.example.data.repository.CooperativeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PortalMode {
    PORTAL_GATE,
    MEMBER_PORTAL,
    ADMIN_PORTAL
}

@OptIn(ExperimentalCoroutinesApi::class)
class CooperativeViewModel(
    private val repository: CooperativeRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _portalMode = MutableStateFlow(PortalMode.PORTAL_GATE)
    val portalMode: StateFlow<PortalMode> = _portalMode.asStateFlow()

    private val _selectedMemberDocId = MutableStateFlow<String?>(null)
    val selectedMemberDocId: StateFlow<String?> = _selectedMemberDocId.asStateFlow()

    private val _showRawCiphertext = MutableStateFlow(false)
    val showRawCiphertext: StateFlow<Boolean> = _showRawCiphertext.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val isAdmin: StateFlow<Boolean> = repository.observeIsAdmin(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing admin status", firestoreError ?: error)
            emit(false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false
        )

    val boundMembersState: StateFlow<UiState<List<DecryptedMember>>> = repository.observeBoundMembers(currentUserId)
        .map<List<DecryptedMember>, UiState<List<DecryptedMember>>> { list ->
            if (_selectedMemberDocId.value == null && list.isNotEmpty()) {
                _selectedMemberDocId.value = list.first().docId
            }
            UiState.Success(list)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing bound members", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load member profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val boundTransactionsState: StateFlow<UiState<List<DecryptedTransaction>>> = repository.observeBoundTransactions(currentUserId)
        .map<List<DecryptedTransaction>, UiState<List<DecryptedTransaction>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing bound transactions", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load transactions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Only observe all members & all transactions when verified as Admin
    val allMembersState: StateFlow<UiState<List<DecryptedMember>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllMembers()
                    .map<List<DecryptedMember>, UiState<List<DecryptedMember>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        emit(UiState.Error(rootError.message ?: "Failed to load all members"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val allTransactionsState: StateFlow<UiState<List<DecryptedTransaction>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllTransactions()
                    .map<List<DecryptedTransaction>, UiState<List<DecryptedTransaction>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        emit(UiState.Error(rootError.message ?: "Failed to load all transactions"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun toggleCiphertextInspector() {
        _showRawCiphertext.value = !_showRawCiphertext.value
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectMemberAccount(docId: String) {
        _selectedMemberDocId.value = docId
        _portalMode.value = PortalMode.MEMBER_PORTAL
    }

    fun switchPortalMode(mode: PortalMode) {
        _portalMode.value = mode
    }

    fun unlockMemberWithPin(identifier: String, pin: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.loginAndBindMemberDevice(identifier, pin)
            _isBusy.value = false
            result.onSuccess { member ->
                _selectedMemberDocId.value = member.docId
                _portalMode.value = PortalMode.MEMBER_PORTAL
                _statusMessage.value = "Welcome, ${member.name}! Device synchronized in real-time."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to verify Member ID/Mobile & PIN."
            }
        }
    }

    fun unlockAdminConsole(displayName: String, adminPin: String) {
        viewModelScope.launch {
            if (isAdmin.value) {
                _portalMode.value = PortalMode.ADMIN_PORTAL
                return@launch
            }
            _isBusy.value = true
            val result = repository.registerAdminAccount(displayName, adminPin)
            _isBusy.value = false
            result.onSuccess {
                _portalMode.value = PortalMode.ADMIN_PORTAL
                _statusMessage.value = "Admin Console unlocked for Friendship Zone (2005), Kalir Bazar."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to unlock Admin Console."
            }
        }
    }

    fun createMember(
        memberId: String,
        name: String,
        address: String,
        mobileNumber: String,
        nidNumber: String,
        nomineeName: String,
        nomineeAddress: String,
        nomineeNidNumber: String,
        initialPin: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createMember(
                memberId = memberId,
                name = name,
                address = address,
                mobileNumber = mobileNumber,
                nidNumber = nidNumber,
                nomineeName = nomineeName,
                nomineeAddress = nomineeAddress,
                nomineeNidNumber = nomineeNidNumber,
                initialPin = initialPin
            )
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Encrypted profile created for $name ($memberId)."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to create member profile."
            }
        }
    }

    fun updateMemberProfile(
        existingMember: DecryptedMember,
        memberId: String,
        name: String,
        address: String,
        mobileNumber: String,
        nidNumber: String,
        nomineeName: String,
        nomineeAddress: String,
        nomineeNidNumber: String,
        newPinOptional: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateMemberProfile(
                existingMember = existingMember,
                memberId = memberId,
                name = name,
                address = address,
                mobileNumber = mobileNumber,
                nidNumber = nidNumber,
                nomineeName = nomineeName,
                nomineeAddress = nomineeAddress,
                nomineeNidNumber = nomineeNidNumber,
                newPinOptional = newPinOptional
            )
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Member profile updated & re-encrypted."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to update member profile."
            }
        }
    }

    fun changeMemberPin(
        member: DecryptedMember,
        currentPin: String,
        newPin: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.changeMemberPin(member, currentPin, newPin)
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Login PIN updated and synced across all devices."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to update PIN."
            }
        }
    }

    fun addTransaction(
        member: DecryptedMember,
        type: TransactionType,
        amount: Long,
        monthYear: String,
        note: String,
        receiptNumber: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addTransaction(
                member = member,
                type = type,
                amount = amount,
                monthYear = monthYear,
                note = note,
                receiptNumber = receiptNumber
            )
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "${type.label} of ৳$amount recorded for ${member.name}."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to record transaction."
            }
        }
    }

    fun updateTransaction(
        existingTx: DecryptedTransaction,
        member: DecryptedMember,
        newType: TransactionType,
        newAmount: Long,
        newMonthYear: String,
        newNote: String,
        newReceiptNumber: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateTransaction(
                existingTx = existingTx,
                member = member,
                newType = newType,
                newAmount = newAmount,
                newMonthYear = newMonthYear,
                newDateMillis = existingTx.transactionDate,
                newNote = newNote,
                newReceiptNumber = newReceiptNumber
            )
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Transaction updated & member balance recalculated."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to update transaction."
            }
        }
    }

    fun deleteTransaction(
        existingTx: DecryptedTransaction,
        member: DecryptedMember,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteTransaction(existingTx, member)
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = "Transaction removed and balance adjusted."
                onSuccess()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to delete transaction."
            }
        }
    }

    private companion object {
        const val TAG = "CooperativeVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
