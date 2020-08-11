package bg.sirma.keycloak.external.dao;

import bg.sirma.keycloak.external.SimpleUserModel;
import bg.sirma.keycloak.external.config.UserTableConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserDAO {

    private final Connection connection;
    private final UserTableConfig userTableConfig;

    public UserDAO(Connection connection, UserTableConfig userTableConfig) {
        this.connection = connection;
        this.userTableConfig = userTableConfig;
    }

    public Connection getConnection() {
        return connection;
    }

    public SimpleUserModel getUserByUsername(String usernameToSearch) {
        List<String> columns = Stream.of(userTableConfig.getUsernameColumn(), userTableConfig.getEmailColumn(), userTableConfig.getPasswordColumn())
                .map(col -> "\"" + col.trim() + "\"")
                .collect(Collectors.toList());
        String sql = "select " + String.join(", ", columns) +
                " from \"" + userTableConfig.getTableName().trim() + "\"" +
                " where \"" + userTableConfig.getUsernameColumn().trim() + "\" = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usernameToSearch);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String username = resultSet.getString(1);
                    String email = resultSet.getString(2);
                    String credential = resultSet.getString(3);
                    return new SimpleUserModel(username, email, credential);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SimpleUserModel getUserByEmail(String emailToSearch) {
        List<String> columns = Stream.of(userTableConfig.getUsernameColumn(), userTableConfig.getEmailColumn(), userTableConfig.getPasswordColumn())
                .map(col -> "\"" + col.trim() + "\"")
                .collect(Collectors.toList());
        String sql = "select " + String.join(", ", columns) +
                " from \"" + userTableConfig.getTableName().trim() + "\"" +
                " where \"" + userTableConfig.getEmailColumn().trim() + "\" = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, emailToSearch);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String username = resultSet.getString(1);
                    String email = resultSet.getString(2);
                    String credential = resultSet.getString(3);
                    return new SimpleUserModel(username, email, credential);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}