package com.orodent.statistiche.features.sales.products.model;

import java.util.List;
import java.util.Map;
public record ProductDetailData(ProductGroupSummary group, ProductSalesSnapshot snapshot,
                                List<ProductMonthlyValue> monthly, List<ProductCustomerValue> customers,
                                Map<ProductVariantDimension, List<ProductVariantValue>> variants) {
    public ProductDetailData { monthly = List.copyOf(monthly); customers = List.copyOf(customers); variants = Map.copyOf(variants); }
}
