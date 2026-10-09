module com.orodent.statistiche {
    requires javafx.controls;
    requires java.sql;
    requires org.apache.derby.server;
    requires org.apache.derby.tools;


    exports com.orodent.statistiche;
    exports com.orodent.statistiche.app;
    exports com.orodent.statistiche.core.csv;
    exports com.orodent.statistiche.core.database;
    exports com.orodent.statistiche.core.database.csv;
    exports com.orodent.statistiche.core.database.model;
    exports com.orodent.statistiche.core.database.repository;
    exports com.orodent.statistiche.core.database.repository.impl;
    exports com.orodent.statistiche.core.database.service;
    exports com.orodent.statistiche.core;
    exports com.orodent.statistiche.features.sales.dashboard.service;
    exports com.orodent.statistiche.features.sales.analysis.service;
    exports com.orodent.statistiche.features.sales.analysis.model;
}
