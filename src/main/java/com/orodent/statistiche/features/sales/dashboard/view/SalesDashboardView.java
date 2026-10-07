package com.orodent.statistiche.features.sales.dashboard.view;

import com.orodent.statistiche.core.components.AppHeader;
import com.orodent.statistiche.features.sales.dashboard.model.MonthlySales;
import com.orodent.statistiche.features.sales.dashboard.model.SalesDashboardData;
import com.orodent.statistiche.features.sales.dashboard.model.SalesRankingItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
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
                metricCard("Fatturato netto", revenueValue, "Valore medio documento", averageValue),
                metricCard("Quantità", quantityValue),
                metricCard("Clienti", customersValue),
                metricCard("Documenti", documentsValue)
        );
        metrics.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        chartMetricBox.setItems(FXCollections.observableArrayList(ChartMetric.values()));
        chartMetricBox.setValue(ChartMetric.REVENUE);
        chartMetricBox.setOnAction(event -> updateChart(currentData));
        HBox chartHeader = new HBox(12, new Label("Andamento mensile"), chartMetricBox);
        chartHeader.setAlignment(Pos.CENTER_LEFT);
        chartHeader.getChildren().getFirst().getStyleClass().add("card-title");
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

        dashboard.getChildren().addAll(metrics, chartCard, rankings);
    }

    private VBox metricCard(String title, Label value) {
        VBox card = new VBox(7);
        card.getStyleClass().add("dashboard-metric");
        Label label = new Label(title);
        label.getStyleClass().add("muted-label");
        card.getChildren().addAll(label, value);
        return card;
    }

    private VBox metricCard(String title, Label value, String secondaryTitle, Label secondaryValue) {
        VBox card = metricCard(title, value);
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
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(300);
        return chart;
    }

    private TableView<SalesRankingItem> createRankingTable(boolean showDescription) {
        TableView<SalesRankingItem> table = new TableView<>();
        TableColumn<SalesRankingItem, String> code = new TableColumn<>(showDescription ? "Prodotto" : "Cliente");
        code.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().code()));
        TableColumn<SalesRankingItem, String> description = new TableColumn<>("Descrizione");
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
        table.getColumns().add(code);
        if (showDescription) {
            table.getColumns().add(description);
        }
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
        }
        monthlyChart.getData().setAll(series);
    }

    private MonthlySales findMonth(SalesDashboardData data, int month) {
        return data.monthlySales().stream()
                .filter(value -> value.month() == month)
                .findFirst()
                .orElse(new MonthlySales(month, BigDecimal.ZERO, BigDecimal.ZERO, 0));
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
