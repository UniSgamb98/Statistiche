package com.orodent.statistiche.features.sales.products.model;

public enum ProductVariantDimension {
    COLOR("Colore"), HEIGHT("Altezza"), DIAMETER("Diametro");
    private final String label;
    ProductVariantDimension(String label) { this.label = label; }
    public String value(ProductAttributes attributes) {
        return switch (this) {
            case COLOR -> attributes.color().isEmpty() ? "Non previsto" : attributes.color();
            case HEIGHT -> Integer.toString(attributes.height());
            case DIAMETER -> Integer.toString(attributes.diameter());
        };
    }
    @Override public String toString() { return label; }
}
