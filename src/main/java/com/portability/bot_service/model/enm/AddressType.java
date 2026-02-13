package com.portability.bot_service.model.enm;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AddressType {
    CEDIS("cedis"),
    CLIENT("client");
    private final String value;

    AddressType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
