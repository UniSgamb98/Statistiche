package com.orodent.statistiche.features.sales.customers.repository;

import com.orodent.statistiche.features.sales.customers.model.*;
import java.time.LocalDate;
import java.util.List;

public interface CustomerAnalysisRepository {
    CustomerDetail loadBaseDetail(String customerCode, int year);
    List<LocalDate> loadPurchaseDates(String customerCode, int year);
    List<CustomerYearSummary> loadYearlyHistory(String customerCode);
    List<CustomerMonthlyValue> loadMonthlyHistory(String customerCode, int fromYear, int toYear);
    List<CustomerProductItem> loadProducts(String customerCode, int year);
    List<TopCustomerHistory> loadTopCustomerHistory(int selectedYear, int limit);
}
