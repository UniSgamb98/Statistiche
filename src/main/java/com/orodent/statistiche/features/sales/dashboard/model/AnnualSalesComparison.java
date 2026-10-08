package com.orodent.statistiche.features.sales.dashboard.model;

import java.math.BigDecimal;

public record AnnualSalesComparison(
        int year,
        SalesSummary summary,
        BigDecimal revenueChangePercentage,
        BigDecimal quantityChangePercentage
) {
}
