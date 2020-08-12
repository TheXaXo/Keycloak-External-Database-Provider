package bg.sirma.keycloak.external.dao;

import bg.sirma.keycloak.external.Pair;
import bg.sirma.keycloak.external.SimpleUserModel;
import bg.sirma.keycloak.external.UserColumn;
import bg.sirma.keycloak.external.config.DatabaseConfig;
import bg.sirma.keycloak.external.config.EnabledColumnType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserDAO {

    private final Connection connection;
    private final DatabaseConfig databaseConfig;
    private final String baseSql;

    public UserDAO(Connection connection, DatabaseConfig databaseConfig) {
        this.connection = connection;
        this.databaseConfig = databaseConfig;

        this.baseSql = String.format("select user_table.\"%s\" as user_name, user_table.\"%s\" as email_address," +
                        " user_table.\"%s\" as first_name, user_table.\"%s\" as last_name, user_table.\"%s\" as  is_enabled," +
                        " user_table.\"%s\" as credential from \"%s\" user_table ",
                databaseConfig.getUsernameColumn(), databaseConfig.getEmailColumn(), databaseConfig.getFirst(),
                databaseConfig.getLast(), databaseConfig.getEnabled(), databaseConfig.getPasswordColumn(), databaseConfig.getUserTable());
    }

    public Connection getConnection() {
        return connection;
    }

    public SimpleUserModel getUserByColumn(RealmModel realm, UserColumn column, String value) {
        String columnToSearch = column.columnName(databaseConfig);
        String sql = baseSql +
                " where \"" + columnToSearch + "\" = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return getSingleUserModel(realm, resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<RoleModel> getRoleMappings(RealmModel realm, String username) {
        if (databaseConfig.getRolesSql() == null) {
            return new HashSet<>();
        }
        try (PreparedStatement statement = connection.prepareStatement(databaseConfig.getRolesSql())) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                Set<RoleModel> roleMappings = new HashSet<>();

                while (resultSet.next()) {
                    String role = resultSet.getString(1);
                    RoleModel roleMapping = realm.getRole(role);

                    if (roleMapping == null) {
                        roleMapping = realm.addRole(role);
                    }

                    roleMappings.add(roleMapping);
                }

                return roleMappings;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
        try (PreparedStatement statement = connection.prepareStatement(baseSql)) {
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
        String sql = baseSql +
                " where \"" + databaseConfig.getUsernameColumn() +
                "\" like ? or \"" + databaseConfig.getEmailColumn() +
                "\" like ? or \"" + databaseConfig.getFirst() + "\" || ' ' || \"" + databaseConfig.getLast() + "\" like ? ";

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
        String tableName = userColumn.columnName(databaseConfig);
        if (userColumn == UserColumn.ENABLED) {
            if (databaseConfig.getEnabledType() == EnabledColumnType.BOOLEAN) {
                return Pair.of("\"" + tableName + "\" = ?", Boolean.valueOf(value));
            } else {
                if (value.equals("true")) {
                    return Pair.of("\"" + tableName + "\" = ?", 1);
                } else {
                    return Pair.of("\"" + tableName + "\" <> ?", 1);
                }
            }
        }
        return Pair.of("\"" + tableName + "\" like ?", value);
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
            Set<RoleModel> roleMappings = this.getRoleMappings(realm, username);

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
            String credential = resultSet.getString(6);
            Boolean enabled = null;
            if (databaseConfig.getEnabledType() == EnabledColumnType.BOOLEAN) {
                enabled = resultSet.getBoolean(5);
            } else {
                enabled = resultSet.getInt(5) > 0;
            }
            Set<RoleModel> roleMappings = this.getRoleMappings(realm, username);

            return new SimpleUserModel(username, email, firstName, lastName, enabled, credential, roleMappings);
        }
        return null;
    }

}


