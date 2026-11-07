package com.example.data.model

import com.example.security.CryptoVault
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@IgnoreExtraProperties
data class AdminProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val adminCodeHash: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MemberAccount(
    @DocumentId val id: String = "",
    val memberId: String = "",
    val encryptedName: String = "",
    val encryptedAddress: String = "",
    val encryptedMobile: String = "",
    val encryptedNid: String = "",
    val encryptedNomineeName: String = "",
    val encryptedNomineeAddress: String = "",
    val encryptedNomineeNid: String = "",
    val pinHash: String = "",
    val mobileLookupHash: String = "",
    val memberIdLookupHash: String = "",
    val lastBindProof: String = "init",
    val totalDeposited: Long = 0L,
    val totalWithdrawn: Long = 0L,
    val authorizedUids: List<String> = emptyList(),
    val transactionIds: List<String> = emptyList(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDecrypted(): DecryptedMember {
        return DecryptedMember(
            docId = id,
            memberId = memberId,
            name = CryptoVault.decrypt(encryptedName),
            address = CryptoVault.decrypt(encryptedAddress),
            mobileNumber = CryptoVault.decrypt(encryptedMobile),
            nidNumber = CryptoVault.decrypt(encryptedNid),
            nomineeName = CryptoVault.decrypt(encryptedNomineeName),
            nomineeAddress = CryptoVault.decrypt(encryptedNomineeAddress),
            nomineeNidNumber = CryptoVault.decrypt(encryptedNomineeNid),
            encryptedNameRaw = encryptedName,
            encryptedMobileRaw = encryptedMobile,
            encryptedNidRaw = encryptedNid,
            pinHash = pinHash,
            totalDeposited = totalDeposited,
            totalWithdrawn = totalWithdrawn,
            netBalance = totalDeposited - totalWithdrawn,
            authorizedUids = authorizedUids,
            transactionIds = transactionIds,
            createdByUid = createdByUid,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

@IgnoreExtraProperties
data class MemberBinding(
    @DocumentId val id: String = "",
    val memberDocId: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val memberDocId: String = "",
    val memberId: String = "",
    val encryptedMemberName: String = "",
    val type: String = "DEPOSIT",
    val amount: Long = 0L,
    val monthYear: String = "",
    val transactionDate: Long = 0L,
    val encryptedNote: String = "",
    val receiptNumber: String = "",
    val authorizedUids: List<String> = emptyList(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDecrypted(): DecryptedTransaction {
        return DecryptedTransaction(
            id = id,
            memberDocId = memberDocId,
            memberId = memberId,
            memberName = CryptoVault.decrypt(encryptedMemberName),
            type = TransactionType.fromString(type),
            amount = amount,
            monthYear = monthYear,
            transactionDate = transactionDate,
            note = CryptoVault.decrypt(encryptedNote),
            encryptedNoteRaw = encryptedNote,
            receiptNumber = receiptNumber,
            authorizedUids = authorizedUids,
            createdByUid = createdByUid,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

enum class TransactionType(val label: String) {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal");

    companion object {
        fun fromString(value: String): TransactionType {
            return if (value.equals("WITHDRAWAL", ignoreCase = true)) WITHDRAWAL else DEPOSIT
        }
    }
}

data class DecryptedMember(
    val docId: String,
    val memberId: String,
    val name: String,
    val address: String,
    val mobileNumber: String,
    val nidNumber: String,
    val nomineeName: String,
    val nomineeAddress: String,
    val nomineeNidNumber: String,
    val encryptedNameRaw: String,
    val encryptedMobileRaw: String,
    val encryptedNidRaw: String,
    val pinHash: String,
    val totalDeposited: Long,
    val totalWithdrawn: Long,
    val netBalance: Long,
    val authorizedUids: List<String>,
    val transactionIds: List<String>,
    val createdByUid: String,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?
)

data class DecryptedTransaction(
    val id: String,
    val memberDocId: String,
    val memberId: String,
    val memberName: String,
    val type: TransactionType,
    val amount: Long,
    val monthYear: String,
    val transactionDate: Long,
    val note: String,
    val encryptedNoteRaw: String,
    val receiptNumber: String,
    val authorizedUids: List<String>,
    val createdByUid: String,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?
)

data class MonthlySummary(
    val monthYear: String,
    val displayTitle: String,
    val totalDeposited: Long,
    val totalWithdrawn: Long,
    val netFlow: Long,
    val transactionCount: Int,
    val transactions: List<DecryptedTransaction>
)

fun buildMonthlySummaries(transactions: List<DecryptedTransaction>): List<MonthlySummary> {
    val grouped = transactions.groupBy { it.monthYear }
    return grouped.entries
        .sortedByDescending { it.key }
        .map { (monthYear, txList) ->
            val sortedTx = txList.sortedByDescending { it.transactionDate }
            val deposits = sortedTx.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount }
            val withdrawals = sortedTx.filter { it.type == TransactionType.WITHDRAWAL }.sumOf { it.amount }
            MonthlySummary(
                monthYear = monthYear,
                displayTitle = formatMonthYearLabel(monthYear),
                totalDeposited = deposits,
                totalWithdrawn = withdrawals,
                netFlow = deposits - withdrawals,
                transactionCount = sortedTx.size,
                transactions = sortedTx
            )
        }
}

fun formatMonthYearLabel(monthYear: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
        val date = parser.parse(monthYear) ?: return monthYear
        formatter.format(date)
    } catch (e: Exception) {
        monthYear
    }
}

fun currentMonthYearString(epochMillis: Long = System.currentTimeMillis()): String {
    val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
    return formatter.format(Date(epochMillis))
}
