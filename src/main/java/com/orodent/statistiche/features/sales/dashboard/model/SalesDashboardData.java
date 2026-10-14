package com.orodent.statistiche.features.sales.dashboard.model;

import java.util.List;

public record SalesDashboardData(
        List<Integer> availableYears,
        int selectedYear,
        SalesFilter filter,
        SalesSummary summary,
        SalesSummary previousSummary,
        List<AnnualSalesComparison> annualComparisons,
        List<MonthlySales> monthlySales,
        List<MonthlySales> previousMonthlySales,
        List<SalesRankingItem> topCustomers,
        List<SalesRankingItem> topProducts,
        List<TopCustomerHistory> topCustomerHistory,
        DashboardProjections projections
) {
    public SalesDashboardData {
        availableYears = List.copyOf(availableYears);
        annualComparisons = List.copyOf(annualComparisons);
        monthlySales = List.copyOf(monthlySales);
        previousMonthlySales = List.copyOf(previousMonthlySales);
        topCustomers = List.copyOf(topCustomers);
        topProducts = List.copyOf(topProducts);
        topCustomerHistory = List.copyOf(topCustomerHistory);
    }

    public SalesDashboardData(List<Integer> availableYears, int selectedYear, SalesFilter filter,
                              SalesSummary summary, SalesSummary previousSummary,
                              List<AnnualSalesComparison> annualComparisons, List<MonthlySales> monthlySales,
                              List<MonthlySales> previousMonthlySales, List<SalesRankingItem> topCustomers,
                              List<SalesRankingItem> topProducts, List<TopCustomerHistory> topCustomerHistory) {
        this(availableYears, selectedYear, filter, summary, previousSummary, annualComparisons,
                monthlySales, previousMonthlySales, topCustomers, topProducts, topCustomerHistory, DashboardProjections.empty());
    }

    public boolean empty() {
        return summary.documents() == 0;
    }
}
