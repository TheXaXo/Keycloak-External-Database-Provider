package dao;

import config.DatabaseConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    public String getPasswordHashedByUsername(String username) {
        String sql = "select " + databaseConfig.getPasswordColumn() +
                " from \"" + databaseConfig.getUserTable() + "\"" +
                " where " + databaseConfig.getUsernameColumn() + " = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
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
}