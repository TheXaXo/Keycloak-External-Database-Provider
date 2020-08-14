package bg.sirma.keycloak.external.config;

public class DatabaseConfig {

    private final String userTable;
    private final String usernameColumn;
    private final String emailColumn;
    private final String first;
    private final String last;
    private final String enabled;
    private final EnabledColumnType enabledType;
    private final String passwordColumn;
    private final String rolesTable;
    private final String roleColumn;
    private final String userIdForeignKeyColumn;
    private final String userIdPrimaryKeyColumn;
    private final String rolesSql;

    public DatabaseConfig(String userTable, String usernameColumn, String emailColumn, String first, String last, String enabled, EnabledColumnType enabledColumnType, String passwordColumn,
                          String rolesTable, String roleColumn, String userIdForeignKeyColumn, String userIdPrimaryKeyColumn,
                          String rolesSql) {
        this.userTable = userTable;
        this.usernameColumn = usernameColumn;
        this.emailColumn = emailColumn;
        this.first = first;
        this.last = last;
        this.enabled = enabled;
        this.enabledType = enabledColumnType;
        this.passwordColumn = passwordColumn;
        this.rolesTable = rolesTable;
        this.roleColumn = roleColumn;
        this.userIdForeignKeyColumn = userIdForeignKeyColumn;
        this.userIdPrimaryKeyColumn = userIdPrimaryKeyColumn;
        this.rolesSql = rolesSql;
    }

    public String getUserTable() {
        return userTable;
    }

    public String getUsernameColumn() {
        return usernameColumn;
    }

    public String getEmailColumn() {
        return emailColumn;
    }

    public String getFirst() {
        return first;
    }

    public String getLast() {
        return last;
    }

    public String getEnabled() {
        return enabled;
    }

    public EnabledColumnType getEnabledType() {
        return enabledType;
    }

    public String getPasswordColumn() {
        return passwordColumn;
    }

    public String getRolesTable() {
        return rolesTable;
    }

    public String getRoleColumn() {
        return roleColumn;
    }

    public String getUserIdForeignKeyColumn() {
        return userIdForeignKeyColumn;
    }

    public String getUserIdPrimaryKeyColumn() {
        return userIdPrimaryKeyColumn;
    }

    public String getRolesSql() {
        return rolesSql;
    }
}
