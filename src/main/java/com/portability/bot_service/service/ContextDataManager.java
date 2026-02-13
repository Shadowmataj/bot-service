package com.portability.bot_service.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.portability.bot_service.model.dto.AddressResponse;
import com.portability.bot_service.model.dto.CheckoutSessionResponse;
import com.portability.bot_service.model.dto.CustomerResponse;
import com.portability.bot_service.model.dto.OrderResponse;
import com.portability.bot_service.model.dto.PortabilityResponse;
import com.portability.bot_service.model.dto.ScrapeResponse;
import com.portability.bot_service.model.dto.SimCardResponse;
import com.portability.bot_service.security.LogSanitizer;
import com.portability.bot_service.security.SensitiveDataEncryptor;

/**
 * Service responsible for extracting and storing relevant information from tool
 * responses into the conversation context_data. This enables the bot to
 * maintain state across multiple interactions and reuse previously collected
 * information.
 *
 * SECURITY NOTE: This service handles PII (Personally Identifiable
 * Information). - Logs are sanitized to prevent exposure of sensitive data -
 * Sensitive fields (NIP, IMEI, checkout URLs) are encrypted using AES-256-GCM -
 * Data is automatically cleaned up after retention period (see
 * ContextDataCleanupService)
 */
@Service
public class ContextDataManager {

    private static final Logger logger = LoggerFactory.getLogger(ContextDataManager.class);

    private final ConversationStateService stateService;
    private final SensitiveDataEncryptor encryptor;

    public ContextDataManager(ConversationStateService stateService, SensitiveDataEncryptor encryptor) {
        this.stateService = stateService;
        this.encryptor = encryptor;
    }

    /**
     * Process tool response and extract relevant data to store in context
     */
    public void processToolResponse(String conversationId, String toolName, Object toolResponse) {
        try {
            logger.info("Processing tool response for tool: {} in conversation: {}", toolName, conversationId);

            Map<String, Object> extractedData = extractRelevantData(toolName, toolResponse);

            if (!extractedData.isEmpty()) {
                stateService.storeContextData(conversationId, extractedData);
                logger.info("Stored {} context data entries from tool: {}", extractedData.size(), toolName);
            }

        } catch (Exception e) {
            logger.error("Error processing tool response for tool: {}", toolName, e);
        }
    }

    /**
     * Extract relevant data based on tool type and response
     */
    private Map<String, Object> extractRelevantData(String toolName, Object toolResponse) {
        Map<String, Object> data = new HashMap<>();

        if (toolResponse == null) {
            return data;
        }

        switch (toolName) {
            case "registerCustomer":
            case "getCustomerById":
            case "getCustomerByEmail":
            case "getCustomerByPhoneNumber":
                if (toolResponse instanceof CustomerResponse customer) {
                    data.put("customer_id", customer.id());
                    data.put("customer_name", customer.firstName() + " " + customer.lastName());
                    data.put("customer_first_name", customer.firstName());
                    data.put("customer_last_name", customer.lastName());
                    data.put("customer_email", customer.email());
                    // SECURITY: Sanitize PII in logs
                    logger.debug("Extracted customer data: ID={}, email={}",
                            customer.id(),
                            LogSanitizer.maskEmail(customer.email()));
                }
                break;

            case "createAddress":
                if (toolResponse instanceof AddressResponse address) {
                    data.put("address_id", address.getId());
                    data.put("address_district", address.getDistrict());
                    data.put("address_postal_code", address.getPostalCode());
                    // SECURITY: Sanitize address in logs
                    logger.debug("Extracted address data: ID={}, district={}",
                            address.getId(),
                            address.getDistrict());
                }
                break;

            case "createNewOrderForSimCardPurchase":
            case "createOrderForSimCardPortabilityPurchase":
                if (toolResponse instanceof OrderResponse order) {
                    data.put("order_id", order.id());
                    data.put("order_product_id", order.productId());
                    data.put("last_order_id", order.id()); // For easy reference
                    logger.debug("Extracted order data: ID={}", order.id());
                }
                break;

            case "getPortabilityByPhoneNumber":
            case "updateImei":
            case "updatePortabilityNip":
                if (toolResponse instanceof PortabilityResponse portability) {
                    data.put("portability_id", portability.getId());

                    // SECURITY: Encrypt sensitive data before storing
                    if (portability.getImei() != null) {
                        String encryptedImei = encryptor.encrypt(portability.getImei());
                        data.put("portability_imei", encryptedImei);
                    }
                    if (portability.getPortabilityNip() != null) {
                        String encryptedNip = encryptor.encrypt(portability.getPortabilityNip());
                        data.put("portability_nip", encryptedNip);
                    }

                    data.put("portability_order_id", portability.getOrderId());
                    // SECURITY: NEVER log NIP or IMEI in plain text
                    logger.debug("Extracted portability data: ID={}, phone={}, has_imei={}, has_nip={}",
                            portability.getId(),
                            LogSanitizer.maskPhone(portability.getPhoneNumber()),
                            portability.getImei() != null,
                            portability.getPortabilityNip() != null);
                }
                break;

            case "getSimIcc":
                if (toolResponse instanceof SimCardResponse simCardResponse) {
                    String encryptedIcc = encryptor.encrypt(simCardResponse.icc());
                    data.put("sim_card_icc", encryptedIcc);
                    // SECURITY: Mask ICC in logs (contains sensitive information)
                    logger.debug("Extracted SIM card data");
                }
                break;

            case "Create_checkout_session":
                if (toolResponse instanceof CheckoutSessionResponse checkout) {
                    // SECURITY: Encrypt checkout URL (contains payment token)
                    String encryptedUrl = encryptor.encrypt(checkout.stripe_session_url());
                    data.put("checkout_session_url", encryptedUrl);

                    data.put("payment_completed", false); // Initial state
                    // SECURITY: Mask URL in logs (contains payment token)
                    logger.debug("Extracted checkout data: session_id={}, url={}",
                            checkout.checkout_session_id(),
                            LogSanitizer.maskUrl(checkout.stripe_session_url()));
                }
                break;

            case "scrapeImeiCompatibility":
                if (toolResponse instanceof ScrapeResponse scrape) {
                    data.put("imei_compatible", scrape.compatibility());
                    data.put("imei_compatibility_message", scrape.message());
                    logger.debug("Extracted IMEI compatibility: compatible={}", scrape.compatibility());
                }
                break;

            default:
                logger.debug("No specific extraction logic for tool: {}", toolName);
                break;
        }

        return data;
    }

    /**
     * Format address into a readable string
     */
    private String formatAddress(AddressResponse address) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getStreet());
        if (address.getNumber() != null && !address.getNumber().isEmpty()) {
            sb.append(" #").append(address.getNumber());
        }
        if (address.getDistrict() != null && !address.getDistrict().isEmpty()) {
            sb.append(", ").append(address.getDistrict());
        }
        if (address.getPostalCode() != null && !address.getPostalCode().isEmpty()) {
            sb.append(", CP ").append(address.getPostalCode());
        }
        if (address.getReference() != null && !address.getReference().isEmpty()) {
            sb.append(" (").append(address.getReference()).append(")");
        }
        return sb.toString();
    }

    /**
     * Check if required data for order creation is available
     */
    public boolean hasRequiredOrderData(String conversationId) {
        Object customerId = stateService.getContextData(conversationId, "customer_id");
        Object addressId = stateService.getContextData(conversationId, "address_id");

        return customerId != null && addressId != null;
    }

    /**
     * Check if customer is registered
     */
    public boolean hasCustomerData(String conversationId) {
        Object customerId = stateService.getContextData(conversationId, "customer_id");
        return customerId != null;
    }

    /**
     * Check if address is registered
     */
    public boolean hasAddressData(String conversationId) {
        Object addressId = stateService.getContextData(conversationId, "address_id");
        return addressId != null;
    }

    /**
     * Check if order has been created
     */
    public boolean hasOrderData(String conversationId) {
        Object orderId = stateService.getContextData(conversationId, "order_id");
        return orderId != null;
    }

    /**
     * Get customer ID from context
     */
    public Long getCustomerId(String conversationId) {
        Object customerId = stateService.getContextData(conversationId, "customer_id");
        if (customerId instanceof Long) {
            return (Long) customerId;
        } else if (customerId instanceof Integer) {
            return ((Integer) customerId).longValue();
        }
        return null;
    }

    /**
     * Get address ID from context
     */
    public Long getAddressId(String conversationId) {
        Object addressId = stateService.getContextData(conversationId, "address_id");
        if (addressId instanceof Long) {
            return (Long) addressId;
        } else if (addressId instanceof Integer) {
            return ((Integer) addressId).longValue();
        }
        return null;
    }

    /**
     * Get decrypted portability NIP from context SECURITY: Automatically
     * decrypts the stored encrypted NIP
     */
    public String getDecryptedPortabilityNip(String conversationId) {
        Object encryptedNip = stateService.getContextData(conversationId, "portability_nip");
        if (encryptedNip instanceof String) {
            try {
                return encryptor.decrypt((String) encryptedNip);
            } catch (Exception e) {
                logger.error("Failed to decrypt portability NIP for conversation: {}", conversationId, e);
                return null;
            }
        }
        return null;
    }

    /**
     * Get decrypted portability IMEI from context SECURITY: Automatically
     * decrypts the stored encrypted IMEI
     */
    public String getDecryptedPortabilityImei(String conversationId) {
        Object encryptedImei = stateService.getContextData(conversationId, "portability_imei");
        if (encryptedImei instanceof String) {
            try {
                return encryptor.decrypt((String) encryptedImei);
            } catch (Exception e) {
                logger.error("Failed to decrypt portability IMEI for conversation: {}", conversationId, e);
                return null;
            }
        }
        return null;
    }

    /**
     * Get decrypted checkout session URL from context SECURITY: Automatically
     * decrypts the stored encrypted checkout URL
     */
    public String getDecryptedCheckoutUrl(String conversationId) {
        Object encryptedUrl = stateService.getContextData(conversationId, "checkout_session_url");
        if (encryptedUrl instanceof String) {
            try {
                return encryptor.decrypt((String) encryptedUrl);
            } catch (Exception e) {
                logger.error("Failed to decrypt checkout URL for conversation: {}", conversationId, e);
                return null;
            }
        }
        return null;
    }
}
