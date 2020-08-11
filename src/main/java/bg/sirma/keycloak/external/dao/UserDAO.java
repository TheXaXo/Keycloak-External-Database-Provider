package bg.sirma.keycloak.external.dao;

import bg.sirma.keycloak.external.Column;
import bg.sirma.keycloak.external.SimpleUserModel;
import bg.sirma.keycloak.external.config.DatabaseConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserDAO {

    private final Connection connection;
    private final DatabaseConfig databaseConfig;

    public UserDAO(Connection connection, DatabaseConfig databaseConfig) {
        this.connection = connection;
        this.databaseConfig = databaseConfig;
    }

    public Connection getConnection() {
        return connection;
    }

    public SimpleUserModel getUserByColumn(RealmModel realm, Column column, String value) {
        List<String> columns = Stream.of(databaseConfig.getUsernameColumn(), databaseConfig.getEmailColumn(), databaseConfig.getPasswordColumn())
                .map(col -> "\"" + col.trim() + "\"")
                .collect(Collectors.toList());

        String columnToSearch = column.equals(Column.USERNAME) ?
                databaseConfig.getUsernameColumn().trim() : databaseConfig.getEmailColumn().trim();

        String sql = "select " + String.join(", ", columns) +
                " from \"" + databaseConfig.getUserTable().trim() + "\"" +
                " where \"" + columnToSearch + "\" = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String username = resultSet.getString(1);
                    String email = resultSet.getString(2);
                    String credential = resultSet.getString(3);
                    Set<RoleModel> roleMappings = this.getRoleMappings(realm, username);

                    return new SimpleUserModel(username, email, credential, roleMappings);
                }

                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<RoleModel> getRoleMappings(RealmModel realm, String username) {
        try (PreparedStatement statement = connection.prepareStatement(this.databaseConfig.getRolesSql())) {
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
        List<String> columns = Stream.of(databaseConfig.getUsernameColumn(), databaseConfig.getEmailColumn(), databaseConfig.getPasswordColumn())
                .map(col -> "\"" + col.trim() + "\"")
                .collect(Collectors.toList());
        String sql = "select " + String.join(", ", columns) +
                " from \"" + databaseConfig.getUserTable().trim() + "\"";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (maxResults != null) {
                if (firstResult != null) {
                    maxResults += firstResult;
                }
                statement.setMaxRows(maxResults);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SimpleUserModel> users = new ArrayList<>();
                while (resultSet.next()) {
                    if (firstResult != null && firstResult > 0) {
                        firstResult--;
                        continue;
                    }
                    String username = resultSet.getString(1);
                    String email = resultSet.getString(2);
                    String credential = resultSet.getString(3);

                    Set<RoleModel> roleMappings = getRoleMappings(realm, username);
                    users.add(new SimpleUserModel(username, email, credential, roleMappings));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SimpleUserModel> searchForUser(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        List<String> columns = Stream.of(databaseConfig.getUsernameColumn(), databaseConfig.getEmailColumn(), databaseConfig.getPasswordColumn())
                .map(col -> "\"" + col.trim() + "\"")
                .collect(Collectors.toList());
        search = search.trim();
        if (!search.startsWith("%")) {
            search = "%" + search;
        }
        if (!search.endsWith("%")) {
            search += "%";
        }
        String sql = "select " + String.join(", ", columns) +
                " from \"" + databaseConfig.getUserTable().trim() + "\"" +
                " where \"" + databaseConfig.getUsernameColumn() + "\" ilike ? or \"" + databaseConfig.getEmailColumn() + "\" ilike ? ";
        // TODO: Add check for full name (first name + last name)

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, search);
            statement.setString(2, search);
            if (maxResults != null) {
                if (firstResult != null) {
                    maxResults += firstResult;
                }
                statement.setMaxRows(maxResults);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SimpleUserModel> users = new ArrayList<>();
                while (resultSet.next()) {
                    if (firstResult != null && firstResult > 0) {
                        firstResult--;
                        continue;
                    }
                    String username = resultSet.getString(1);
                    String email = resultSet.getString(2);
                    String credential = resultSet.getString(3);
                    Set<RoleModel> roleMappings = getRoleMappings(realm, username);
                    users.add(new SimpleUserModel(username, email, credential, roleMappings));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SimpleUserModel> searchForUser(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        // Valid parameters are: "first" - first name "last" - last name "email" - email "username" - username "enabled" - is user enabled (true/false)
        List<String> columns = Stream.of(databaseConfig.getUsernameColumn(), databaseConfig.getEmailColumn(), databaseConfig.getPasswordColumn())
                .map(col -> "\"" + col.trim() + "\"")
                .collect(Collectors.toList());
        params = processParams(params);
        Map<String, Object> statements = new LinkedHashMap<>();
        params.forEach((col, p) -> {
            String statement = getStatementForParam(col);
            statements.put(statement, p);
        });
        String sql = "select " + String.join(", ", columns) +
                " from \"" + databaseConfig.getUserTable().trim() + "\" ";

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
                List<SimpleUserModel> users = new ArrayList<>();
                while (resultSet.next()) {
                    if (firstResult != null && firstResult > 0) {
                        firstResult--;
                        continue;
                    }
                    String username = resultSet.getString(1);
                    String email = resultSet.getString(2);
                    String credential = resultSet.getString(3);
                    Set<RoleModel> roleMappings = getRoleMappings(realm, username);
                    users.add(new SimpleUserModel(username, email, credential, roleMappings));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getStatementForParam(String name) {
        Param param = Param.fromName(name);
        String tableName = param.tableName(databaseConfig);
        return "\"" + tableName + "\" like ?";

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

    enum Param {
        FIRST("first"),
        LAST("last"),
        EMAIL("email"),
        USERNAME("username"),
        ENABLED("enabled");

        private String name;

        Param(String name) {
            this.name = name;
        }

        public static Param fromName(String name) {
            return Arrays.stream(Param.values()).filter(p -> p.getName().equals(name))
                    .findFirst().orElseThrow(() -> new RuntimeException("Unsupported parameter: " + name));
        }

        public String tableName(DatabaseConfig config) {
            switch (this) {
                case FIRST:
                case LAST:
                case ENABLED:
                    throw new RuntimeException("Unsupported parameter: " + name);
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

}


