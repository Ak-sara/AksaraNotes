package com.aksara.notes.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKeyFactory

/**
 * Helper class for encrypting and decrypting backup data using AES-256-GCM
 * with PBKDF2 key derivation from the user's master password
 */
class EncryptionHelper {
    
    companion object {
        private const val TAG = "EncryptionHelper"
        
        // Encryption constants
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 12 // 96 bits for GCM
        private const val TAG_SIZE = 128 // 128 bits authentication tag
        
        // PBKDF2 constants
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 100000 // OWASP recommended minimum
        private const val SALT_SIZE = 32 // 256 bits
        
        // Backup file format constants
        private const val BACKUP_VERSION = 1
        private const val BACKUP_MAGIC = "AKSARA_BACKUP"
    }
    
    /**
     * Backup file structure (binary format):
     * - Magic string (12 bytes): "AKSARA_BACKUP"
     * - Version (4 bytes): int32
     * - Salt length (4 bytes): int32
     * - Salt (variable): byte array
     * - IV length (4 bytes): int32  
     * - IV (variable): byte array
     * - Encrypted data length (4 bytes): int32
     * - Encrypted data (variable): byte array with authentication tag
     */
    
    data class EncryptedBackup(
        val salt: ByteArray,
        val iv: ByteArray,
        val encryptedData: ByteArray
    )
    
    /**
     * Encrypts backup data using the user's master password
     * @param data The JSON data to encrypt
     * @param masterPassword The user's master password
     * @return EncryptedBackup containing salt, IV, and encrypted data
     */
    fun encryptBackupData(data: String, masterPassword: String): EncryptedBackup {
        try {
            Log.d(TAG, "Starting backup encryption")
            
            // Generate random salt for PBKDF2
            val salt = generateRandomBytes(SALT_SIZE)
            
            // Derive key from master password using PBKDF2
            val secretKey = deriveKeyFromPassword(masterPassword, salt)
            
            // Generate random IV for AES-GCM
            val iv = generateRandomBytes(IV_SIZE)
            
            // Encrypt the data
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            
            val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            
            Log.d(TAG, "Backup encryption completed successfully")
            Log.d(TAG, "Salt size: ${salt.size}, IV size: ${iv.size}, Encrypted data size: ${encryptedData.size}")
            
            return EncryptedBackup(salt, iv, encryptedData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting backup data", e)
            throw SecurityException("Failed to encrypt backup data: ${e.message}", e)
        }
    }
    
    /**
     * Decrypts backup data using the user's master password
     * @param encryptedBackup The encrypted backup data
     * @param masterPassword The user's master password
     * @return Decrypted JSON string
     */
    fun decryptBackupData(encryptedBackup: EncryptedBackup, masterPassword: String): String {
        try {
            Log.d(TAG, "Starting backup decryption")
            
            // Derive key from master password using the same salt
            val secretKey = deriveKeyFromPassword(masterPassword, encryptedBackup.salt)
            
            // Decrypt the data
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, encryptedBackup.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBackup.encryptedData)
            val decryptedData = String(decryptedBytes, StandardCharsets.UTF_8)
            
            Log.d(TAG, "Backup decryption completed successfully")
            
            return decryptedData
            
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting backup data", e)
            throw SecurityException("Failed to decrypt backup data. Check your password: ${e.message}", e)
        }
    }
    
    /**
     * Converts EncryptedBackup to binary format for file storage
     */
    fun encryptedBackupToBytes(backup: EncryptedBackup): ByteArray {
        try {
            val magic = BACKUP_MAGIC.toByteArray(StandardCharsets.UTF_8)
            val version = BACKUP_VERSION
            
            // Calculate total size
            val totalSize = magic.size + 4 + // magic + version
                           4 + backup.salt.size + // salt length + salt
                           4 + backup.iv.size + // iv length + iv  
                           4 + backup.encryptedData.size // encrypted data length + data
            
            val buffer = ByteArray(totalSize)
            var offset = 0
            
            // Write magic string
            System.arraycopy(magic, 0, buffer, offset, magic.size)
            offset += magic.size
            
            // Write version
            writeInt32(buffer, offset, version)
            offset += 4
            
            // Write salt
            writeInt32(buffer, offset, backup.salt.size)
            offset += 4
            System.arraycopy(backup.salt, 0, buffer, offset, backup.salt.size)
            offset += backup.salt.size
            
            // Write IV
            writeInt32(buffer, offset, backup.iv.size)
            offset += 4
            System.arraycopy(backup.iv, 0, buffer, offset, backup.iv.size)
            offset += backup.iv.size
            
            // Write encrypted data
            writeInt32(buffer, offset, backup.encryptedData.size)
            offset += 4
            System.arraycopy(backup.encryptedData, 0, buffer, offset, backup.encryptedData.size)
            
            return buffer
            
        } catch (e: Exception) {
            Log.e(TAG, "Error converting backup to bytes", e)
            throw IllegalStateException("Failed to serialize backup data", e)
        }
    }
    
    /**
     * Converts binary backup file data to EncryptedBackup
     */
    fun bytesToEncryptedBackup(data: ByteArray): EncryptedBackup {
        try {
            var offset = 0
            
            // Read and verify magic string
            val magic = ByteArray(BACKUP_MAGIC.length)
            System.arraycopy(data, offset, magic, 0, magic.size)
            offset += magic.size
            
            if (String(magic, StandardCharsets.UTF_8) != BACKUP_MAGIC) {
                throw IllegalArgumentException("Invalid backup file format")
            }
            
            // Read version
            val version = readInt32(data, offset)
            offset += 4
            
            if (version != BACKUP_VERSION) {
                throw IllegalArgumentException("Unsupported backup file version: $version")
            }
            
            // Read salt
            val saltLength = readInt32(data, offset)
            offset += 4
            val salt = ByteArray(saltLength)
            System.arraycopy(data, offset, salt, 0, saltLength)
            offset += saltLength
            
            // Read IV
            val ivLength = readInt32(data, offset)
            offset += 4
            val iv = ByteArray(ivLength)
            System.arraycopy(data, offset, iv, 0, ivLength)
            offset += ivLength
            
            // Read encrypted data
            val encryptedDataLength = readInt32(data, offset)
            offset += 4
            val encryptedData = ByteArray(encryptedDataLength)
            System.arraycopy(data, offset, encryptedData, 0, encryptedDataLength)
            
            return EncryptedBackup(salt, iv, encryptedData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing backup file", e)
            throw IllegalArgumentException("Invalid backup file format: ${e.message}", e)
        }
    }
    
    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
    
    private fun generateRandomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return bytes
    }
    
    private fun writeInt32(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value shr 24).toByte()
        buffer[offset + 1] = (value shr 16).toByte()
        buffer[offset + 2] = (value shr 8).toByte()
        buffer[offset + 3] = value.toByte()
    }
    
    private fun readInt32(buffer: ByteArray, offset: Int): Int {
        return ((buffer[offset].toInt() and 0xFF) shl 24) or
               ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
               ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
               (buffer[offset + 3].toInt() and 0xFF)
    }
}