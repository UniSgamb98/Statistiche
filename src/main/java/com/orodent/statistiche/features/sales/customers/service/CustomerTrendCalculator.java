package com.orodent.statistiche.features.sales.customers.service;

import com.orodent.statistiche.features.sales.customers.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class CustomerTrendCalculator {
    private static final int WINDOW_MONTHS = 12;
    private static final int CHART_MONTHS = 24;

    public CustomerTrendData calculate(List<CustomerMonthlyValue> chronologicalValues) {
        List<CustomerMonthlyValue> values = chronologicalValues.size() <= CHART_MONTHS
                ? chronologicalValues
                : chronologicalValues.subList(chronologicalValues.size() - CHART_MONTHS, chronologicalValues.size());
        List<CustomerMonthlyValue> recent = tail(values, WINDOW_MONTHS);
        List<CustomerMonthlyValue> previous = values.subList(
                Math.max(0, values.size() - WINDOW_MONTHS * 2), Math.max(0, values.size() - WINDOW_MONTHS));

        int recentOrders = recent.stream().mapToInt(CustomerMonthlyValue::documents).sum();
        int previousOrders = previous.stream().mapToInt(CustomerMonthlyValue::documents).sum();
        BigDecimal recentRevenue = sumRevenue(recent);
        BigDecimal previousRevenue = sumRevenue(previous);
        BigDecimal orderChange = percentage(recentOrders, previousOrders);
        BigDecimal revenueChange = percentage(recentRevenue, previousRevenue);
        CustomerTrendStatus status = status(orderChange, previous.size());
        CustomerTrendSummary summary = new CustomerTrendSummary(status, recentOrders, previousOrders,
                orderChange, recentRevenue, revenueChange, explanation(status, orderChange));
        return new CustomerTrendData(summary, points(values));
    }

    private List<CustomerTrendPoint> points(List<CustomerMonthlyValue> values) {
        List<CustomerTrendPoint> points = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            int from = Math.max(0, index - 2);
            BigDecimal total = BigDecimal.ZERO;
            for (int cursor = from; cursor <= index; cursor++) {
                total = total.add(BigDecimal.valueOf(values.get(cursor).documents()));
            }
            CustomerMonthlyValue value = values.get(index);
            points.add(new CustomerTrendPoint(value.year(), value.month(), value.documents(),
                    total.divide(BigDecimal.valueOf(index - from + 1L), 1, RoundingMode.HALF_UP)));
        }
        return List.copyOf(points);
    }

    private List<CustomerMonthlyValue> tail(List<CustomerMonthlyValue> values, int size) {
        return values.subList(Math.max(0, values.size() - size), values.size());
    }

    private BigDecimal sumRevenue(List<CustomerMonthlyValue> values) {
        return values.stream().map(CustomerMonthlyValue::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentage(int current, int previous) {
        return percentage(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private BigDecimal percentage(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) return null;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private CustomerTrendStatus status(BigDecimal change, int previousMonths) {
        if (change == null || previousMonths < WINDOW_MONTHS) return CustomerTrendStatus.INSUFFICIENT_DATA;
        if (change.compareTo(BigDecimal.TEN) >= 0) return CustomerTrendStatus.GROWING;
        if (change.compareTo(BigDecimal.valueOf(-20)) <= 0) return CustomerTrendStatus.DECLINING;
        if (change.compareTo(BigDecimal.valueOf(-10)) <= 0) return CustomerTrendStatus.SLOWING;
        return CustomerTrendStatus.STABLE;
    }

    private String explanation(CustomerTrendStatus status, BigDecimal change) {
        if (change == null) return "Non c'è ancora uno storico sufficiente per confrontare due periodi equivalenti.";
        String variation = change.abs().stripTrailingZeros().toPlainString() + "%";
        return switch (status) {
            case GROWING -> "Gli ordini degli ultimi 12 mesi sono aumentati del " + variation + ".";
            case SLOWING, DECLINING -> "Gli ordini degli ultimi 12 mesi sono diminuiti del " + variation + ".";
            case STABLE -> "Gli ordini degli ultimi 12 mesi sono sostanzialmente stabili.";
            case INSUFFICIENT_DATA -> "Non c'è ancora uno storico sufficiente per confrontare due periodi equivalenti.";
        };
    }
}
