package com.orodent.statistiche.features.sales.customers.model;

public enum ProjectionConfidence {
    HIGH("Alta"), MEDIUM("Media"), LOW("Bassa"), ACTUAL("Consuntivo");
    private final String label;
    ProjectionConfidence(String label) { this.label = label; }
    public String label() { return label; }
}
