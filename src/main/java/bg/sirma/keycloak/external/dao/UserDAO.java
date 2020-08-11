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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
}