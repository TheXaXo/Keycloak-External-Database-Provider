package bg.sirma.keycloak.external.dao;

import bg.sirma.keycloak.external.Pair;
import bg.sirma.keycloak.external.SimpleUserModel;
import bg.sirma.keycloak.external.UserColumn;
import bg.sirma.keycloak.external.config.DatabaseConfig;
import bg.sirma.keycloak.external.config.EnabledColumnType;
import org.apache.commons.lang.StringUtils;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.sql.*;
import java.util.*;

public class UserDAO {
    private static final String userTableAlias = "user_table";
    private static final String roleTableAlias = "role_table";
    private final Connection connection;
    private final DatabaseConfig databaseConfig;
    private final boolean supportsRoles;
    private final String baseSql;
    private final String groupBySql;

    public UserDAO(Connection connection, DatabaseConfig cfg) {
        this.connection = connection;
        this.databaseConfig = cfg;

        if (StringUtils.isNotEmpty(cfg.getRolesTable()) && StringUtils.isNotEmpty(cfg.getRoleColumn()) &&
                StringUtils.isNotEmpty(cfg.getUserIdForeignKeyColumn()) && StringUtils.isNotEmpty(cfg.getUserIdPrimaryKeyColumn())) {
            supportsRoles = true;
            this.baseSql = String.format("select %s.\"%s\" as user_name, %s.\"%s\" as email_address," +
                            " %s.\"%s\" as first_name, %s.\"%s\" as last_name, %s.\"%s\" as  is_enabled," +
                            " %s.\"%s\" as credential, array_agg(%s.\"%s\") as user_roles from \"%s\" %s " +
                    " left join \"%s\" as %s on %s.\"%s\"=%s.\"%s\" ",
                    userTableAlias, cfg.getUsernameColumn(),
                    userTableAlias, cfg.getEmailColumn(),
                    userTableAlias, cfg.getFirst(),
                    userTableAlias, cfg.getLast(),
                    userTableAlias, cfg.getEnabled(),
                    userTableAlias, cfg.getPasswordColumn(),
                    roleTableAlias, cfg.getRoleColumn(),
                    cfg.getUserTable(), userTableAlias,
                    cfg.getRolesTable(), roleTableAlias,
                    roleTableAlias, cfg.getUserIdForeignKeyColumn(),
                    userTableAlias, cfg.getUserIdPrimaryKeyColumn());
            this.groupBySql = " group by 1, 2, 3, 4, 5, 6 ";
        } else {
            supportsRoles = false;
            this.baseSql = String.format("select %s.\"%s\" as user_name, %s.\"%s\" as email_address," +
                            " %s.\"%s\" as first_name, %s.\"%s\" as last_name, %s.\"%s\" as  is_enabled," +
                            " %s.\"%s\" as credential from \"%s\" %s ",
                    userTableAlias, cfg.getUsernameColumn(),
                    userTableAlias, cfg.getEmailColumn(),
                    userTableAlias, cfg.getFirst(),
                    userTableAlias, cfg.getLast(),
                    userTableAlias, cfg.getEnabled(),
                    userTableAlias, cfg.getPasswordColumn(),
                    cfg.getUserTable(), userTableAlias);
            this.groupBySql = "";
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public SimpleUserModel getUserByColumn(RealmModel realm, UserColumn column, String value) {
        String columnToSearch = column.columnName(databaseConfig);
        String sql = baseSql +
                " where "+userTableAlias+".\"" + columnToSearch + "\" = ? " +
                (supportsRoles ? groupBySql : "");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return getSingleUserModel(realm, resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<RoleModel> getRoleMappings(RealmModel realm, String[] roles) {
        Set<RoleModel> roleMappings = new HashSet<>();
        for (String role : roles) {
            RoleModel roleMapping = realm.getRole(role);
            if (roleMapping == null) {
                roleMapping = realm.addRole(role);
            }
            roleMappings.add(roleMapping);
        }
        return roleMappings;
    }

    public int getUsersCount() {
        String sql = "select count(*)" +
                " from \"" + databaseConfig.getUserTable().trim() + "\"";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                throw new RuntimeException("Error while fetching users count.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SimpleUserModel> getUsers(RealmModel realm) {
        return getUsers(realm, null, null);
    }

    public List<SimpleUserModel> getUsers(RealmModel realm, Integer firstResult, Integer maxResults) {
        try (PreparedStatement statement = connection.prepareStatement(baseSql + groupBySql)) {
            if (maxResults != null) {
                if (firstResult != null) {
                    maxResults += firstResult;
                }
                statement.setMaxRows(maxResults);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return getUserModels(realm, firstResult, resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SimpleUserModel> searchForUser(RealmModel realm, String search, Integer firstResult, Integer maxResults) {

        search = search.trim();
        if (!search.startsWith("%")) {
            search = "%" + search;
        }
        if (!search.endsWith("%")) {
            search += "%";
        }
        String sql = String.format("%s where %s.\"%s\" like ? or %s.\"%s\" like ? or %s.\"%s\" || ' ' || %s.\"%s\" like ? %s",
                baseSql,
                userTableAlias, databaseConfig.getUsernameColumn(),
                userTableAlias, databaseConfig.getEmailColumn(),
                userTableAlias, databaseConfig.getFirst(),
                userTableAlias, databaseConfig.getLast(),
                supportsRoles ? groupBySql : "");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, search);
            statement.setString(2, search);
            statement.setString(3, search);
            if (maxResults != null) {
                if (firstResult != null) {
                    maxResults += firstResult;
                }
                statement.setMaxRows(maxResults);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return getUserModels(realm, firstResult, resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SimpleUserModel> searchForUser(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        params = processParams(params);
        Map<String, Object> statements = new LinkedHashMap<>();
        params.forEach((col, p) -> {
            UserColumn userColumn = UserColumn.fromName(col);
            Pair<String, Object> pair = getStatementForParam(userColumn, p);
            statements.put(pair.getLeft(), pair.getRight());
        });
        String sql = baseSql;

        if (!statements.isEmpty()) {
            sql = sql + " where " +
                    String.join(" and ", statements.keySet());
        }
        sql += (supportsRoles ? groupBySql : "");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            for (Object p : statements.values()) {
                statement.setObject(i++, p);
            }
            if (maxResults != null) {
                if (firstResult != null) {
                    maxResults += firstResult;
                }
                statement.setMaxRows(maxResults);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return getUserModels(realm, firstResult, resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Pair<String, Object> getStatementForParam(UserColumn userColumn, String value) {
        String columnName = userColumn.columnName(databaseConfig);
        if (userColumn == UserColumn.ENABLED) {
            if (databaseConfig.getEnabledType() == EnabledColumnType.BOOLEAN) {
                return Pair.of(String.format("%s.\"%s\" = ?", userTableAlias, columnName), Boolean.valueOf(value));
            } else {
                if (value.equals("true")) {
                    return Pair.of(String.format("%s.\"%s\" = ?", userTableAlias, columnName), 1);
                } else {
                    return Pair.of(String.format("%s.\"%s\" <> ?", userTableAlias, columnName), 1);
                }
            }
        }
        return Pair.of(String.format("%s.\"%s\" like ?", userTableAlias, columnName), value);
    }

    private Map<String, String> processParams(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();
        params.forEach((key, value) -> {
            String newValue = value.trim();
            if (!newValue.startsWith("%")) {
                newValue = "%" + newValue;
            }
            if (!newValue.endsWith("%")) {
                newValue += "%";
            }
            result.put(key, newValue);
        });
        return result;
    }

    private List<SimpleUserModel> getUserModels(RealmModel realm, Integer firstResult, ResultSet resultSet) throws SQLException {
        List<SimpleUserModel> users = new ArrayList<>();
        while (resultSet.next()) {
            if (firstResult != null && firstResult > 0) {
                firstResult--;
                continue;
            }
            String username = resultSet.getString(1);
            String email = resultSet.getString(2);
            String firstName = resultSet.getString(3);
            String lastName = resultSet.getString(4);
            String credential = resultSet.getString(6);

            Boolean enabled = null;
            if (databaseConfig.getEnabledType() == EnabledColumnType.BOOLEAN) {
                enabled = resultSet.getBoolean(5);
            } else {
                enabled = resultSet.getInt(5) > 0;
            }
            Set<RoleModel> roleMappings = new HashSet<>();
            if (supportsRoles) {
                String[] roles = (String[]) resultSet.getArray(7).getArray();
                roleMappings = this.getRoleMappings(realm, roles);
            }

            users.add(new SimpleUserModel(username, email, firstName, lastName, enabled, credential, roleMappings));
        }
        return users;
    }

    private SimpleUserModel getSingleUserModel(RealmModel realm, ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            String username = resultSet.getString(1);
            String email = resultSet.getString(2);
            String firstName = resultSet.getString(3);
            String lastName = resultSet.getString(4);
            Boolean enabled = null;
            if (databaseConfig.getEnabledType() == EnabledColumnType.BOOLEAN) {
                enabled = resultSet.getBoolean(5);
            } else {
                enabled = resultSet.getInt(5) > 0;
            }
            Set<RoleModel> roleMappings = new HashSet<>();
            if (supportsRoles) {
                String[] roles = (String[]) resultSet.getArray(7).getArray();
                roleMappings = this.getRoleMappings(realm, roles);
            }
            String credential = resultSet.getString(6);
            return new SimpleUserModel(username, email, firstName, lastName, enabled, credential, roleMappings);
        }
        return null;
    }

}


