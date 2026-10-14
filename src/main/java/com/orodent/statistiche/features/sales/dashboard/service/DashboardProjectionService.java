package com.orodent.statistiche.features.sales.dashboard.service;

import com.orodent.statistiche.features.sales.dashboard.model.*;
import com.orodent.statistiche.features.sales.dashboard.repository.SalesStatisticsRepository;
import com.orodent.statistiche.features.sales.projection.model.*;
import com.orodent.statistiche.features.sales.projection.service.SeasonalProjectionCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

/** Coordinates per-measure forecasts only for the current whole-year KPI comparisons. */
final class DashboardProjectionService {
    DashboardProjections load(SalesStatisticsRepository repository, int year, SalesFilter filter,
                              SalesSummary actual, List<AnnualSalesComparison> annual, List<Integer> years) {
        if (year != LocalDate.now().getYear() || !filter.equals(SalesFilter.wholeYear(year))) return DashboardProjections.empty();
        RevenueProjection revenue = annual.stream().filter(value -> value.year() == year)
                .map(AnnualSalesComparison::projection).filter(Objects::nonNull).findFirst().orElse(null);
        var coverage = revenue == null ? repository.loadDataCoverage(year)
                : Optional.of(new SalesDataCoverage(revenue.dataFrom(), revenue.dataThrough()));
        if (coverage.isEmpty() || coverage.get().through().isAfter(LocalDate.now())
                || coverage.get().through().equals(LocalDate.of(year, 12, 31))) return DashboardProjections.empty();
        int fromYear = years.stream().min(Integer::compareTo).orElse(year);
        MetricProjection quantity = actual.quantity().signum() <= 0 ? null
                : new SeasonalProjectionCalculator(3).calculate(year, actual.quantity(), coverage.get(),
                        repository.loadDailyQuantityHistory(fromYear, year), true);
        MetricProjection documents = actual.documents() <= 0 ? null
                : new SeasonalProjectionCalculator(0).calculate(year, BigDecimal.valueOf(actual.documents()), coverage.get(),
                        repository.loadDailyDocumentHistory(fromYear, year), true);
        return new DashboardProjections(MetricProjection.fromRevenue(revenue), quantity, documents);
    }
}
