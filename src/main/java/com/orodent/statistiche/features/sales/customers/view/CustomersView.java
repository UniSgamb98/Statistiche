package com.orodent.statistiche.features.sales.customers.view;

import com.orodent.statistiche.features.sales.analysis.model.*;
import com.orodent.statistiche.features.sales.analysis.view.AnalysisView;
import com.orodent.statistiche.features.sales.customers.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public final class CustomersView extends AnalysisView<CustomerAnalysisItem> {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);
    private final LineChart<String, Number> topChart = lineChart(260, "top-customers-chart");
    private final TextField search = new TextField();
    private final VBox detail = new VBox(14);
    private final ScrollPane detailScroll = new ScrollPane(detail);
    private final CustomerDetailScrollState detailScrollState = new CustomerDetailScrollState(detailScroll);
    private String detailCustomerCode;
    private boolean detailAvailable;
    private final Label detailTitle = new Label("Seleziona un cliente");
    private final Label detailSubtitle = new Label("Clicca una riga per aprire storico, prodotti e frequenza di acquisto.");
    private final CustomerKpiPane detailMetrics = new CustomerKpiPane();
    private final CustomerMonthlyPane monthlyPane = new CustomerMonthlyPane();
    private final StackedBarChart<String, Number> yearlyChart = new StackedBarChart<>(new CategoryAxis(), new NumberAxis());
    private final TableView<CustomerProductItem> products = new TableView<>();
    private final ProgressIndicator detailLoading = new ProgressIndicator();
    private final TabPane detailTabs = new TabPane();
    private final CustomerTrendPane trendPane = new CustomerTrendPane();
    private final Label projectionNote = new Label();
    private Consumer<CustomerAnalysisItem> selected = item -> { };
    private List<CustomerAnalysisItem> allCustomers = List.of();
    private boolean notifyCustomerSelection = true;

    public CustomersView() {
        super("Analisi clienti", "Fatturato, comportamento d'acquisto e storico per cliente");
        configureFilters();
        TitledPane topPane = configureTopCustomers();
        configureDetail();

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null && notifyCustomerSelection) selected.accept(value);
        });
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

    private void configureFilters() {
        search.setPromptText("Codice o ragione sociale");
        search.setPrefWidth(260);
        Label searchLabel = new Label("Cerca cliente");
        searchLabel.getStyleClass().add("filter-label");
        filterBar.getChildren().add(1, new VBox(5, searchLabel, search));
        search.textProperty().addListener((obs, old, value) -> filterCustomers(value));
    }

    private TitledPane configureTopCustomers() {
        TitledPane pane = new TitledPane("Top 5 clienti — evoluzione storica", topChart);
        pane.setExpanded(false);
        pane.setAnimated(false);
        pane.expandedProperty().addListener((obs, old, expanded) ->
                pane.setPrefHeight(expanded ? 330 : Region.USE_COMPUTED_SIZE));
        return pane;
    }

    private void configureDetail() {
        detail.getStyleClass().add("customer-detail");
        detail.setPadding(new Insets(18));
        detailTitle.getStyleClass().add("section-title");
        detailSubtitle.getStyleClass().add("muted-label");
        detailSubtitle.setWrapText(true);
        detailLoading.setMaxSize(34, 34);
        detailLoading.setVisible(false);
        configureProducts();

        yearlyChart.getStyleClass().add("customer-yearly-chart");
        projectionNote.getStyleClass().add("customer-projection-note");
        projectionNote.setWrapText(true);
        VBox yearlyContent = new VBox(8, yearlyChart, projectionNote);
        VBox.setVgrow(yearlyChart, Priority.ALWAYS);
        detailTabs.getTabs().setAll(new Tab("Trend recente", trendPane), new Tab("Storico annuale", yearlyContent),
                new Tab("Andamento mensile", monthlyPane), new Tab("Prodotti", products));
        detailTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        detailTabs.setPrefHeight(390);
        detailTabs.getStyleClass().add("customer-detail-tabs");
        detailTabs.setVisible(false);
        detailTabs.setManaged(false);
        VBox.setVgrow(detailTabs, Priority.ALWAYS);
        HBox detailHeading = new HBox(10, detailTitle, detailLoading);
        detailHeading.setAlignment(Pos.CENTER_LEFT);
        detailHeading.setMinHeight(34);
        HBox.setHgrow(detailTitle, Priority.ALWAYS);
        detailTitle.setMaxWidth(Double.MAX_VALUE);
        detail.getChildren().setAll(detailHeading, detailSubtitle, detailMetrics, detailTabs);
        detailMetrics.setVisible(false);
        detailMetrics.setManaged(false);
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
    public int comparisonYears() { return monthlyPane.comparisonYears(); }
    public void onComparisonYearsChanged(Runnable handler) { monthlyPane.onComparisonYearsChanged(handler); }

    public void selectCustomerSilently(CustomerAnalysisItem customer) {
        updateSelectionSilently(() -> {
            if (table.getItems().contains(customer)) {
                table.getSelectionModel().select(customer);
                table.scrollTo(customer);
            } else {
                table.getSelectionModel().clearSelection();
            }
        });
    }

    public void clearCustomerSelection() {
        updateSelectionSilently(() -> table.getSelectionModel().clearSelection());
    }

    public void showCustomerUnavailable(String customerName, String customerCode, int year) {
        detailScrollState.reset();
        detailCustomerCode = customerCode;
        detailAvailable = false;
        detailLoading.setVisible(false);
        detailMetrics.setVisible(false);
        detailMetrics.setManaged(false);
        detailTabs.setVisible(false);
        detailTabs.setManaged(false);
        detailTitle.setText(customerName == null || customerName.isBlank() ? customerCode : customerName);
        detailSubtitle.setText("Il cliente " + customerCode + " non ha vendite disponibili per il " + year + ".");
    }

    private void updateSelectionSilently(Runnable update) {
        notifyCustomerSelection = false;
        try {
            update.run();
        } finally {
            notifyCustomerSelection = true;
        }
    }

    public void showDetailLoading(CustomerAnalysisItem customer) {
        boolean preserveDetail = detailAvailable && Objects.equals(detailCustomerCode, customer.code());
        if (preserveDetail) {
            detailScrollState.capture();
        } else {
            detailScrollState.reset();
            detailAvailable = false;
        }
        detailCustomerCode = customer.code();
        detailTitle.setText(customer.name());
        detailSubtitle.setText("Caricamento del profilo cliente…");
        detailLoading.setVisible(true);
        if (!preserveDetail) {
            detailMetrics.setVisible(false);
            detailMetrics.setManaged(false);
            detailTabs.setVisible(false);
            detailTabs.setManaged(false);
        }
    }

    public void showDetail(CustomerDetailData data) {
        CustomerDetail value = data.detail();
        detailTitle.setText(value.name());
        detailSubtitle.setText("Codice " + value.code() + anagraphic(value));
        detailLoading.setVisible(false);
        detailMetrics.show(value, data.projection());
        detailMetrics.setVisible(true);
        detailMetrics.setManaged(true);
        trendPane.show(data.trend());
        updateYearly(data.yearly(), data.projection());
        updateProjectionNote(data.projection());
        showMonthlyHistory(data.monthly());
        products.getItems().setAll(data.products());
        detailTabs.setVisible(true);
        detailTabs.setManaged(true);
        detailCustomerCode = value.code();
        detailAvailable = true;
        detailScrollState.restoreAfterLayout();
    }

    public void showMonthlyLoading() { monthlyPane.showLoading(); }

    public void showMonthlyHistory(List<CustomerMonthlyValue> values) { monthlyPane.showHistory(values); }

    public void showMonthlyError(Throwable error) { monthlyPane.showError(error); }

    public void showDetailError(Throwable error) {
        detailScrollState.restoreAfterLayout();
        detailLoading.setVisible(false);
        detailSubtitle.setText(error == null || error.getMessage() == null
                ? "Impossibile caricare il cliente." : error.getMessage());
    }

    @Override protected void updateSummary(AnalysisPageData<CustomerAnalysisItem> data) {
        metrics.getChildren().setAll(metric("Clienti attivi", number.format(data.summary().count())),
                metric("Fatturato", currency.format(data.summary().primaryValue())),
                metric("Quantità", number.format(data.summary().secondaryValue())));
    }

    private void updateYearly(List<CustomerYearSummary> values, CustomerRevenueProjection projection) {
        XYChart.Series<String, Number> actual = new XYChart.Series<>(); actual.setName("Fatturato registrato");
        XYChart.Series<String, Number> forecast = new XYChart.Series<>(); forecast.setName("Residuo previsto");
        values.forEach(value -> {
            String year = Integer.toString(value.year());
            actual.getData().add(new XYChart.Data<>(year, value.revenue()));
            BigDecimal remaining = value.year() == projection.year()
                    ? projection.projectedRemainingRevenue() : BigDecimal.ZERO;
            forecast.getData().add(new XYChart.Data<>(year, remaining));
        });
        yearlyChart.getData().setAll(actual, forecast);
        yearlyChart.setLegendVisible(true);
        yearlyChart.setAnimated(false);
    }

    private void updateProjectionNote(CustomerRevenueProjection projection) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (projection.method() == ProjectionMethod.ACTUAL) {
            projectionNote.setText("Consuntivo annuale · Dati disponibili dal "
                    + projection.dataFrom().format(format) + " al " + projection.dataThrough().format(format));
            return;
        }
        String method = switch (projection.method()) {
            case ACTUAL -> throw new IllegalStateException("Metodo consuntivo già gestito");
            case SEASONAL -> "stagionalità di " + projection.historicalYears() + " anni storici";
            case LINEAR -> "andamento lineare (storico insufficiente)";
        };
        projectionNote.setText("Proiezione al 31/12 basata su " + method
                + " · Affidabilità " + projection.confidence().label().toLowerCase(Locale.ITALY)
                + " · Dati disponibili dal " + projection.dataFrom().format(format)
                + " al " + projection.dataThrough().format(format));
    }

    private void configureProducts() {
        products.getColumns().addAll(productColumn("Codice", CustomerProductItem::code),
                productColumn("Descrizione", CustomerProductItem::description),
                productColumn("Fatturato", item -> currency.format(item.revenue())),
                productColumn("Quantità", item -> number.format(item.quantity())));
        products.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private String anagraphic(CustomerDetail value) {
        List<String> parts = new ArrayList<>();
        if (value.country()!=null) parts.add(value.country()); if(value.customerType()!=null)parts.add(value.customerType());
        if(value.agent()!=null)parts.add("Agente " + value.agent()); return parts.isEmpty()?"":" · "+String.join(" · ",parts);
    }

    private static LineChart<String, Number> lineChart(double height, String styleClass) {
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);
        chart.setLegendSide(Side.BOTTOM);
        chart.setMinHeight(height);
        chart.setPrefHeight(height);
        chart.getStyleClass().add(styleClass);
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
