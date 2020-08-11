import config.DatabaseEngine;
import config.PasswordHashingAlgorithm;
import config.DatabaseConfig;
import dao.UserDAO;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class ExternalDatabaseStorageProviderFactory implements UserStorageProviderFactory<ExternalDatabaseStorageProvider> {

    public static final String PROVIDER_NAME = "external-database";
    public static final String DB_DATABASE_ENGINE_KEY = "db:database-engine";
    public static final String DB_HOST_KEY = "db:host";
    public static final String DB_PORT_KEY = "db:port";
    public static final String DB_DATABASE_NAME_KEY = "db:database-name";
    public static final String DB_USERNAME_KEY = "db:username";
    public static final String DB_PASSWORD_KEY = "db:password";
    public static final String DB_USER_TABLE_KEY = "db:user-table";
    public static final String DB_USER_TABLE_USERNAME_COLUMN_KEY = "db:user-table-username";
    public static final String DB_USER_TABLE_PASSWORD_COLUMN_KEY = "db:user-table-password";
    public static final String DB_PASSWORD_HASHING_ALGORITHM_KEY = "db:password-hashing-algorithm";
    public static final String DB_ROLES_SQL_KEY = "db:roles-sql";

    @Override
    public ExternalDatabaseStorageProvider create(KeycloakSession session, ComponentModel model) {
        UserDAO userDAO;
        PasswordHashingAlgorithm passwordHashingAlgorithm;

        try {
            MultivaluedHashMap<String, String> config = model.getConfig();

            DatabaseEngine databaseEngine = DatabaseEngine.fromName(config.getFirst(DB_DATABASE_ENGINE_KEY));
            String host = config.getFirst(DB_HOST_KEY);
            String port = config.getFirst(DB_PORT_KEY);
            String databaseName = config.getFirst(DB_DATABASE_NAME_KEY);
            String username = config.getFirst(DB_USERNAME_KEY);
            String password = config.getFirst(DB_PASSWORD_KEY);
            passwordHashingAlgorithm = PasswordHashingAlgorithm.fromName(config.getFirst(DB_PASSWORD_HASHING_ALGORITHM_KEY));

            Class.forName(databaseEngine.getDriver());

            Connection connection = DriverManager.getConnection(
                    databaseEngine.getProtocol() + "//" + host + ":" + port + "/" + databaseName, username, password);

            String userTable = config.getFirst(DB_USER_TABLE_KEY);
            String usernameColumn = config.getFirst(DB_USER_TABLE_USERNAME_COLUMN_KEY);
            String passwordColumn = config.getFirst(DB_USER_TABLE_PASSWORD_COLUMN_KEY);
            String rolesSql = config.getFirst(DB_ROLES_SQL_KEY);

            DatabaseConfig databaseConfig = new DatabaseConfig(userTable, usernameColumn, passwordColumn, rolesSql);
            userDAO = new UserDAO(connection, databaseConfig);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            throw new RuntimeException(e);
        }

        return new ExternalDatabaseStorageProvider(passwordHashingAlgorithm, session, model, userDAO);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                // Database Engine
                .property().name(DB_DATABASE_ENGINE_KEY)
                .type(ProviderConfigProperty.LIST_TYPE)
                .label("Database Engine")
                .options(DatabaseEngine.POSTGRESQL.getName())
                .defaultValue(DatabaseEngine.POSTGRESQL.getName())
                .add()

                // Connection Host
                .property().name(DB_HOST_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Host")
                .defaultValue("localhost")
                .add()

                // DB Port
                .property().name(DB_PORT_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Port")
                .defaultValue("5432")
                .add()

                // Database Name
                .property().name(DB_DATABASE_NAME_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Name")
                .add()

                // DB Username
                .property().name(DB_USERNAME_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Username")
                .defaultValue("user")
                .add()

                // DB Password
                .property().name(DB_PASSWORD_KEY)
                .type(ProviderConfigProperty.PASSWORD)
                .label("Database Password")
                .defaultValue("PASSWORD")
                .add()

                // User Table
                .property().name(DB_USER_TABLE_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("User Table Name")
                .defaultValue("user")
                .add()

                // Username Column
                .property().name(DB_USER_TABLE_USERNAME_COLUMN_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Username Column Name")
                .defaultValue("username")
                .add()

                // Password Column
                .property().name(DB_USER_TABLE_PASSWORD_COLUMN_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Password Column Name")
                .defaultValue("password")
                .add()

                // Password Hashing Algorithm
                .property().name(DB_PASSWORD_HASHING_ALGORITHM_KEY)
                .type(ProviderConfigProperty.LIST_TYPE)
                .label("Password Hashing Algorithm")
                .options(PasswordHashingAlgorithm.PKCS5S2.getName())
                .defaultValue(PasswordHashingAlgorithm.PKCS5S2.getName())
                .add()

                // Roles SQL
                .property().name(DB_ROLES_SQL_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Roles SQL")
                .add()
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
