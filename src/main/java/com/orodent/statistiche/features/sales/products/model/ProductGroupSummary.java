package com.orodent.statistiche.features.sales.products.model;

import java.math.BigDecimal;
public record ProductGroupSummary(ProductGroupKey key, ProductTotals current, ProductTotals previous,
                                  BigDecimal revenueChange, BigDecimal quantityChange) { }
