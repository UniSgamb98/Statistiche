package com.orodent.statistiche.features.sales.dashboard.view;

import com.orodent.statistiche.core.components.AppHeader;
import com.orodent.statistiche.features.sales.dashboard.model.MonthlySales;
import com.orodent.statistiche.features.sales.dashboard.model.AnnualSalesComparison;
import com.orodent.statistiche.features.sales.dashboard.model.SalesDashboardData;
import com.orodent.statistiche.features.sales.dashboard.model.SalesRankingItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

public final class SalesDashboardView extends BorderPane {

    private final AppHeader header = new AppHeader(
            "Dashboard vendite",
            "Andamento, clienti e prodotti in un'unica vista"
    );
    private final ComboBox<Integer> yearBox = new ComboBox<>();
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final ComboBox<PeriodPreset> periodBox = new ComboBox<>();
    private final Button applyButton = new Button("Applica");
    private final Button retryButton = new Button("Riprova");
    private final Button importButton = new Button("Importa vendite");
    private final ProgressIndicator loading = new ProgressIndicator();
    private final Label errorLabel = new Label();
    private final VBox emptyState = new VBox(12);
    private final VBox dashboard = new VBox(22);
    private final Label revenueValue = metricValue();
    private final Label quantityValue = metricValue();
    private final Label customersValue = metricValue();
    private final Label documentsValue = metricValue();
    private final Label averageValue = metricValue();
    private final Label revenueChange = comparisonValue();
    private final Label quantityChange = comparisonValue();
    private final Label customersChange = comparisonValue();
    private final Label documentsChange = comparisonValue();
    private final BarChart<String, Number> annualChart = createAnnualChart();
    private final TableView<AnnualSalesComparison> annualTable = createAnnualTable();
    private final ComboBox<ChartMetric> chartMetricBox = new ComboBox<>();
    private final LineChart<String, Number> monthlyChart = createMonthlyChart();
    private final TableView<SalesRankingItem> customersTable = createRankingTable(false);
    private final TableView<SalesRankingItem> productsTable = createRankingTable(true);
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);
    private SalesDashboardData currentData;

    public SalesDashboardView() {
        getStyleClass().add("page");
        setTop(header);
        setCenter(buildContent());
        showLoading();
    }

    public AppHeader header() {
        return header;
    }

    public ComboBox<Integer> yearBox() {
        return yearBox;
    }

    public DatePicker fromPicker() {
        return fromPicker;
    }

    public DatePicker toPicker() {
        return toPicker;
    }

    public Button applyButton() {
        return applyButton;
    }

    public ComboBox<PeriodPreset> periodBox() {
        return periodBox;
    }

    public Button retryButton() {
        return retryButton;
    }

    public Button importButton() {
        return importButton;
    }

    public void showLoading() {
        setFiltersDisabled(true);
        loading.setVisible(true);
        loading.setManaged(true);
        dashboard.setVisible(false);
        dashboard.setManaged(false);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        retryButton.setVisible(false);
        retryButton.setManaged(false);
    }

    public void showData(SalesDashboardData data) {
        setFiltersDisabled(false);
        loading.setVisible(false);
        loading.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        retryButton.setVisible(false);
        retryButton.setManaged(false);

        yearBox.setItems(FXCollections.observableArrayList(data.availableYears()));
        yearBox.setValue(data.selectedYear());
        fromPicker.setValue(data.filter().from());
        toPicker.setValue(data.filter().to());
        currentData = data;

        if (data.empty()) {
            dashboard.setVisible(false);
            dashboard.setManaged(false);
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        revenueValue.setText(currency.format(data.summary().netRevenue()));
        quantityValue.setText(number.format(data.summary().quantity()));
        customersValue.setText(number.format(data.summary().customers()));
        documentsValue.setText(number.format(data.summary().documents()));
        averageValue.setText(currency.format(data.summary().averageDocumentValue()));
        updateComparison(revenueChange, data.summary().netRevenue(), data.previousSummary().netRevenue(), data.selectedYear());
        updateComparison(quantityChange, data.summary().quantity(), data.previousSummary().quantity(), data.selectedYear());
        updateComparison(customersChange,
                BigDecimal.valueOf(data.summary().customers()),
                BigDecimal.valueOf(data.previousSummary().customers()),
                data.selectedYear()
        );
        updateComparison(documentsChange,
                BigDecimal.valueOf(data.summary().documents()),
                BigDecimal.valueOf(data.previousSummary().documents()),
                data.selectedYear()
        );
        updateAnnualComparison(data);
        updateChart(data);
        customersTable.getItems().setAll(data.topCustomers());
        productsTable.getItems().setAll(data.topProducts());
        dashboard.setVisible(true);
        dashboard.setManaged(true);
    }

    public void showError(Throwable error) {
        setFiltersDisabled(false);
        loading.setVisible(false);
        loading.setManaged(false);
        dashboard.setVisible(false);
        dashboard.setManaged(false);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        errorLabel.setText(error == null || error.getMessage() == null
                ? "Impossibile caricare le statistiche."
                : error.getMessage());
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        retryButton.setVisible(true);
        retryButton.setManaged(true);
    }

    private StackPane buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(28, 34, 36, 34));
        HBox errorState = new HBox(12, errorLabel, retryButton);
        errorState.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(buildFilters(), buildEmptyState(), dashboard, errorState);
        buildDashboard();

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("dashboard-scroll");

        StackPane stack = new StackPane(scrollPane, loading);
        StackPane.setAlignment(loading, Pos.CENTER);
        return stack;
    }

    private HBox buildFilters() {
        yearBox.setPrefWidth(110);
        periodBox.setItems(FXCollections.observableArrayList(PeriodPreset.values()));
        periodBox.setValue(PeriodPreset.WHOLE_YEAR);
        periodBox.setPrefWidth(150);
        fromPicker.setPrefWidth(145);
        toPicker.setPrefWidth(145);
        applyButton.getStyleClass().add("primary-button");

        HBox filters = new HBox(12,
                filterField("Anno", yearBox),
                filterField("Periodo", periodBox),
                filterField("Dal", fromPicker),
                filterField("Al", toPicker),
                applyButton
        );
        filters.getStyleClass().add("dashboard-filters");
        filters.setAlignment(Pos.BOTTOM_LEFT);
        return filters;
    }

    private VBox filterField(String label, javafx.scene.Node control) {
        Label title = new Label(label);
        title.getStyleClass().add("filter-label");
        return new VBox(5, title, control);
    }

    private VBox buildEmptyState() {
        emptyState.getStyleClass().add("dashboard-empty");
        emptyState.setAlignment(Pos.CENTER);
        Label title = new Label("Nessuna vendita disponibile");
        title.getStyleClass().add("section-title");
        Label description = new Label(
                "Importa un file annuale per visualizzare fatturato, andamento, clienti e prodotti."
        );
        description.getStyleClass().add("muted-label");
        importButton.getStyleClass().add("primary-button");
        emptyState.getChildren().addAll(title, description, importButton);
        return emptyState;
    }

    private void buildDashboard() {
        HBox metrics = new HBox(14,
                metricCard("Fatturato netto", revenueValue, revenueChange, "Valore medio documento", averageValue),
                metricCard("Quantità", quantityValue, quantityChange),
                metricCard("Clienti", customersValue, customersChange),
                metricCard("Documenti", documentsValue, documentsChange)
        );
        metrics.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        chartMetricBox.setItems(FXCollections.observableArrayList(ChartMetric.values()));
        chartMetricBox.setValue(ChartMetric.REVENUE);
        chartMetricBox.setOnAction(event -> updateChart(currentData));
        HBox chartHeader = new HBox(12, new Label("Andamento mensile"), chartMetricBox);
        chartHeader.setAlignment(Pos.CENTER_LEFT);
        chartHeader.getChildren().getFirst().getStyleClass().add("card-title");
        VBox historicalCard = card("Confronto storico automatico", annualChart);
        VBox annualTableCard = card("Riepilogo di tutti gli anni", annualTable);
        GridPane history = new GridPane();
        history.setHgap(18);
        history.add(historicalCard, 0, 0);
        history.add(annualTableCard, 1, 0);
        history.getColumnConstraints().addAll(
                new javafx.scene.layout.ColumnConstraints(0, 520, Double.MAX_VALUE, Priority.ALWAYS, null, true),
                new javafx.scene.layout.ColumnConstraints(0, 620, Double.MAX_VALUE, Priority.ALWAYS, null, true)
        );

        VBox chartCard = card(chartHeader, monthlyChart);
        GridPane rankings = new GridPane();
        rankings.setHgap(18);
        rankings.add(card("Top clienti", customersTable), 0, 0);
        rankings.add(card("Top prodotti", productsTable), 1, 0);
        GridPane.setHgrow(rankings.getChildren().get(0), Priority.ALWAYS);
        GridPane.setHgrow(rankings.getChildren().get(1), Priority.ALWAYS);
        rankings.getColumnConstraints().addAll(
                new javafx.scene.layout.ColumnConstraints(0, 520, Double.MAX_VALUE, Priority.ALWAYS, null, true),
                new javafx.scene.layout.ColumnConstraints(0, 520, Double.MAX_VALUE, Priority.ALWAYS, null, true)
        );

        dashboard.getChildren().addAll(metrics, history, chartCard, rankings);
    }

    private VBox metricCard(String title, Label value) {
        VBox card = new VBox(7);
        card.getStyleClass().add("dashboard-metric");
        Label label = new Label(title);
        label.getStyleClass().add("muted-label");
        card.getChildren().addAll(label, value);
        return card;
    }

    private VBox metricCard(String title, Label value, Label comparison) {
        VBox card = metricCard(title, value);
        card.getChildren().add(comparison);
        return card;
    }

    private VBox metricCard(String title, Label value, Label comparison, String secondaryTitle, Label secondaryValue) {
        VBox card = metricCard(title, value, comparison);
        Label separator = new Label(secondaryTitle);
        separator.getStyleClass().add("metric-secondary-label");
        secondaryValue.getStyleClass().remove("dashboard-metric-value");
        secondaryValue.getStyleClass().add("metric-secondary-value");
        card.getChildren().addAll(separator, secondaryValue);
        return card;
    }

    private VBox card(String title, javafx.scene.Node content) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        return card(titleLabel, content);
    }

    private VBox card(javafx.scene.Node title, javafx.scene.Node content) {
        VBox card = new VBox(14);
        card.getStyleClass().add("dashboard-card");
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(content, Priority.ALWAYS);
        card.getChildren().addAll(title, content);
        return card;
    }

    private LineChart<String, Number> createMonthlyChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(300);
        return chart;
    }

    private BarChart<String, Number> createAnnualChart() {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(310);
        return chart;
    }

    private TableView<AnnualSalesComparison> createAnnualTable() {
        TableView<AnnualSalesComparison> table = new TableView<>();
        TableColumn<AnnualSalesComparison, String> year = textColumn("Anno", item -> Integer.toString(item.year()));
        TableColumn<AnnualSalesComparison, String> revenue = textColumn(
                "Fatturato", item -> currency.format(item.summary().netRevenue())
        );
        TableColumn<AnnualSalesComparison, String> change = textColumn(
                "Δ anno", item -> formatPercentage(item.revenueChangePercentage())
        );
        TableColumn<AnnualSalesComparison, String> quantity = textColumn(
                "Quantità", item -> number.format(item.summary().quantity())
        );
        TableColumn<AnnualSalesComparison, String> customers = textColumn(
                "Clienti", item -> number.format(item.summary().customers())
        );
        TableColumn<AnnualSalesComparison, String> average = textColumn(
                "Media doc.", item -> currency.format(item.summary().averageDocumentValue())
        );
        table.getColumns().addAll(year, revenue, change, quantity, customers, average);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(310);
        table.setPlaceholder(new Label("Nessuno storico disponibile"));
        return table;
    }

    private <T> TableColumn<T, String> textColumn(String title, java.util.function.Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(item -> new SimpleStringProperty(value.apply(item.getValue())));
        return column;
    }

    private TableView<SalesRankingItem> createRankingTable(boolean showDescription) {
        TableView<SalesRankingItem> table = new TableView<>();
        TableColumn<SalesRankingItem, String> code = new TableColumn<>(showDescription ? "Prodotto" : "Codice");
        code.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().code()));
        TableColumn<SalesRankingItem, String> description = new TableColumn<>(showDescription ? "Descrizione" : "Cliente");
        description.setCellValueFactory(item -> new SimpleStringProperty(
                item.getValue().description() == null ? "" : item.getValue().description()
        ));
        TableColumn<SalesRankingItem, String> revenue = new TableColumn<>("Fatturato");
        revenue.setCellValueFactory(item -> new SimpleStringProperty(currency.format(item.getValue().netRevenue())));
        TableColumn<SalesRankingItem, String> quantity = new TableColumn<>("Quantità");
        quantity.setCellValueFactory(item -> new SimpleStringProperty(number.format(item.getValue().quantity())));
        TableColumn<SalesRankingItem, String> documents = new TableColumn<>("Documenti");
        documents.setCellValueFactory(item -> new SimpleStringProperty(number.format(item.getValue().documents())));
        TableColumn<SalesRankingItem, String> percentage = new TableColumn<>("Incidenza");
        percentage.setCellValueFactory(item -> new SimpleStringProperty(
                number.format(item.getValue().percentage()) + "%"
        ));
        table.getColumns().addAll(code, description);
        table.getColumns().addAll(revenue, quantity, documents, percentage);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(310);
        table.setPlaceholder(new Label("Nessun dato"));
        return table;
    }

    private void updateChart(SalesDashboardData data) {
        if (data == null) {
            return;
        }
        ChartMetric metric = chartMetricBox.getValue() == null ? ChartMetric.REVENUE : chartMetricBox.getValue();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(Integer.toString(data.selectedYear()));
        XYChart.Series<String, Number> previousSeries = new XYChart.Series<>();
        previousSeries.setName(Integer.toString(data.selectedYear() - 1));
        int firstMonth = data.filter().from().getMonthValue();
        int lastMonth = data.filter().to().getMonthValue();
        for (int monthNumber = firstMonth; monthNumber <= lastMonth; monthNumber++) {
            MonthlySales month = findMonth(data, monthNumber);
            Number value = metric.value(month);
            String label = Month.of(monthNumber).getDisplayName(TextStyle.SHORT, Locale.ITALY);
            XYChart.Data<String, Number> point = new XYChart.Data<>(label, value);
            String tooltip = Month.of(monthNumber).getDisplayName(TextStyle.FULL, Locale.ITALY)
                    + " " + data.selectedYear() + "\n" + metric.label() + ": " + metric.format(value, currency, number);
            point.nodeProperty().addListener((observable, oldNode, node) -> {
                if (node != null) {
                    Tooltip.install(node, new Tooltip(tooltip));
                }
            });
            series.getData().add(point);
            MonthlySales previousMonth = findMonth(data.previousMonthlySales(), monthNumber);
            previousSeries.getData().add(new XYChart.Data<>(label, metric.value(previousMonth)));
        }
        if (data.previousMonthlySales().isEmpty()) {
            monthlyChart.getData().setAll(series);
        } else {
            monthlyChart.getData().setAll(series, previousSeries);
        }
    }

    private MonthlySales findMonth(SalesDashboardData data, int month) {
        return findMonth(data.monthlySales(), month);
    }

    private MonthlySales findMonth(java.util.List<MonthlySales> values, int month) {
        return values.stream()
                .filter(value -> value.month() == month)
                .findFirst()
                .orElse(new MonthlySales(month, BigDecimal.ZERO, BigDecimal.ZERO, 0));
    }

    private void updateAnnualComparison(SalesDashboardData data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (AnnualSalesComparison annual : data.annualComparisons()) {
            XYChart.Data<String, Number> point = new XYChart.Data<>(
                    Integer.toString(annual.year()), annual.summary().netRevenue()
            );
            String tooltip = annual.year() + "\nFatturato: " + currency.format(annual.summary().netRevenue())
                    + "\nVariazione: " + formatPercentage(annual.revenueChangePercentage());
            point.nodeProperty().addListener((observable, oldNode, node) -> {
                if (node != null) Tooltip.install(node, new Tooltip(tooltip));
            });
            series.getData().add(point);
        }
        annualChart.getData().setAll(series);
        annualTable.getItems().setAll(data.annualComparisons());
    }

    private Label comparisonValue() {
        Label value = new Label("—");
        value.getStyleClass().add("metric-comparison");
        return value;
    }

    private String formatChange(BigDecimal current, BigDecimal previous, int year) {
        if (previous.signum() == 0) return "— nessun confronto disponibile";
        BigDecimal percentage = current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 1, java.math.RoundingMode.HALF_UP);
        String arrow = percentage.signum() > 0 ? "▲ " : percentage.signum() < 0 ? "▼ " : "= ";
        return arrow + formatPercentage(percentage) + " rispetto al " + (year - 1);
    }

    private void updateComparison(Label label, BigDecimal current, BigDecimal previous, int year) {
        label.setText(formatChange(current, previous, year));
        label.getStyleClass().removeAll("comparison-positive", "comparison-negative", "comparison-neutral");
        String style = previous.signum() == 0 || current.compareTo(previous) == 0
                ? "comparison-neutral"
                : current.compareTo(previous) > 0 ? "comparison-positive" : "comparison-negative";
        label.getStyleClass().add(style);
    }

    private String formatPercentage(BigDecimal percentage) {
        if (percentage == null) return "—";
        String prefix = percentage.signum() > 0 ? "+" : "";
        return prefix + number.format(percentage) + "%";
    }

    private Label metricValue() {
        Label value = new Label("—");
        value.getStyleClass().add("dashboard-metric-value");
        return value;
    }

    private void setFiltersDisabled(boolean disabled) {
        yearBox.setDisable(disabled);
        periodBox.setDisable(disabled);
        fromPicker.setDisable(disabled);
        toPicker.setDisable(disabled);
        applyButton.setDisable(disabled);
    }

    public enum PeriodPreset {
        WHOLE_YEAR("Anno intero"),
        FIRST_QUARTER("Primo trimestre"),
        SECOND_QUARTER("Secondo trimestre"),
        THIRD_QUARTER("Terzo trimestre"),
        FOURTH_QUARTER("Quarto trimestre"),
        CUSTOM("Personalizzato");

        private final String label;

        PeriodPreset(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum ChartMetric {
        REVENUE("Fatturato") {
            @Override Number value(MonthlySales month) { return month.netRevenue(); }
            @Override String format(Number value, NumberFormat currency, NumberFormat number) {
                return currency.format(value);
            }
        },
        QUANTITY("Quantità") {
            @Override Number value(MonthlySales month) { return month.quantity(); }
        },
        DOCUMENTS("Documenti") {
            @Override Number value(MonthlySales month) { return month.documents(); }
        };

        private final String label;

        ChartMetric(String label) { this.label = label; }
        abstract Number value(MonthlySales month);
        String label() { return label; }
        String format(Number value, NumberFormat currency, NumberFormat number) { return number.format(value); }
        @Override public String toString() { return label; }
    }
}
