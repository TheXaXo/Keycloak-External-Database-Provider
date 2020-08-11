package bg.sirma.keycloak.external;

import bg.sirma.keycloak.external.config.Database;
import bg.sirma.keycloak.external.config.PasswordHashingAlgorithm;
import bg.sirma.keycloak.external.config.UserTableConfig;
import bg.sirma.keycloak.external.dao.UserDAO;
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
    public static final String DB_DATABASE_KEY = "db:database";
    public static final String DB_HOST_KEY = "db:host";
    public static final String DB_PORT_KEY = "db:port";
    public static final String DB_DATABASE_NAME_KEY = "db:database-name";
    public static final String DB_USERNAME_KEY = "db:username";
    public static final String DB_PASSWORD_KEY = "db:password";
    public static final String DB_USER_TABLE_KEY = "db:user-table";
    public static final String DB_USER_TABLE_USERNAME_COLUMN_KEY = "db:user-table-username";
    public static final String DB_USER_TABLE_EMAIL_COLUMN_KEY = "db:user-table-email";
    public static final String DB_USER_TABLE_PASSWORD_COLUMN_KEY = "db:user-table-password";
    public static final String DB_PASSWORD_HASHING_ALGORITHM_KEY = "db:password-hashing-algorithm";

    @Override
    public ExternalDatabaseStorageProvider create(KeycloakSession session, ComponentModel model) {
        UserDAO userDAO;
        PasswordHashingAlgorithm passwordHashingAlgorithm;

        try {
            MultivaluedHashMap<String, String> config = model.getConfig();

            Database database = Database.fromName(config.getFirst(DB_DATABASE_KEY));
            String host = config.getFirst(DB_HOST_KEY);
            String port = config.getFirst(DB_PORT_KEY);
            String databaseName = config.getFirst(DB_DATABASE_NAME_KEY);
            String username = config.getFirst(DB_USERNAME_KEY);
            String password = config.getFirst(DB_PASSWORD_KEY);
            passwordHashingAlgorithm = PasswordHashingAlgorithm.fromName(config.getFirst(DB_PASSWORD_HASHING_ALGORITHM_KEY));

            Class.forName(database.getDriver());

            Connection connection = DriverManager.getConnection(
                    database.getProtocol() + "//" + host + ":" + port + "/" + databaseName, username, password);

            String userTable = config.getFirst(DB_USER_TABLE_KEY);
            String usernameColumn = config.getFirst(DB_USER_TABLE_USERNAME_COLUMN_KEY);
            String emailColumn = config.getFirst(DB_USER_TABLE_EMAIL_COLUMN_KEY);
            String passwordColumn = config.getFirst(DB_USER_TABLE_PASSWORD_COLUMN_KEY);

            UserTableConfig userTableConfig = new UserTableConfig(userTable, usernameColumn, emailColumn, passwordColumn);
            userDAO = new UserDAO(connection, userTableConfig);
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
                // Database
                .property().name(DB_DATABASE_KEY)
                .type(ProviderConfigProperty.LIST_TYPE)
                .label("Database")
                .options(Database.POSTGRESQL.getName())
                .add()

                // Connection Host
                .property().name(DB_HOST_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Host")
                .defaultValue("127.0.0.1")
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
                .secret(true)
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

                // Email Column
                .property().name(DB_USER_TABLE_EMAIL_COLUMN_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Email Column Name")
                .defaultValue("email")
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
                .add()
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
