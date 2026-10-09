package com.orodent.statistiche.core.database;

import java.nio.file.Path;
import java.util.Objects;

public record DatabaseConfiguration(Path home, int databasePort, int discoveryPort, int discoveryTimeoutMs) {
    public static final String DATABASE_NAME = "StatisticheDatabase";
    public static final String USER = "APP";
    public static final String PASSWORD = "pw";

    public DatabaseConfiguration {
        Objects.requireNonNull(home, "home");
        if (databasePort < 1 || databasePort > 65535 || discoveryPort < 1 || discoveryPort > 65535)
            throw new IllegalArgumentException("Porta database o discovery non valida");
        if (discoveryTimeoutMs < 1 || discoveryTimeoutMs > 60_000)
            throw new IllegalArgumentException("Timeout discovery non valido");
        home = home.toAbsolutePath().normalize();
    }

    public static DatabaseConfiguration forHome(Path home) {
        return new DatabaseConfiguration(home,
                Integer.parseInt(System.getProperty("statistiche.database.port", "1527")),
                Integer.parseInt(System.getProperty("statistiche.discovery.port", "45679")),
                Integer.parseInt(System.getProperty("statistiche.discovery.timeout.ms", "1500")));
    }
}
