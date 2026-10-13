package com.orodent.statistiche.features.sales.products.model;

public record ProductGroupKey(String material, ProductFamily family, String color) {
    public static ProductGroupKey of(ProductAttributes attributes, ProductGrouping grouping) {
        return new ProductGroupKey(attributes.material(), attributes.family(),
                grouping == ProductGrouping.FAMILY_COLOR ? attributes.color() : "");
    }
    public boolean matches(ProductAttributes attributes) {
        return material.equals(attributes.material()) && family == attributes.family()
                && (color.isEmpty() || color.equals(attributes.color()));
    }
    public String label() { return material + " · " + family + (color.isEmpty() ? "" : " · " + color); }
}
