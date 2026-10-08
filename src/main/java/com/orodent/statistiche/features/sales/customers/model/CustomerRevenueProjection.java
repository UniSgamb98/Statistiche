package com.orodent.statistiche.features.sales.customers.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerRevenueProjection(
        int year,
        BigDecimal actualRevenue,
        BigDecimal projectedRemainingRevenue,
        BigDecimal projectedAnnualRevenue,
        LocalDate dataFrom,
        LocalDate dataThrough,
        ProjectionMethod method,
        ProjectionConfidence confidence,
        int historicalYears
) { }
