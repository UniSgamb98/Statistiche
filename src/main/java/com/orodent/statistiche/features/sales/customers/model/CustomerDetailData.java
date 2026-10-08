package com.orodent.statistiche.features.sales.customers.model;
import java.util.List;
public record CustomerDetailData(CustomerDetail detail, List<CustomerYearSummary> yearly,
                                 List<CustomerMonthlyValue> monthly, List<CustomerProductItem> products,
                                 CustomerTrendData trend, CustomerRevenueProjection projection) {
    public CustomerDetailData { yearly=List.copyOf(yearly); monthly=List.copyOf(monthly); products=List.copyOf(products); }
}
