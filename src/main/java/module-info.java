module com.orodent.statistiche {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.derby.server;


    opens com.orodent.statistiche to javafx.fxml;
    exports com.orodent.statistiche;
    exports com.orodent.statistiche.core.database;
    exports com.orodent.statistiche.core.database.model;
    exports com.orodent.statistiche.core.database.repository;
    exports com.orodent.statistiche.core.database.repository.impl;
}
