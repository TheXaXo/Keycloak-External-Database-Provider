package config;

public class DatabaseConfig {

    private final String userTable;
    private final String usernameColumn;
    private final String passwordColumn;
    private final String rolesSql;

    public DatabaseConfig(String userTable, String usernameColumn, String passwordColumn, String rolesSql) {
        this.userTable = userTable;
        this.usernameColumn = usernameColumn;
        this.passwordColumn = passwordColumn;
        this.rolesSql = rolesSql;
    }

    public String getUserTable() {
        return userTable;
    }

    public String getUsernameColumn() {
        return usernameColumn;
    }

    public String getPasswordColumn() {
        return passwordColumn;
    }

    public String getRolesSql() {
        return rolesSql;
    }
}
