package com.orodent.statistiche.features.sales.customers.service;

import com.orodent.statistiche.features.sales.customers.model.CustomerMonthlyValue;
import com.orodent.statistiche.features.sales.customers.model.CustomerTrendData;
import com.orodent.statistiche.features.sales.customers.model.CustomerTrendStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerTrendCalculatorTest {
    private final CustomerTrendCalculator calculator = new CustomerTrendCalculator();

    @Test
    void identifiesAProgressiveOrderDeclineAcrossEquivalentPeriods() {
        List<CustomerMonthlyValue> months = new ArrayList<>();
        for (int month = 1; month <= 24; month++) {
            int year = month <= 12 ? 2025 : 2026;
            int monthOfYear = month <= 12 ? month : month - 12;
            int orders = month <= 12 ? 10 : 7;
            months.add(value(year, monthOfYear, orders));
        }

        CustomerTrendData result = calculator.calculate(months);

        assertEquals(CustomerTrendStatus.DECLINING, result.summary().status());
        assertEquals(120, result.summary().previousOrders());
        assertEquals(84, result.summary().recentOrders());
        assertEquals(new BigDecimal("-30.0"), result.summary().orderChangePercentage());
        assertEquals(new BigDecimal("7.0"), result.points().getLast().movingAverage());
    }

    @Test
    void reportsInsufficientDataWithoutACompletePreviousWindow() {
        CustomerTrendData result = calculator.calculate(List.of(value(2026, 1, 4), value(2026, 2, 5)));

        assertEquals(CustomerTrendStatus.INSUFFICIENT_DATA, result.summary().status());
    }

    private CustomerMonthlyValue value(int year, int month, int orders) {
        return new CustomerMonthlyValue(year, month, BigDecimal.valueOf(orders * 100L),
                BigDecimal.valueOf(orders), orders);
    }
}
