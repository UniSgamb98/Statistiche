package com.orodent.statistiche.features.sales.dashboard.view;

import com.orodent.statistiche.features.sales.dashboard.model.AnnualSalesComparison;
import com.orodent.statistiche.features.sales.projection.model.ProjectionMethod;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Displays actual annual data and estimates in explicitly separate columns. */
final class AnnualSalesTable extends TableView<AnnualSalesComparison> {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);
    private Map<Integer, AnnualSalesComparison> comparisons = Map.of();

    AnnualSalesTable() {
        TableColumn<AnnualSalesComparison, Integer> year = new TableColumn<>("Anno");
        year.setCellValueFactory(item -> new SimpleIntegerProperty(item.getValue().year()).asObject());
        year.setSortType(TableColumn.SortType.DESCENDING);
        getColumns().addAll(year,
                column("Fatturato", item -> currency.format(item.summary().netRevenue())),
                column("Δ registrato", item -> percentage(item.revenueChangePercentage())),
                column("Previsto 31/12", item -> forecast(item) ? currency.format(item.projection().projectedAnnualRevenue()) : "—"),
                column("Δ previsto", this::projectedChange),
                column("Quantità", item -> number.format(item.summary().quantity())),
                column("Clienti", item -> number.format(item.summary().customers())),
                column("Media doc.", item -> currency.format(item.summary().averageDocumentValue())));
        getSortOrder().add(year);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setPrefHeight(310);
        setPlaceholder(new Label("Nessuno storico disponibile"));
    }
    void show(List<AnnualSalesComparison> values) {
        comparisons = values.stream().collect(Collectors.toUnmodifiableMap(AnnualSalesComparison::year, Function.identity()));
        getItems().setAll(values); sort();
    }
    private boolean forecast(AnnualSalesComparison item) {
        return item.projection() != null && item.projection().method() != ProjectionMethod.ACTUAL;
    }
    private String projectedChange(AnnualSalesComparison item) {
        var previous = comparisons.get(item.year() - 1);
        if (!forecast(item) || previous == null || previous.summary().netRevenue().signum() == 0) return "—";
        BigDecimal baseline = previous.summary().netRevenue();
        return percentage(item.projection().projectedAnnualRevenue().subtract(baseline)
                .multiply(BigDecimal.valueOf(100)).divide(baseline.abs(), 1, RoundingMode.HALF_UP));
    }
    private String percentage(BigDecimal value) {
        return value == null ? "—" : (value.signum() > 0 ? "+" : "") + number.format(value) + "%";
    }
    private TableColumn<AnnualSalesComparison, String> column(String name, Function<AnnualSalesComparison, String> display) {
        TableColumn<AnnualSalesComparison, String> column = new TableColumn<>(name);
        column.setCellValueFactory(item -> new SimpleStringProperty(display.apply(item.getValue())));
        return column;
    }
}
