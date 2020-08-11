package bg.sirma.keycloak.external.config;

public enum Database {
    POSTGRESQL("PostgreSQL", "jdbc:postgresql:", "org.postgresql.Driver");

    private String name;
    private String protocol;
    private String driver;

    Database(String name, String protocol, String driver) {
        this.name = name;
        this.protocol = protocol;
        this.driver = driver;
    }

    public static Database fromName(String name) {
        for (Database database : Database.values()) {
            if (database.getName().equalsIgnoreCase(name)) {
                return database;
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
