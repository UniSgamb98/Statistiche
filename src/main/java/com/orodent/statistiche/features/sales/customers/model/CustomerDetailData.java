package com.orodent.statistiche.features.sales.customers.model;

import com.orodent.statistiche.features.sales.projection.model.RevenueProjection;
import java.util.List;
public record CustomerDetailData(CustomerDetail detail, List<CustomerYearSummary> yearly,
                                 List<CustomerMonthlyValue> monthly, List<CustomerProductItem> products,
                                 CustomerTrendData trend, RevenueProjection projection) {
    public CustomerDetailData { yearly=List.copyOf(yearly); monthly=List.copyOf(monthly); products=List.copyOf(products); }
}
