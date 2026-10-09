package com.orodent.statistiche.features.sales.customers.view;

import com.orodent.statistiche.features.sales.customers.model.*;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    private final Label revenueDirection = new Label();
    private final Label quantityDirection = new Label();
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
        chart.setLegendVisible(false);
        chart.setPrefHeight(270);
        chart.getStyleClass().add("customer-trend-chart");
        VBox.setVgrow(chart, Priority.ALWAYS);
        HBox legend = new HBox(18, legendItem("Ordini mensili", "actual-orders"),
                legendItem("Media mobile 3 mesi", "moving-average"));
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getStyleClass().add("customer-trend-legend");
        FlowPane heading = new FlowPane(10, 6, status, revenueDirection, quantityDirection);
        heading.setAlignment(Pos.CENTER_LEFT);
        revenueDirection.getStyleClass().add("customer-trend-direction");
        quantityDirection.getStyleClass().add("customer-trend-direction");
        getChildren().addAll(heading, explanation, indicators, legend, chart);
    }

    void show(CustomerTrendData data) {
        CustomerTrendSummary summary = data.summary();
        status.setText(summary.status().label());
        status.getStyleClass().removeIf(style -> style.startsWith("trend-status-"));
        status.getStyleClass().add("trend-status-" + summary.status().name().toLowerCase(Locale.ROOT).replace('_', '-'));
        updateDirection(revenueDirection, "fatturato", summary.revenueChangePercentage());
        updateDirection(quantityDirection, "quantità", summary.quantityChangePercentage());
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

    private void updateDirection(Label label, String metric, BigDecimal percentage) {
        label.getStyleClass().removeAll("metric-growing", "metric-stable", "metric-declining");
        CustomerTrendDirection direction = CustomerTrendDirection.forValue(percentage);
        if (direction == CustomerTrendDirection.UNAVAILABLE) {
            label.setText("→ " + metric + " non confrontabile");
            label.getStyleClass().add("metric-stable");
            return;
        }
        String arrow = switch (direction) {
            case UP -> "↗ ";
            case DOWN -> "↘ ";
            default -> "→ ";
        };
        String style = switch (direction) {
            case UP -> "metric-growing";
            case DOWN -> "metric-declining";
            default -> "metric-stable";
        };
        String sign = percentage.signum() > 0 ? "+" : "";
        label.setText(arrow + sign + percentage.stripTrailingZeros().toPlainString() + "% " + metric);
        label.getStyleClass().add(style);
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

    private HBox legendItem(String text, String seriesStyle) {
        Region line = new Region();
        line.getStyleClass().addAll("customer-trend-legend-line", seriesStyle);
        Label label = new Label(text);
        HBox item = new HBox(7, line, label);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("customer-trend-legend-item");
        return item;
    }

    private String comparison(BigDecimal percentage) {
        if (percentage == null) return " · confronto non disponibile";
        String sign = percentage.signum() > 0 ? "+" : "";
        return " · " + sign + percentage.stripTrailingZeros().toPlainString() + "%";
    }
}
