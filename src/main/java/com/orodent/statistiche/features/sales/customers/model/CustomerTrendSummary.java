package com.orodent.statistiche.features.sales.customers.model;

import java.math.BigDecimal;

public record CustomerTrendSummary(
        CustomerTrendStatus status,
        int recentOrders,
        int previousOrders,
        BigDecimal orderChangePercentage,
        BigDecimal recentRevenue,
        BigDecimal revenueChangePercentage,
        BigDecimal recentQuantity,
        BigDecimal quantityChangePercentage,
        String explanation
) { }
