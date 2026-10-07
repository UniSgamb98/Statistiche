package com.orodent.statistiche.app;

import com.orodent.statistiche.app.navigation.AppNavigator;
import com.orodent.statistiche.core.components.AppHeader;
import com.orodent.statistiche.features.sales.dashboard.controller.SalesDashboardController;
import com.orodent.statistiche.features.sales.dashboard.view.SalesDashboardView;
import com.orodent.statistiche.features.sales.importing.controller.SalesImportController;
import com.orodent.statistiche.features.sales.importing.view.SalesImportView;
import com.orodent.statistiche.features.customers.importing.controller.CustomerImportController;
import com.orodent.statistiche.features.customers.importing.view.CustomerImportView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public final class AppController implements AppNavigator {

    private static final double INITIAL_WIDTH = 1120;
    private static final double INITIAL_HEIGHT = 760;

    private final Stage stage;
    private final AppContainer app;
    private final String globalCss;
    private Scene scene;
    private Runnable activePageCleanup = () -> { };

    public AppController(Stage stage, AppContainer app, String globalCss) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.app = Objects.requireNonNull(app, "app");
        this.globalCss = Objects.requireNonNull(globalCss, "globalCss");
        showHome();
    }

    @Override
    public void showHome() {
        SalesDashboardView view = new SalesDashboardView();
        configureHeader(view.header());
        SalesDashboardController controller = new SalesDashboardController(
                view,
                app.salesDashboardService(),
                this
        );
        showView(view, controller::dispose, "/css/features/sales-dashboard.css");
        stage.setTitle("Statistiche - Dashboard vendite");
        controller.loadInitialData();
    }

    @Override
    public void showSalesImport() {
        SalesImportView view = new SalesImportView();
        configureHeader(view.header());
        SalesImportController controller = new SalesImportController(
                view,
                app.venditeCsvImportService()
        );
        showView(view, controller::dispose, "/css/features/sales-import.css");
        stage.setTitle("Statistiche - Importa vendite");
    }

    @Override
    public void showCustomerImport() {
        CustomerImportView view = new CustomerImportView();
        configureHeader(view.header());
        CustomerImportController controller = new CustomerImportController(view, app.clientiCsvImportService());
        showView(view, controller::dispose, "/css/features/sales-import.css");
        stage.setTitle("Statistiche - Importa anagrafica clienti");
    }

    private void showView(Parent root, String... extraCss) {
        showView(root, () -> { }, extraCss);
    }

    private void showView(Parent root, Runnable cleanup, String... extraCss) {
        runActivePageCleanup();
        activePageCleanup = cleanup;

        if (scene == null) {
            scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        scene.getStylesheets().setAll(globalCss);
        for (String css : extraCss) {
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource(css),
                    "CSS non trovato: " + css
            ).toExternalForm());
        }
    }

    private void configureHeader(AppHeader header) {
        header.homeButton().setOnAction(event -> showHome());
        header.salesImportItem().setOnAction(event -> showSalesImport());
        header.customersImportItem().setOnAction(event -> showCustomerImport());
    }

    private void runActivePageCleanup() {
        try {
            activePageCleanup.run();
        } catch (RuntimeException exception) {
            System.err.println("Errore durante la chiusura della pagina: " + exception.getMessage());
        }
    }

    public void shutdown() {
        runActivePageCleanup();
        activePageCleanup = () -> { };
        app.shutdown();
    }
}
