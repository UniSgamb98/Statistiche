package com.orodent.statistiche.features.sales.customers.view;

import com.orodent.statistiche.features.sales.customers.model.*;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

final class CustomerTrendPane extends VBox {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final Label status = new Label();
    private final Label explanation = new Label();
    private final Label orders = new Label();
    private final Label revenue = new Label();
    private final LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), new NumberAxis());

    CustomerTrendPane() {
        setSpacing(12);
        getStyleClass().add("customer-trend-pane");
        status.getStyleClass().add("customer-trend-status");
        explanation.getStyleClass().add("muted-label");
        explanation.setWrapText(true);
        HBox indicators = new HBox(10, indicator("Ordini ultimi 12 mesi", orders),
                indicator("Fatturato ultimi 12 mesi", revenue));
        indicators.setAlignment(Pos.CENTER_LEFT);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);
        chart.setPrefHeight(270);
        chart.getStyleClass().add("customer-trend-chart");
        VBox.setVgrow(chart, Priority.ALWAYS);
        getChildren().addAll(status, explanation, indicators, chart);
    }

    void show(CustomerTrendData data) {
        CustomerTrendSummary summary = data.summary();
        status.setText(summary.status().label());
        status.getStyleClass().removeIf(style -> style.startsWith("trend-status-"));
        status.getStyleClass().add("trend-status-" + summary.status().name().toLowerCase(Locale.ROOT).replace('_', '-'));
        explanation.setText(summary.explanation());
        orders.setText(summary.recentOrders() + comparison(summary.orderChangePercentage()));
        revenue.setText(currency.format(summary.recentRevenue()) + comparison(summary.revenueChangePercentage()));

        XYChart.Series<String, Number> actual = new XYChart.Series<>();
        actual.setName("Ordini mensili");
        XYChart.Series<String, Number> average = new XYChart.Series<>();
        average.setName("Media mobile 3 mesi");
        for (CustomerTrendPoint point : data.points()) {
            String month = Month.of(point.month()).getDisplayName(TextStyle.SHORT, Locale.ITALY)
                    + " " + Integer.toString(point.year()).substring(2);
            actual.getData().add(new XYChart.Data<>(month, point.orders()));
            average.getData().add(new XYChart.Data<>(month, point.movingAverage()));
        }
        chart.getData().setAll(actual, average);
    }

    private VBox indicator(String title, Label value) {
        Label heading = new Label(title);
        heading.getStyleClass().add("muted-label");
        value.getStyleClass().add("customer-trend-value");
        VBox card = new VBox(5, heading, value);
        card.getStyleClass().add("customer-trend-indicator");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private String comparison(BigDecimal percentage) {
        if (percentage == null) return " · confronto non disponibile";
        String sign = percentage.signum() > 0 ? "+" : "";
        return " · " + sign + percentage.stripTrailingZeros().toPlainString() + "%";
    }
}
