package com.portability.bot_service.model.enm;

public enum ConversationState {

    INITIAL,                     // Customer begins interaction
    CUSTOMER_REGISTRATION,      // Customer data
    
    INTENT_SELECTION,           // Choose: buy / portability / tracking
    PRODUCT_SELECTED,           // SIM or SIM + portability

    IMEI_REQUIRED,              // IMEI missing
    IMEI_VALIDATED,             // IMEI compatible

    ADDRESS_REQUIRED,           // Address missing
    PAYMENT_PENDING,            // Link sent, payment not confirmed
    PAYMENT_CONFIRMED,          // Payment confirmed

    SIM_SHIPPED,                // SIM shipped

    PORTABILITY_WAIT_SIM,       // Waiting for SIM to arrive
    PORTABILITY_NIP_REQUIRED,   // NIP missing
    PORTABILITY_SIM_ACTIVATION, // Activate SIM
    PORTABILITY_IN_PROGRESS,    // Scraper running
    PORTABILITY_COMPLETED,      // Portability successful

    COMPLETED,                  // Flow completed
    BLOCKED,                     // Invalid flow or error

    ABANDONED,                   // Conversation abandoned
    ERROR_STATE                     // Error encountered
}
