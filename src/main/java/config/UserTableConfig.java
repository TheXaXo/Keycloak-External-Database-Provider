package config;

public class UserTableConfig {

    private final String tableName;
    private final String usernameColumn;
    private final String passwordColumn;

    public UserTableConfig(String tableName, String usernameColumn, String passwordColumn) {
        this.tableName = tableName;
        this.usernameColumn = usernameColumn;
        this.passwordColumn = passwordColumn;
    }

    public String getTableName() {
        return tableName;
    }

    public String getUsernameColumn() {
        return usernameColumn;
    }

    public String getPasswordColumn() {
        return passwordColumn;
    }
}
