package com.example.security

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides authenticated AES-256-GCM field-level encryption at rest and SHA-256
 * capability hashing for Friendship Zone (2005), Kalir Bazar.
 *
 * Every sensitive PII and financial note field is encrypted with a fresh 96-bit
 * random IV and 128-bit GCM authentication tag before leaving the device.
 */
object CryptoVault {
    private const val PREFIX = "ENCv1:"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private const val VAULT_SEED = "FriendshipZone-2005-KalirBazar-Cooperative-AES256-VaultKey-v1"
    private const val HASH_SALT = "FZ2005-KalirBazar-Salt"

    const val DEFAULT_ADMIN_MASTER_PIN = "2005"

    private val secureRandom = SecureRandom()

    private val secretKeySpec: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(VAULT_SEED.toByteArray(StandardCharsets.UTF_8))
        SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext into `ENCv1:<Base64(IV + Ciphertext + AuthTag)>`.
     */
    fun encrypt(plaintext: String): String {
        val clean = plaintext.trim()
        if (clean.isEmpty()) return encrypt("N/A")
        if (clean.startsWith(PREFIX)) return clean
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec)
        val cipherBytes = cipher.doFinal(clean.toByteArray(StandardCharsets.UTF_8))

        val combined = ByteBuffer.allocate(iv.size + cipherBytes.size)
            .put(iv)
            .put(cipherBytes)
            .array()

        val encoded = Base64.getEncoder().encodeToString(combined)
        return "$PREFIX$encoded"
    }

    /**
     * Decrypts `ENCv1:<Base64(IV + Ciphertext + AuthTag)>` back to plaintext.
     */
    fun decrypt(ciphertext: String): String {
        if (ciphertext.isBlank()) return ""
        if (!ciphertext.startsWith(PREFIX)) return ciphertext
        return try {
            val rawBase64 = ciphertext.removePrefix(PREFIX)
            val combined = Base64.getDecoder().decode(rawBase64)
            if (combined.size <= IV_LENGTH_BYTES) return "[Encrypted Data]"

            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val encryptedPayload = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec)
            val plainBytes = cipher.doFinal(encryptedPayload)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "[Protected Record]"
        }
    }

    /**
     * Computes a deterministic 64-character lowercase hex SHA-256 digest.
     */
    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun normalizeMemberId(memberId: String): String {
        return memberId.trim().uppercase().replace("\\s+".toRegex(), "")
    }

    fun normalizeMobile(mobile: String): String {
        val digitsOnly = mobile.filter { it.isDigit() }
        return if (digitsOnly.startsWith("880") && digitsOnly.length == 13) {
            digitsOnly.removePrefix("88")
        } else {
            digitsOnly
        }
    }

    fun computePinHash(pin: String): String {
        return sha256Hex("$HASH_SALT:PIN:${pin.trim()}")
    }

    fun computeAdminCodeHash(code: String): String {
        return sha256Hex("$HASH_SALT:ADMIN:${code.trim()}")
    }

    fun computeMemberIdLookupHash(memberId: String): String {
        return sha256Hex("$HASH_SALT:MEMBER_ID:${normalizeMemberId(memberId)}")
    }

    fun computeMobileLookupHash(mobile: String): String {
        return sha256Hex("$HASH_SALT:MOBILE:${normalizeMobile(mobile)}")
    }

    /**
     * Capability binding document ID combining the normalized Member ID + PIN hash.
     */
    fun computeMemberIdBindingId(memberId: String, pinHash: String): String {
        return sha256Hex("BIND_ID:${normalizeMemberId(memberId)}:$pinHash")
    }

    /**
     * Capability binding document ID combining the normalized Mobile Number + PIN hash.
     */
    fun computeMobileBindingId(mobile: String, pinHash: String): String {
        return sha256Hex("BIND_MOB:${normalizeMobile(mobile)}:$pinHash")
    }
}
