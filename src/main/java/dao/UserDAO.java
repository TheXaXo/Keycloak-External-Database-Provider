package dao;

import config.UserTableConfig;

import java.sql.*;

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

    public String getPasswordHashedByUsername(String username) {
        String sql = "select " + userTableConfig.getPasswordColumn() +
                " from \"" + userTableConfig.getTableName() + "\"" +
                " where " + userTableConfig.getUsernameColumn() + " = ?";

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
}