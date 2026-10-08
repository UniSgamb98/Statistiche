package com.orodent.statistiche.features.sales.dashboard.repository;

import com.orodent.statistiche.features.sales.dashboard.model.MonthlySales;
import com.orodent.statistiche.features.sales.dashboard.model.SalesFilter;
import com.orodent.statistiche.features.sales.dashboard.model.SalesRankingItem;
import com.orodent.statistiche.features.sales.dashboard.model.SalesSummary;

import java.util.List;

public interface SalesStatisticsRepository {

    List<Integer> findAvailableYears();

    SalesSummary loadSummary(SalesFilter filter);

    List<MonthlySales> loadMonthlySales(SalesFilter filter);

    List<SalesRankingItem> loadTopCustomers(SalesFilter filter, int limit);

    List<SalesRankingItem> loadTopProducts(SalesFilter filter, int limit);
}
