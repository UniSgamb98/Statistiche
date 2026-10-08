package com.orodent.statistiche.features.sales.customers.view;

import com.orodent.statistiche.features.sales.analysis.model.*;
import com.orodent.statistiche.features.sales.analysis.view.AnalysisView;
import com.orodent.statistiche.features.sales.customers.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.NumberFormat;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Consumer;

public final class CustomersView extends AnalysisView<CustomerAnalysisItem> {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);
    private final LineChart<String, Number> topChart = lineChart(260);
    private final TextField search = new TextField();
    private final VBox detail = new VBox(14);
    private final Label detailTitle = new Label("Seleziona un cliente");
    private final Label detailSubtitle = new Label("Clicca una riga per aprire storico, prodotti e frequenza di acquisto.");
    private final GridPane detailMetrics = new GridPane();
    private final ComboBox<Integer> comparisonYears = new ComboBox<>();
    private final LineChart<String, Number> monthlyChart = lineChart(260);
    private final BarChart<String, Number> yearlyChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final TableView<CustomerProductItem> products = new TableView<>();
    private Consumer<CustomerAnalysisItem> selected = item -> { };
    private List<CustomerAnalysisItem> allCustomers = List.of();

    public CustomersView() {
        super("Analisi clienti", "Fatturato, comportamento d'acquisto e storico per cliente");
        comparisonYears.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        comparisonYears.setValue(1);
        search.setPromptText("Codice o ragione sociale");
        search.setPrefWidth(260);
        Label searchLabel = new Label("Cerca cliente");
        searchLabel.getStyleClass().add("filter-label");
        filterBar.getChildren().add(1, new VBox(5, searchLabel, search));
        search.textProperty().addListener((obs, old, value) -> filterCustomers(value));
        TitledPane topPane = new TitledPane("Top 5 clienti — evoluzione storica", topChart);
        topChart.getStyleClass().add("top-customers-chart");
        topPane.setExpanded(false);
        topPane.setAnimated(false);
        topPane.expandedProperty().addListener((obs, old, expanded) -> topPane.setPrefHeight(
                expanded ? 330 : Region.USE_COMPUTED_SIZE
        ));

        detail.getStyleClass().add("customer-detail");
        detail.setPadding(new Insets(18));
        detailTitle.getStyleClass().add("section-title");
        detailSubtitle.getStyleClass().add("muted-label");
        detailSubtitle.setWrapText(true);
        detailMetrics.setHgap(10); detailMetrics.setVgap(10);
        detail.getChildren().addAll(detailTitle, detailSubtitle);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null) selected.accept(value);
        });
        ScrollPane detailScroll = new ScrollPane(detail);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.getStyleClass().add("customer-detail-scroll");
        SplitPane split = new SplitPane(table, detailScroll);
        split.setDividerPositions(0.55);
        split.setMinHeight(480);
        split.setPrefHeight(620);
        VBox.setVgrow(split, Priority.ALWAYS);
        content().getChildren().remove(table);
        content().getChildren().add(2, topPane);
        content().getChildren().add(3, split);
        setCenter(null);
        ScrollPane pageScroll = new ScrollPane(content());
        pageScroll.setFitToWidth(true);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.getStyleClass().add("dashboard-scroll");
        setCenter(pageScroll);
    }

    @Override protected void configureTable(TableView<CustomerAnalysisItem> table) {
        table.getColumns().addAll(column("Codice", CustomerAnalysisItem::code),
                column("Ragione sociale", CustomerAnalysisItem::name),
                column("Fatturato", item -> currency.format(item.revenue())),
                column("Quantità", item -> number.format(item.quantity())),
                column("Documenti", item -> number.format(item.documents())),
                column("Ultima vendita", item -> item.lastSale().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    public void showOverview(CustomerOverviewData overview) {
        showData(overview.customers());
        allCustomers = overview.customers().items();
        filterCustomers(search.getText());
        Map<String, XYChart.Series<String, Number>> series = new LinkedHashMap<>();
        for (TopCustomerHistory value : overview.topHistory()) {
            XYChart.Series<String, Number> customer = series.computeIfAbsent(value.customerCode(), code -> {
                XYChart.Series<String, Number> created = new XYChart.Series<>();
                created.setName(value.customerName()); return created;
            });
            customer.getData().add(new XYChart.Data<>(Integer.toString(value.year()), value.revenue()));
        }
        topChart.getData().setAll(series.values());
    }

    private void filterCustomers(String text) {
        String term = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (allCustomers.isEmpty()) return;
        table.getItems().setAll(allCustomers.stream().filter(item -> term.isEmpty()
                || item.code().toLowerCase(Locale.ROOT).contains(term)
                || item.name().toLowerCase(Locale.ROOT).contains(term)).toList());
    }

    public void onCustomerSelected(Consumer<CustomerAnalysisItem> handler) { selected = handler; }
    public int comparisonYears() { return comparisonYears.getValue() == null ? 1 : comparisonYears.getValue(); }
    public void onComparisonYearsChanged(Runnable handler) {
        comparisonYears.setOnAction(event -> handler.run());
    }

    public void showDetailLoading(CustomerAnalysisItem customer) {
        detailTitle.setText(customer.name());
        detailSubtitle.setText("Caricamento del profilo cliente…");
        detail.getChildren().setAll(detailTitle, detailSubtitle, new ProgressIndicator());
    }

    public void showDetail(CustomerDetailData data) {
        CustomerDetail value = data.detail();
        detailTitle.setText(value.name());
        detailSubtitle.setText("Codice " + value.code() + anagraphic(value));
        detailMetrics.getChildren().clear();
        addMetric(0, 0, "Fatturato", currency.format(value.revenue()));
        addMetric(1, 0, "Documenti", number.format(value.documents()));
        addMetric(0, 1, "Valore medio", currency.format(value.averageDocumentValue()));
        addMetric(1, 1, "Quantità media", number.format(value.averageDocumentQuantity()));
        addMetric(0, 2, "Frequenza media", frequency(value.averageFrequencyDays()));
        addMetric(1, 2, "Frequenza tipica", frequency(value.typicalFrequencyDays()));
        addMetric(0, 3, "Ultimo acquisto", value.lastPurchase().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        addMetric(1, 3, "Recenza", value.daysSinceLastPurchase() + " giorni fa");

        updateYearly(data.yearly());
        updateMonthly(data.monthly());
        configureProducts();
        products.getItems().setAll(data.products());
        HBox compare = new HBox(8, new Label("Confronta con anni precedenti:"), comparisonYears);
        compare.setAlignment(Pos.CENTER_LEFT);
        TabPane tabs = new TabPane(new Tab("Storico annuale", yearlyChart),
                new Tab("Andamento mensile", new VBox(8, compare, monthlyChart)), new Tab("Prodotti", products));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefHeight(390);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        detail.getChildren().setAll(detailTitle, detailSubtitle, detailMetrics, tabs);
    }

    public void showDetailError(Throwable error) {
        detailSubtitle.setText(error == null || error.getMessage() == null
                ? "Impossibile caricare il cliente." : error.getMessage());
        detail.getChildren().setAll(detailTitle, detailSubtitle);
    }

    @Override protected void updateSummary(AnalysisPageData<CustomerAnalysisItem> data) {
        metrics.getChildren().setAll(metric("Clienti attivi", number.format(data.summary().count())),
                metric("Fatturato", currency.format(data.summary().primaryValue())),
                metric("Quantità", number.format(data.summary().secondaryValue())));
    }

    private void updateYearly(List<CustomerYearSummary> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("Fatturato");
        values.forEach(value -> series.getData().add(new XYChart.Data<>(Integer.toString(value.year()), value.revenue())));
        yearlyChart.getData().setAll(series); yearlyChart.setLegendVisible(false); yearlyChart.setAnimated(false);
    }

    private void updateMonthly(List<CustomerMonthlyValue> values) {
        Map<Integer, XYChart.Series<String, Number>> series = new LinkedHashMap<>();
        values.forEach(value -> {
            XYChart.Series<String, Number> year = series.computeIfAbsent(value.year(), key -> {
                XYChart.Series<String, Number> created = new XYChart.Series<>(); created.setName(Integer.toString(key)); return created;
            });
            String month = Month.of(value.month()).getDisplayName(TextStyle.SHORT, Locale.ITALY);
            year.getData().add(new XYChart.Data<>(month, value.revenue()));
        });
        monthlyChart.getData().setAll(series.values());
    }

    private void configureProducts() {
        if (!products.getColumns().isEmpty()) return;
        products.getColumns().addAll(productColumn("Codice", CustomerProductItem::code),
                productColumn("Descrizione", CustomerProductItem::description),
                productColumn("Fatturato", item -> currency.format(item.revenue())),
                productColumn("Quantità", item -> number.format(item.quantity())));
        products.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void addMetric(int column, int row, String title, String value) { detailMetrics.add(metric(title, value), column, row); }
    private String frequency(java.math.BigDecimal days) { return days == null ? "Non disponibile" : "ogni " + number.format(days) + " giorni"; }
    private String anagraphic(CustomerDetail value) {
        List<String> parts = new ArrayList<>();
        if (value.country()!=null) parts.add(value.country()); if(value.customerType()!=null)parts.add(value.customerType());
        if(value.agent()!=null)parts.add("Agente " + value.agent()); return parts.isEmpty()?"":" · "+String.join(" · ",parts);
    }

    private static LineChart<String, Number> lineChart(double height) {
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);
        chart.setLegendSide(Side.BOTTOM);
        chart.setMinHeight(height);
        chart.setPrefHeight(height);
        return chart;
    }

    private TableColumn<CustomerAnalysisItem, String> column(String title, java.util.function.Function<CustomerAnalysisItem, String> value) {
        TableColumn<CustomerAnalysisItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(item -> new SimpleStringProperty(value.apply(item.getValue()))); return column;
    }
    private TableColumn<CustomerProductItem, String> productColumn(String title, java.util.function.Function<CustomerProductItem, String> value) {
        TableColumn<CustomerProductItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(item -> new SimpleStringProperty(value.apply(item.getValue()))); return column;
    }
}
