package com.orodent.statistiche.features.sales.projection.service;

import com.orodent.statistiche.features.sales.projection.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RevenueProjectionCalculator {
    public RevenueProjection calculate(
            int year, BigDecimal actualRevenue, SalesDataCoverage coverage, List<DailyRevenueValue> history,
            boolean yearInProgress
    ) {
        if (!yearInProgress || coverage.through().getMonthValue() == 12 && coverage.through().getDayOfMonth() == 31) {
            return projection(year, actualRevenue, actualRevenue, coverage, ProjectionMethod.ACTUAL,
                    ProjectionConfidence.ACTUAL, 0);
        }
        List<BigDecimal> shares = historicalShares(year, coverage.through(), history);
        if (shares.size() >= 2) {
            BigDecimal share = median(shares);
            BigDecimal total = share.signum() == 0 ? actualRevenue
                    : actualRevenue.divide(share, 2, RoundingMode.HALF_UP);
            ProjectionConfidence confidence = shares.size() >= 3
                    ? ProjectionConfidence.HIGH : ProjectionConfidence.MEDIUM;
            return projection(year, actualRevenue, total, coverage, ProjectionMethod.SEASONAL,
                    confidence, shares.size());
        }
        BigDecimal elapsed = BigDecimal.valueOf(coverage.through().getDayOfYear());
        BigDecimal days = BigDecimal.valueOf(coverage.through().lengthOfYear());
        BigDecimal total = actualRevenue.multiply(days).divide(elapsed, 2, RoundingMode.HALF_UP);
        return projection(year, actualRevenue, total, coverage, ProjectionMethod.LINEAR,
                ProjectionConfidence.LOW, shares.size());
    }

    private List<BigDecimal> historicalShares(int selectedYear, LocalDate cutoff,
                                               List<DailyRevenueValue> history) {
        List<BigDecimal> shares = new ArrayList<>();
        history.stream().map(value -> value.date().getYear()).distinct().sorted().forEach(year -> {
            if (year >= selectedYear) return;
            BigDecimal total = revenue(history, year, LocalDate.of(year, 12, 31));
            if (total.signum() <= 0) return;
            LocalDate equivalentCutoff = equivalentDate(year, cutoff);
            BigDecimal partial = revenue(history, year, equivalentCutoff);
            if (partial.signum() <= 0 || partial.compareTo(total) > 0) return;
            shares.add(partial.divide(total, 6, RoundingMode.HALF_UP));
        });
        return shares;
    }

    private BigDecimal revenue(List<DailyRevenueValue> history, int year, LocalDate through) {
        return history.stream().filter(value -> value.date().getYear() == year && !value.date().isAfter(through))
                .map(DailyRevenueValue::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate equivalentDate(int year, LocalDate cutoff) {
        try { return LocalDate.of(year, cutoff.getMonth(), cutoff.getDayOfMonth()); }
        catch (DateTimeException ignored) { return LocalDate.of(year, 2, 28); }
    }

    private BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle)
                : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.TWO, 6, RoundingMode.HALF_UP);
    }

    private RevenueProjection projection(int year, BigDecimal actual, BigDecimal estimated,
                                                   SalesDataCoverage coverage, ProjectionMethod method,
                                                   ProjectionConfidence confidence, int historicalYears) {
        BigDecimal total = estimated.max(actual).setScale(2, RoundingMode.HALF_UP);
        return new RevenueProjection(year, actual, total.subtract(actual), total,
                coverage.from(), coverage.through(), method, confidence, historicalYears);
    }
}
