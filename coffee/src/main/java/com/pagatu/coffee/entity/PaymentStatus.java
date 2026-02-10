package com.pagatu.coffee.entity;

/**
 * Payment status for group memberships.
 * Values are stored in Italian in the database for backward compatibility.
 */
public enum PaymentStatus {
    PAGATO("PAGATO"), // PAID
    NON_PAGATO("NON_PAGATO"), // NOT_PAID
    SALTATO("SALTATO"); // SKIPPED

    private final String dbValue;

    PaymentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
