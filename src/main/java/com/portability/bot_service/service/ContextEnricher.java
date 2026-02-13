package com.portability.bot_service.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for enriching the system prompt with available context data.
 * Generates a human-readable summary of stored information that the AI agent can use
 * to make informed decisions without asking for information that's already available.
 */
@Service
public class ContextEnricher {

    private static final Logger logger = LoggerFactory.getLogger(ContextEnricher.class);

    private final ConversationStateService stateService;

    public ContextEnricher(ConversationStateService stateService) {
        this.stateService = stateService;
    }

    /**
     * Generate a formatted summary of available context data for inclusion in system prompt
     */
    public String generateContextSummary(String conversationId) {
        Map<String, Object> contextData = stateService.getAllContextData(conversationId);

        if (contextData == null || contextData.isEmpty()) {
            return "No hay información previa disponible. Deberás recopilar todos los datos necesarios del usuario.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("=== INFORMACIÓN DISPONIBLE EN EL CONTEXTO ===\n\n");
        summary.append("IMPORTANTE: Usa estos datos cuando estén disponibles. NO vuelvas a preguntar información que ya tienes.\n\n");

        // Customer information
        if (hasCustomerData(contextData)) {
            summary.append("DATOS DEL CLIENTE:\n");
            appendIfPresent(summary, "  - ID del cliente", contextData.get("customer_id"));
            appendIfPresent(summary, "  - Nombre completo", contextData.get("customer_name"));
            appendIfPresent(summary, "  - Nombre", contextData.get("customer_first_name"));
            appendIfPresent(summary, "  - Apellido", contextData.get("customer_last_name"));
            appendIfPresent(summary, "  - Email", contextData.get("customer_email"));
            appendIfPresent(summary, "  - Teléfono", contextData.get("customer_phone"));
            summary.append("Cliente REGISTRADO - Utiliza customer_id: ").append(contextData.get("customer_id")).append(" en las tools\n\n");
        }

        // Address information
        if (hasAddressData(contextData)) {
            summary.append("DATOS DE DIRECCIÓN:\n");
            appendIfPresent(summary, "  - ID de dirección", contextData.get("address_id"));
            appendIfPresent(summary, "  - Calle", contextData.get("address_street"));
            appendIfPresent(summary, "  - Número", contextData.get("address_number"));
            appendIfPresent(summary, "  - Distrito", contextData.get("address_district"));
            appendIfPresent(summary, "  - Código Postal", contextData.get("address_postal_code"));
            appendIfPresent(summary, "  - Referencia", contextData.get("address_reference"));
            appendIfPresent(summary, "  - Dirección completa", contextData.get("address_full"));
            summary.append("Dirección REGISTRADA - Utiliza address_id: ").append(contextData.get("address_id")).append(" en las tools\n\n");
        }

        // Order information
        if (hasOrderData(contextData)) {
            summary.append("DATOS DE ORDEN:\n");
            appendIfPresent(summary, "  - ID de orden", contextData.get("order_id"));
            appendIfPresent(summary, "  - ID de producto", contextData.get("order_product_id"));
            appendIfPresent(summary, "  - Estado", contextData.get("order_status"));
            summary.append("Orden CREADA - Utiliza order_id: ").append(contextData.get("order_id")).append(" para crear el checkout\n\n");
        }

        // Portability information
        if (hasPortabilityData(contextData)) {
            summary.append("DATOS DE PORTABILIDAD:\n");
            appendIfPresent(summary, "  - ID de portabilidad", contextData.get("portability_id"));
            appendIfPresent(summary, "  - Teléfono a portar", contextData.get("portability_phone"));
            appendIfPresent(summary, "  - Estado", contextData.get("portability_status"));
            appendIfPresent(summary, "  - IMEI", contextData.get("portability_imei"));
            appendIfPresent(summary, "  - NIP", contextData.get("portability_nip"));
            
            boolean hasImei = contextData.get("portability_imei") != null;
            boolean hasNip = contextData.get("portability_nip") != null;
            
            if (!hasImei || !hasNip) {
                summary.append("FALTA INFORMACIÓN:\n");
                if (!hasImei) summary.append("    - Solicita el IMEI del dispositivo\n");
                if (!hasNip) summary.append("    - Solicita el NIP de portabilidad\n");
            } else {
                summary.append("Portabilidad COMPLETA con IMEI y NIP\n");
            }
            summary.append("\n");
        }

        // Checkout/Payment information
        if (hasCheckoutData(contextData)) {
            summary.append("DATOS DE PAGO:\n");
            appendIfPresent(summary, "  - ID de sesión de checkout", contextData.get("checkout_session_id"));
            appendIfPresent(summary, "  - URL de pago", contextData.get("checkout_session_url"));
            
            Boolean paymentCompleted = (Boolean) contextData.get("payment_completed");
            if (Boolean.TRUE.equals(paymentCompleted)) {
                summary.append("Pago COMPLETADO\n");
            } else {
                summary.append("Pago PENDIENTE - Proporciona la URL al usuario\n");
            }
            summary.append("\n");
        }

        // IMEI compatibility
        if (hasImeiCompatibilityData(contextData)) {
            summary.append("VERIFICACIÓN DE IMEI:\n");
            appendIfPresent(summary, "  - IMEI verificado", contextData.get("imei_checked"));
            Boolean compatible = (Boolean) contextData.get("imei_compatible");
            if (Boolean.TRUE.equals(compatible)) {
                summary.append("IMEI COMPATIBLE con la red\n");
            } else {
                summary.append("IMEI NO COMPATIBLE con la red\n");
            }
            summary.append("\n");
        }

        // Error tracking
        if (hasErrorData(contextData)) {
            summary.append("INFORMACIÓN DE ERRORES:\n");
            appendIfPresent(summary, "  - Último error", contextData.get("last_error"));
            appendIfPresent(summary, "  - Herramienta fallida", contextData.get("failed_tool"));
            appendIfPresent(summary, "  - Contador de errores", contextData.get("error_count"));
            summary.append("  El usuario puede estar reintentando una operación fallida\n\n");
        }

        summary.append("=== FIN DE INFORMACIÓN DISPONIBLE ===\n");
        
        logger.debug("Generated context summary for conversation {}: {} characters", 
                    conversationId, summary.length());
        
        return summary.toString();
    }

    /**
     * Helper method to append context data if present
     */
    private void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value != null) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }

    // Helper methods to check data availability
    private boolean hasCustomerData(Map<String, Object> context) {
        return context.containsKey("customer_id") && context.get("customer_id") != null;
    }

    private boolean hasAddressData(Map<String, Object> context) {
        return context.containsKey("address_id") && context.get("address_id") != null;
    }

    private boolean hasOrderData(Map<String, Object> context) {
        return context.containsKey("order_id") && context.get("order_id") != null;
    }

    private boolean hasPortabilityData(Map<String, Object> context) {
        return context.containsKey("portability_id") && context.get("portability_id") != null;
    }

    private boolean hasCheckoutData(Map<String, Object> context) {
        return context.containsKey("checkout_session_id") && context.get("checkout_session_id") != null;
    }

    private boolean hasImeiCompatibilityData(Map<String, Object> context) {
        return context.containsKey("imei_compatible");
    }

    private boolean hasErrorData(Map<String, Object> context) {
        return context.containsKey("last_error") && context.get("last_error") != null;
    }
}
