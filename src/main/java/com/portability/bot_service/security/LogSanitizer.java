package com.portability.bot_service.security;

/**
 * Utility class for sanitizing sensitive data in logs.
 * 
 * CRITICAL: Use these methods whenever logging PII or sensitive data.
 * Never log sensitive data in plain text.
 */
public class LogSanitizer {

    /**
     * Mask email address for logging
     * Example: john.doe@example.com -> j***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }
        
        String[] parts = email.split("@");
        if (parts[0].isEmpty()) {
            return "***@" + parts[1];
        }
        
        return parts[0].charAt(0) + "***@" + parts[1];
    }
    
    /**
     * Mask phone number for logging
     * Shows only last 4 digits
     * Example: +525512345678 -> ***5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "******";
        }
        
        return "***" + phone.substring(phone.length() - 4);
    }
    
    /**
     * Mask NIP (portability code)
     * NEVER log NIPs, even partially
     */
    public static String maskNip(String nip) {
        return "****";
    }
    
    /**
     * Mask IMEI
     * Shows only last 4 digits
     * Example: 123456789012345 -> ***2345
     */
    public static String maskImei(String imei) {
        if (imei == null || imei.length() < 4) {
            return "***";
        }
        
        return "***" + imei.substring(imei.length() - 4);
    }
    
    /**
     * Mask URL with sensitive parameters
     * Removes everything after domain
     * Example: https://checkout.stripe.com/c/pay/cs_test_abc123 -> https://checkout.stripe.com/***
     */
    public static String maskUrl(String url) {
        if (url == null || !url.startsWith("http")) {
            return "***";
        }
        
        try {
            java.net.URL parsed = new java.net.URL(url);
            return parsed.getProtocol() + "://" + parsed.getHost() + "/***";
        } catch (Exception e) {
            return "***";
        }
    }
    
    /**
     * Mask address
     * Shows only district
     * Example: Calle Principal #123, Centro, CP 12345 -> ***, Centro, ***
     */
    public static String maskAddress(String address) {
        if (address == null) {
            return "***";
        }
        
        // Try to extract district (second part after comma)
        String[] parts = address.split(",");
        if (parts.length >= 2) {
            return "***, " + parts[1].trim() + ", ***";
        }
        
        return "***";
    }
    
    /**
     * Mask credit card number
     * Shows only last 4 digits
     */
    public static String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****-****-****-****";
        }
        
        String digitsOnly = cardNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 4) {
            return "****-****-****-****";
        }
        
        return "****-****-****-" + digitsOnly.substring(digitsOnly.length() - 4);
    }
}
