package com.orodent.statistiche.core.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class AppHeader extends HBox {

    private final Button homeButton = navigationButton("Home");
    private final MenuButton analysisMenu = navigationMenu("Analisi");
    private final MenuItem customersItem = new MenuItem("Clienti");
    private final MenuItem productsItem = new MenuItem("Prodotti");
    private final MenuItem discountsItem = new MenuItem("Sconti");
    private final MenuItem archiveItem = new MenuItem("Archivio vendite");
    private final MenuButton importMenu = navigationMenu("Importazioni");
    private final MenuItem salesImportItem = new MenuItem("Vendite");
    private final MenuItem customersImportItem = new MenuItem("Anagrafica clienti");

    public AppHeader(String pageTitle, String pageDescription) {
        getStyleClass().add("app-header");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(18);

        Label brand = new Label("STATISTICHE");
        brand.getStyleClass().add("brand-label");

        VBox pageInfo = new VBox(2);
        Label title = new Label(pageTitle);
        title.getStyleClass().add("page-title");
        Label description = new Label(pageDescription);
        description.getStyleClass().add("page-description");
        pageInfo.getChildren().addAll(title, description);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        analysisMenu.getItems().addAll(customersItem, productsItem, discountsItem, archiveItem);
        importMenu.getItems().addAll(salesImportItem, customersImportItem);
        getChildren().addAll(brand, pageInfo, spacer, homeButton, analysisMenu, importMenu);
    }

    public Button homeButton() {
        return homeButton;
    }

    public MenuItem customersItem() { return customersItem; }
    public MenuItem productsItem() { return productsItem; }
    public MenuItem discountsItem() { return discountsItem; }
    public MenuItem archiveItem() { return archiveItem; }
    public MenuItem salesImportItem() { return salesImportItem; }
    public MenuItem customersImportItem() { return customersImportItem; }

    private Button navigationButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        return button;
    }

    private MenuButton navigationMenu(String text) {
        MenuButton menu = new MenuButton(text);
        menu.getStyleClass().addAll("nav-button", "nav-menu");
        return menu;
    }
}
