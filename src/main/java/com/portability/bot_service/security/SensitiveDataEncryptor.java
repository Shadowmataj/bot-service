package com.portability.bot_service.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for encrypting and decrypting sensitive data using AES-256-GCM.
 * 
 * CRITICAL: This service encrypts sensitive PII data before storage.
 * The encryption key MUST be stored securely (environment variable, secrets manager).
 * 
 * Fields that should be encrypted:
 * - portability_nip (4-digit portability code)
 * - portability_imei (device IMEI number)
 * - checkout_session_url (Stripe payment URL with token)
 * - customer_email (PII)
 * - customer_phone (PII)
 * - address_full (PII)
 */
@Service
public class SensitiveDataEncryptor {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataEncryptor.class);
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    @Value("${security.encryption.key:#{null}}")
    private String encryptionKey;
    
    /**
     * Encrypt plaintext using AES-256-GCM
     * 
     * @param plaintext The string to encrypt
     * @return Base64-encoded encrypted string (IV + ciphertext)
     * @throws SecurityException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        validateEncryptionKey();
        
        try {
            SecretKey key = getKeyFromString(encryptionKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + ciphertext for storage
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new SecurityException("Failed to encrypt sensitive data", e);
        }
    }
    
    /**
     * Decrypt Base64-encoded encrypted string
     * 
     * @param encrypted Base64-encoded string (IV + ciphertext)
     * @return Decrypted plaintext
     * @throws SecurityException if decryption fails
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return encrypted;
        }
        
        validateEncryptionKey();
        
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            
            // Extract IV and ciphertext
            byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);
            
            SecretKey key = getKeyFromString(encryptionKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new SecurityException("Failed to decrypt sensitive data", e);
        }
    }
    
    /**
     * Check if encryption is enabled (key is configured)
     */
    public boolean isEncryptionEnabled() {
        return encryptionKey != null && !encryptionKey.isEmpty();
    }
    
    private void validateEncryptionKey() {
        if (!isEncryptionEnabled()) {
            throw new IllegalStateException(
                "Encryption key not configured. Set 'security.encryption.key' in environment variables."
            );
        }
    }
    
    private SecretKey getKeyFromString(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}
