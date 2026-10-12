package com.orodent.statistiche.features.sales.customers.model;

public record CustomerTrendSummary(CustomerTrendStatus status,
                                   CustomerTrendComparison rolling,
                                   CustomerTrendComparison annual,
                                   String explanation) { }
