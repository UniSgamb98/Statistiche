package com.orodent.statistiche.features.sales.products.model;

public enum ProductGrouping {
    FAMILY("Famiglia"), FAMILY_COLOR("Famiglia e colore");
    private final String label;
    ProductGrouping(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
