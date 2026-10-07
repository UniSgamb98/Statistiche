package com.orodent.statistiche.core.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class AppHeader extends HBox {

    private final Button homeButton = navigationButton("Home");
    private final Button importButton = navigationButton("Importa vendite");

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
        getChildren().addAll(brand, pageInfo, spacer, homeButton, importButton);
    }

    public Button homeButton() {
        return homeButton;
    }

    public Button importButton() {
        return importButton;
    }

    private Button navigationButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        return button;
    }
}
