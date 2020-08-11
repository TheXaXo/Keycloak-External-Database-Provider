package bg.sirma.keycloak.external.config;

public enum DatabaseEngine {
    POSTGRESQL("PostgreSQL", "jdbc:postgresql:", "org.postgresql.Driver");

    private String name;
    private String protocol;
    private String driver;

    DatabaseEngine(String name, String protocol, String driver) {
        this.name = name;
        this.protocol = protocol;
        this.driver = driver;
    }

    public static DatabaseEngine fromName(String name) {
        for (DatabaseEngine databaseEngine : DatabaseEngine.values()) {
            if (databaseEngine.getName().equalsIgnoreCase(name)) {
                return databaseEngine;
            }
        }

        throw new RuntimeException(name + " is not supported.");
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getDriver() {
        return driver;
    }
}
