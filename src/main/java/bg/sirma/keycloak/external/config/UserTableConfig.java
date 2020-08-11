package bg.sirma.keycloak.external.config;

public class UserTableConfig {

    private final String tableName;
    private final String usernameColumn;
    private final String emailColumn;
    private final String passwordColumn;

    public UserTableConfig(String tableName, String usernameColumn, String emailColumn, String passwordColumn) {
        this.tableName = tableName;
        this.usernameColumn = usernameColumn;
        this.emailColumn = emailColumn;
        this.passwordColumn = passwordColumn;
    }

    public String getTableName() {
        return tableName;
    }

    public String getUsernameColumn() {
        return usernameColumn;
    }

    public String getEmailColumn() {
        return emailColumn;
    }

    public String getPasswordColumn() {
        return passwordColumn;
    }
}
