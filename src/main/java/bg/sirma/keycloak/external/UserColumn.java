package bg.sirma.keycloak.external;

import bg.sirma.keycloak.external.config.DatabaseConfig;
import bg.sirma.keycloak.external.dao.UserDAO;

import java.util.Arrays;

public enum UserColumn {
    FIRST("first"),
    LAST("last"),
    EMAIL("email"),
    USERNAME("username"),
    ENABLED("enabled");

    private final String name;

    UserColumn(String name) {
        this.name = name;
    }

    public static UserColumn fromName(String name) {
        return Arrays.stream(UserColumn.values()).filter(p -> p.getName().equals(name))
                .findFirst().orElseThrow(() -> new RuntimeException("Unsupported parameter: " + name));
    }

    public String columnName(DatabaseConfig config) {
        switch (this) {
            case FIRST:
                return config.getFirst();
            case LAST:
                return config.getLast();
            case ENABLED:
                return config.getEnabled();
            case USERNAME:
                return config.getUsernameColumn();
            case EMAIL:
                return config.getEmailColumn();
        }
        throw new RuntimeException("Unsupported parameter: " + name);
    }

    public String getName() {
        return name;
    }
}
