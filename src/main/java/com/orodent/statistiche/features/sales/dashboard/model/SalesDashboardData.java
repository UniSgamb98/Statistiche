package com.orodent.statistiche.features.sales.dashboard.model;

import java.util.List;

public record SalesDashboardData(
        List<Integer> availableYears,
        int selectedYear,
        SalesFilter filter,
        SalesSummary summary,
        List<MonthlySales> monthlySales,
        List<SalesRankingItem> topCustomers,
        List<SalesRankingItem> topProducts
) {
    public SalesDashboardData {
        availableYears = List.copyOf(availableYears);
        monthlySales = List.copyOf(monthlySales);
        topCustomers = List.copyOf(topCustomers);
        topProducts = List.copyOf(topProducts);
    }

    public boolean empty() {
        return summary.documents() == 0;
    }
}
