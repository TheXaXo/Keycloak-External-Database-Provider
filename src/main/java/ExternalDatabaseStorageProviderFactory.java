import config.UserTableConfig;
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
    public static final String DB_HOST_KEY = "db:host";
    public static final String DB_PORT_KEY = "db:port";
    public static final String DB_DATABASE_KEY = "db:database";
    public static final String DB_USERNAME_KEY = "db:username";
    public static final String DB_PASSWORD_KEY = "db:password";
    public static final String DB_USER_TABLE_KEY = "db:user-table";
    public static final String DB_USER_TABLE_USERNAME_COLUMN_KEY = "db:user-table-username";
    public static final String DB_USER_TABLE_PASSWORD_COLUMN_KEY = "db:user-table-password";

    @Override
    public ExternalDatabaseStorageProvider create(KeycloakSession session, ComponentModel model) {
        UserDAO userDAO = null;

        try {
            MultivaluedHashMap<String, String> config = model.getConfig();

            String host = config.getFirst(DB_HOST_KEY);
            String port = config.getFirst(DB_PORT_KEY);
            String database = config.getFirst(DB_DATABASE_KEY);
            String username = config.getFirst(DB_USERNAME_KEY);
            String password = config.getFirst(DB_PASSWORD_KEY);

            Class.forName("org.postgresql.Driver");

            Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://" + host + ":" + port + "/" + database, username, password);

            String userTable = config.getFirst(DB_USER_TABLE_KEY);
            String usernameColumn = config.getFirst(DB_USER_TABLE_USERNAME_COLUMN_KEY);
            String passwordColumn = config.getFirst(DB_USER_TABLE_PASSWORD_COLUMN_KEY);

            UserTableConfig userTableConfig = new UserTableConfig(userTable, usernameColumn, passwordColumn);
            userDAO = new UserDAO(connection, userTableConfig);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        return new ExternalDatabaseStorageProvider(session, model, userDAO);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                // Connection Host
                .property().name(DB_HOST_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Host")
                .defaultValue("localhost")
                .helpText("Host of the connection")
                .add()

                // DB Port
                .property().name(DB_PORT_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Port")
                .defaultValue("3306")
                .add()

                // Connection Database
                .property().name(DB_DATABASE_KEY)
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
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
