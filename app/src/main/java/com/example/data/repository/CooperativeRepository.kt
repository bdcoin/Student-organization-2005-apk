package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DecryptedMember
import com.example.data.model.DecryptedTransaction
import com.example.data.model.MemberAccount
import com.example.data.model.MemberBinding
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.model.currentMonthYearString
import com.example.security.CryptoVault
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CooperativeRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Observes whether the authenticated user is registered in /admins/{uid}
     * or is the bootstrapped admin email.
     */
    fun observeIsAdmin(uid: String): Flow<Boolean> = flow {
        val currentEmail = auth.currentUser?.email
        val isEmailVerified = auth.currentUser?.isEmailVerified == true
        if (currentEmail == "dragonboy20002@gmail.com" && isEmailVerified) {
            emit(true)
        }
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { snapshot ->
                    (currentEmail == "dragonboy20002@gmail.com" && isEmailVerified) || snapshot.exists()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Activates Admin access for the signed-in Google account after verifying the
     * committee Admin PIN.
     */
    suspend fun registerAdminAccount(displayName: String, adminPin: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            if (adminPin.trim() != CryptoVault.DEFAULT_ADMIN_MASTER_PIN) {
                return Result.failure(IllegalArgumentException("Invalid Committee Admin PIN. Use the Kalir Bazar committee PIN (2005)."))
            }
            val docRef = db.collection("admins").document(uid)
            val payload = mapOf(
                "userId" to uid,
                "adminCodeHash" to CryptoVault.computeAdminCodeHash(adminPin),
                "displayName" to displayName.ifBlank { "Kalir Bazar Admin" }.take(120),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unknown"
            handleFirestoreError(e, OperationType.CREATE, "admins/$uid")
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of ALL members (Admin only).
     */
    fun observeAllMembers(): Flow<List<DecryptedMember>> = flow {
        val uid = requireUserId()
        val path = "members"
        emitAll(
            db.collection("members")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            MemberAccount::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toDecrypted()
                    }.sortedBy { it.memberId }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Real-time stream of members bound to the current user's UID (Member portal).
     */
    fun observeBoundMembers(uid: String): Flow<List<DecryptedMember>> = flow {
        val requiredUid = requireUserId()
        val path = "members"
        emitAll(
            db.collection("members")
                .whereArrayContains("authorizedUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            MemberAccount::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toDecrypted()
                    }.sortedBy { it.memberId }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == requiredUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Real-time stream of ALL transactions (Admin only).
     */
    fun observeAllTransactions(): Flow<List<DecryptedTransaction>> = flow {
        val uid = requireUserId()
        val path = "transactions"
        emitAll(
            db.collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            TransactionRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toDecrypted()
                    }.sortedByDescending { it.transactionDate }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Real-time stream of transactions bound to the current user's UID (Member portal).
     */
    fun observeBoundTransactions(uid: String): Flow<List<DecryptedTransaction>> = flow {
        val requiredUid = requireUserId()
        val path = "transactions"
        emitAll(
            db.collection("transactions")
                .whereArrayContains("authorizedUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            TransactionRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toDecrypted()
                    }.sortedByDescending { it.transactionDate }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == requiredUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Admin creates a new Member Profile & Account with AES-256-GCM encryption
     * and writes non-PII capability bindings for Member ID + PIN and Mobile + PIN.
     */
    suspend fun createMember(
        memberId: String,
        name: String,
        address: String,
        mobileNumber: String,
        nidNumber: String,
        nomineeName: String,
        nomineeAddress: String,
        nomineeNidNumber: String,
        initialPin: String
    ): Result<String> {
        val uid = requireUserId()
        val cleanMemberId = CryptoVault.normalizeMemberId(memberId)
        val cleanMobile = CryptoVault.normalizeMobile(mobileNumber)
        val cleanPin = initialPin.trim()

        if (cleanMemberId.isEmpty() || name.isBlank() || cleanMobile.isEmpty() || cleanPin.length < 4) {
            return Result.failure(IllegalArgumentException("Member ID, Name, Mobile Number, and a 4+ digit PIN are required."))
        }

        val memberDocId = "mbr_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val pinHash = CryptoVault.computePinHash(cleanPin)
        val mobileLookupHash = CryptoVault.computeMobileLookupHash(cleanMobile)
        val memberIdLookupHash = CryptoVault.computeMemberIdLookupHash(cleanMemberId)

        val memberRef = db.collection("members").document(memberDocId)
        val payload = mapOf(
            "memberId" to cleanMemberId.take(64),
            "encryptedName" to CryptoVault.encrypt(name),
            "encryptedAddress" to CryptoVault.encrypt(address),
            "encryptedMobile" to CryptoVault.encrypt(cleanMobile),
            "encryptedNid" to CryptoVault.encrypt(nidNumber),
            "encryptedNomineeName" to CryptoVault.encrypt(nomineeName),
            "encryptedNomineeAddress" to CryptoVault.encrypt(nomineeAddress),
            "encryptedNomineeNid" to CryptoVault.encrypt(nomineeNidNumber),
            "pinHash" to pinHash,
            "mobileLookupHash" to mobileLookupHash,
            "memberIdLookupHash" to memberIdLookupHash,
            "lastBindProof" to "init",
            "totalDeposited" to 0L,
            "totalWithdrawn" to 0L,
            "authorizedUids" to listOf(uid),
            "transactionIds" to emptyList<String>(),
            "createdByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            memberRef.set(payload).await()
            writeMemberBindings(memberDocId, cleanMemberId, cleanMobile, pinHash, uid)
            Result.success(memberDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, memberRef.path)
            Result.failure(e)
        }
    }

    private suspend fun writeMemberBindings(
        memberDocId: String,
        memberId: String,
        mobileNumber: String,
        pinHash: String,
        uid: String
    ) {
        val idBindHash = CryptoVault.computeMemberIdBindingId(memberId, pinHash)
        val mobileBindHash = CryptoVault.computeMobileBindingId(mobileNumber, pinHash)
        val bindingPayload = mapOf(
            "memberDocId" to memberDocId,
            "createdByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val idRef = db.collection("member_bindings").document(idBindHash)
        try {
            idRef.set(bindingPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, idRef.path)
            throw e
        }

        if (mobileBindHash != idBindHash) {
            val mobRef = db.collection("member_bindings").document(mobileBindHash)
            try {
                mobRef.set(bindingPayload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, mobRef.path)
                throw e
            }
        }
    }

    /**
     * Admin edits an existing Member Profile (and optionally resets their login PIN).
     */
    suspend fun updateMemberProfile(
        existingMember: DecryptedMember,
        memberId: String,
        name: String,
        address: String,
        mobileNumber: String,
        nidNumber: String,
        nomineeName: String,
        nomineeAddress: String,
        nomineeNidNumber: String,
        newPinOptional: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val cleanMemberId = CryptoVault.normalizeMemberId(memberId)
        val cleanMobile = CryptoVault.normalizeMobile(mobileNumber)
        val newPinHash = if (!newPinOptional.isNullOrBlank()) {
            CryptoVault.computePinHash(newPinOptional.trim())
        } else {
            existingMember.pinHash
        }

        val docRef = db.collection("members").document(existingMember.docId)
        val updates = mapOf(
            "memberId" to cleanMemberId.take(64),
            "encryptedName" to CryptoVault.encrypt(name),
            "encryptedAddress" to CryptoVault.encrypt(address),
            "encryptedMobile" to CryptoVault.encrypt(cleanMobile),
            "encryptedNid" to CryptoVault.encrypt(nidNumber),
            "encryptedNomineeName" to CryptoVault.encrypt(nomineeName),
            "encryptedNomineeAddress" to CryptoVault.encrypt(nomineeAddress),
            "encryptedNomineeNid" to CryptoVault.encrypt(nomineeNidNumber),
            "pinHash" to newPinHash,
            "mobileLookupHash" to CryptoVault.computeMobileLookupHash(cleanMobile),
            "memberIdLookupHash" to CryptoVault.computeMemberIdLookupHash(cleanMemberId),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            writeMemberBindings(existingMember.docId, cleanMemberId, cleanMobile, newPinHash, uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Multi-device Member Login: binds the current authenticated Google UID to the
     * member account matching (Member ID OR Mobile Number) + PIN, and syncs transaction access.
     */
    suspend fun loginAndBindMemberDevice(identifier: String, pin: String): Result<DecryptedMember> {
        val uid = requireUserId()
        val cleanPin = pin.trim()
        if (identifier.isBlank() || cleanPin.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your Member ID or Mobile Number and PIN."))
        }

        val pinHash = CryptoVault.computePinHash(cleanPin)
        val idBindHash = CryptoVault.computeMemberIdBindingId(identifier, pinHash)
        val mobileBindHash = CryptoVault.computeMobileBindingId(identifier, pinHash)

        return try {
            var bindingSnap = db.collection("member_bindings").document(idBindHash).get().await()
            if (!bindingSnap.exists()) {
                bindingSnap = db.collection("member_bindings").document(mobileBindHash).get().await()
            }
            if (!bindingSnap.exists()) {
                return Result.failure(IllegalArgumentException("Invalid Member ID / Mobile Number or PIN."))
            }

            val binding = bindingSnap.toObject(MemberBinding::class.java)
                ?: return Result.failure(IllegalStateException("Invalid binding record."))
            val memberDocId = binding.memberDocId
            val memberRef = db.collection("members").document(memberDocId)

            // Action 3: Prove knowledge of pinHash via lastBindProof = "$uid:$pinHash" and append uid
            memberRef.update(
                mapOf(
                    "authorizedUids" to FieldValue.arrayUnion(uid),
                    "lastBindProof" to "$uid:$pinHash",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val memberSnap = memberRef.get().await()
            val memberAccount = memberSnap.toObject(
                MemberAccount::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Member profile not found."))

            // Sync current device UID onto existing transactions for this member
            for (txId in memberAccount.transactionIds) {
                val txRef = db.collection("transactions").document(txId)
                try {
                    txRef.update(
                        mapOf(
                            "authorizedUids" to FieldValue.arrayUnion(uid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {
                    // Ignore individual missing transactions
                }
            }

            Result.success(memberAccount.toDecrypted())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "member_bindings")
            Result.failure(e)
        }
    }

    /**
     * Allows a Member (or Admin) to change their login PIN and updates capability bindings
     * so future multi-device logins use the new PIN.
     */
    suspend fun changeMemberPin(
        member: DecryptedMember,
        currentPin: String,
        newPin: String
    ): Result<Unit> {
        val uid = requireUserId()
        val currentPinHash = CryptoVault.computePinHash(currentPin.trim())
        if (currentPinHash != member.pinHash) {
            return Result.failure(IllegalArgumentException("Current PIN is incorrect."))
        }
        val cleanNewPin = newPin.trim()
        if (cleanNewPin.length < 4) {
            return Result.failure(IllegalArgumentException("New PIN must be at least 4 digits."))
        }

        val newPinHash = CryptoVault.computePinHash(cleanNewPin)
        val memberRef = db.collection("members").document(member.docId)

        return try {
            memberRef.update(
                mapOf(
                    "pinHash" to newPinHash,
                    "lastBindProof" to "$uid:$newPinHash",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Remove old bindings and create new bindings for seamless multi-device login
            val oldIdHash = CryptoVault.computeMemberIdBindingId(member.memberId, currentPinHash)
            val oldMobHash = CryptoVault.computeMobileBindingId(member.mobileNumber, currentPinHash)
            try {
                db.collection("member_bindings").document(oldIdHash).delete().await()
                if (oldMobHash != oldIdHash) {
                    db.collection("member_bindings").document(oldMobHash).delete().await()
                }
            } catch (_: Exception) {
            }

            writeMemberBindings(
                memberDocId = member.docId,
                memberId = member.memberId,
                mobileNumber = member.mobileNumber,
                pinHash = newPinHash,
                uid = uid
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
            Result.failure(e)
        }
    }

    /**
     * Admin records a new financial transaction (DEPOSIT or WITHDRAWAL) and updates
     * the member's cumulative balance in real-time.
     */
    suspend fun addTransaction(
        member: DecryptedMember,
        type: TransactionType,
        amount: Long,
        monthYear: String = currentMonthYearString(),
        transactionDateMillis: Long = System.currentTimeMillis(),
        note: String,
        receiptNumber: String
    ): Result<String> {
        val uid = requireUserId()
        if (amount <= 0L) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0."))
        }

        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val txRef = db.collection("transactions").document(txId)
        val memberRef = db.collection("members").document(member.docId)

        val cleanReceipt = receiptNumber.ifBlank {
            "FZ-${monthYear.replace("-", "")}-${txId.takeLast(4).uppercase()}"
        }.take(64)

        val authorizedList = (member.authorizedUids + uid).distinct().take(10)
        val updatedTxIds = (member.transactionIds + txId).distinct().takeLast(100)

        val newTotalDeposited = if (type == TransactionType.DEPOSIT) {
            member.totalDeposited + amount
        } else {
            member.totalDeposited
        }
        val newTotalWithdrawn = if (type == TransactionType.WITHDRAWAL) {
            member.totalWithdrawn + amount
        } else {
            member.totalWithdrawn
        }

        val txPayload = mapOf(
            "memberDocId" to member.docId,
            "memberId" to member.memberId,
            "encryptedMemberName" to CryptoVault.encrypt(member.name),
            "type" to type.name,
            "amount" to amount,
            "monthYear" to monthYear,
            "transactionDate" to transactionDateMillis,
            "encryptedNote" to CryptoVault.encrypt(note.ifBlank { "${type.label} for $monthYear" }),
            "receiptNumber" to cleanReceipt,
            "authorizedUids" to authorizedList,
            "createdByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            txRef.set(txPayload).await()
            memberRef.update(
                mapOf(
                    "totalDeposited" to newTotalDeposited,
                    "totalWithdrawn" to newTotalWithdrawn,
                    "transactionIds" to updatedTxIds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, txRef.path)
            Result.failure(e)
        }
    }

    /**
     * Admin edits an existing financial transaction and updates the member's cumulative
     * deposit/withdrawal totals accordingly.
     */
    suspend fun updateTransaction(
        existingTx: DecryptedTransaction,
        member: DecryptedMember,
        newType: TransactionType,
        newAmount: Long,
        newMonthYear: String,
        newDateMillis: Long,
        newNote: String,
        newReceiptNumber: String
    ): Result<Unit> {
        requireUserId()
        if (newAmount <= 0L) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0."))
        }

        val txRef = db.collection("transactions").document(existingTx.id)
        val memberRef = db.collection("members").document(member.docId)

        // Reverse old transaction impact and apply new transaction impact
        var adjustedDeposited = member.totalDeposited
        var adjustedWithdrawn = member.totalWithdrawn

        if (existingTx.type == TransactionType.DEPOSIT) {
            adjustedDeposited = (adjustedDeposited - existingTx.amount).coerceAtLeast(0L)
        } else {
            adjustedWithdrawn = (adjustedWithdrawn - existingTx.amount).coerceAtLeast(0L)
        }

        if (newType == TransactionType.DEPOSIT) {
            adjustedDeposited += newAmount
        } else {
            adjustedWithdrawn += newAmount
        }

        val txUpdates = mapOf(
            "memberId" to member.memberId,
            "encryptedMemberName" to CryptoVault.encrypt(member.name),
            "type" to newType.name,
            "amount" to newAmount,
            "monthYear" to newMonthYear,
            "transactionDate" to newDateMillis,
            "encryptedNote" to CryptoVault.encrypt(newNote.ifBlank { "${newType.label} for $newMonthYear" }),
            "receiptNumber" to newReceiptNumber.ifBlank { existingTx.receiptNumber }.take(64),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            txRef.update(txUpdates).await()
            memberRef.update(
                mapOf(
                    "totalDeposited" to adjustedDeposited,
                    "totalWithdrawn" to adjustedWithdrawn,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, txRef.path)
            Result.failure(e)
        }
    }

    /**
     * Admin deletes a transaction and reverses its impact on the member's totals.
     */
    suspend fun deleteTransaction(
        existingTx: DecryptedTransaction,
        member: DecryptedMember
    ): Result<Unit> {
        requireUserId()
        val txRef = db.collection("transactions").document(existingTx.id)
        val memberRef = db.collection("members").document(member.docId)

        val adjustedDeposited = if (existingTx.type == TransactionType.DEPOSIT) {
            (member.totalDeposited - existingTx.amount).coerceAtLeast(0L)
        } else {
            member.totalDeposited
        }
        val adjustedWithdrawn = if (existingTx.type == TransactionType.WITHDRAWAL) {
            (member.totalWithdrawn - existingTx.amount).coerceAtLeast(0L)
        } else {
            member.totalWithdrawn
        }
        val updatedTxIds = member.transactionIds.filterNot { it == existingTx.id }

        return try {
            txRef.delete().await()
            memberRef.update(
                mapOf(
                    "totalDeposited" to adjustedDeposited,
                    "totalWithdrawn" to adjustedWithdrawn,
                    "transactionIds" to updatedTxIds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, txRef.path)
            Result.failure(e)
        }
    }
}
