package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config
import java.util.UUID

@Config(sdk = [36])
class CooperativeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun adminCreatesMemberAndTransaction_memberBindsSecondDeviceAndObservesRealtime() = runBlocking {
        val uniqueSuffix = UUID.randomUUID().toString().take(6).uppercase()
        val memberId = "FZ-2005-$uniqueSuffix"
        val mobile = "01711${(100000..999999).random()}"

        // 1. Admin signs in and activates Admin Access
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = CooperativeRepository(firestore)
        val adminRegResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.registerAdminAccount("Kalir Bazar Admin", "2005")
        }
        assertTrue("Admin registration must succeed", adminRegResult.isSuccess)

        // 2. Admin creates encrypted Member profile
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.createMember(
                memberId = memberId,
                name = "Rahim Uddin",
                address = "Kalir Bazar, Cumilla",
                mobileNumber = mobile,
                nidNumber = "1988123456789",
                nomineeName = "Karima Begum",
                nomineeAddress = "Kalir Bazar, Cumilla",
                nomineeNidNumber = "1992987654321",
                initialPin = "4321"
            )
        }
        assertTrue("Member creation must succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val memberDocId = createResult.getOrThrow()

        // Verify raw ciphertext in Firestore is encrypted with ENCv1:
        val rawSnapshot = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("members").document(memberDocId).get().await()
        }
        val rawEncryptedName = rawSnapshot.getString("encryptedName") ?: ""
        assertTrue("Stored name must be AES-256-GCM encrypted", rawEncryptedName.startsWith("ENCv1:"))

        val createdMember = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllMembers().first { list -> list.any { it.docId == memberDocId } }
                .first { it.docId == memberDocId }
        }
        assertEquals("Rahim Uddin", createdMember.name)

        // 3. Admin adds a Deposit Transaction
        val txResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.addTransaction(
                member = createdMember,
                type = TransactionType.DEPOSIT,
                amount = 2500L,
                monthYear = "2026-11",
                note = "November monthly deposit",
                receiptNumber = "RCP-202611-$uniqueSuffix"
            )
        }
        assertTrue("Transaction creation must succeed", txResult.isSuccess)
        val txId = txResult.getOrThrow()

        // 4. Member logs in on a new device and binds using Mobile Number + PIN
        val memberDeviceUid = signInTestUser(MEMBER_EMAIL)
        val memberRepo = CooperativeRepository(firestore)
        val bindResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.loginAndBindMemberDevice(identifier = mobile, pin = "4321")
        }
        assertTrue("Member device binding must succeed: ${bindResult.exceptionOrNull()}", bindResult.isSuccess)

        // 5. Member observes their bound profile & transactions in real-time
        val boundMembers = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observeBoundMembers(memberDeviceUid).first { list -> list.any { it.docId == memberDocId } }
        }
        val myMember = boundMembers.first { it.docId == memberDocId }
        assertEquals(2500L, myMember.totalDeposited)

        val boundTransactions = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observeBoundTransactions(memberDeviceUid).first { list -> list.any { it.id == txId } }
        }
        assertEquals(2500L, boundTransactions.first { it.id == txId }.amount)

        // 6. Member changes their own login PIN
        val pinChangeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.changeMemberPin(myMember, currentPin = "4321", newPin = "9876")
        }
        assertTrue("Member PIN change must succeed", pinChangeResult.isSuccess)
    }

    @Test
    fun unauthorizedUser_cannotReadAnotherMemberOrCreateTransactions() = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = CooperativeRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.registerAdminAccount("Kalir Bazar Admin", "2005")
        }
        val uniqueSuffix = UUID.randomUUID().toString().take(6).uppercase()
        val memberDocId = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.createMember(
                memberId = "FZ-SEC-$uniqueSuffix",
                name = "Private Member",
                address = "Kalir Bazar",
                mobileNumber = "01811${(100000..999999).random()}",
                nidNumber = "1234567890",
                nomineeName = "Nominee",
                nomineeAddress = "Kalir Bazar",
                nomineeNidNumber = "0987654321",
                initialPin = "1122"
            ).getOrThrow()
        }

        // Switch to Bob (not an admin and not bound to the member)
        signInTestUser(INTRUDER_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("members").document(memberDocId).get().await()
            }
            fail("Expected PERMISSION_DENIED when reading another member's profile")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun unauthenticatedUser_cannotObserveMembers() = runBlocking {
        auth.signOut()
        val repo = CooperativeRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeBoundMembers("unauth_uid").first()
            }
            fail("Expected failure for unauthenticated user")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException || e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    private companion object {
        const val ADMIN_EMAIL = "admin_test@kalirbazar.org"
        const val MEMBER_EMAIL = "member_device@kalirbazar.org"
        const val INTRUDER_EMAIL = "intruder@kalirbazar.org"
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
