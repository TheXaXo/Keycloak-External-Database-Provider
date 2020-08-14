package bg.sirma.keycloak.external;

import bg.sirma.keycloak.external.config.DatabaseConfig;
import bg.sirma.keycloak.external.config.DatabaseEngine;
import bg.sirma.keycloak.external.config.EnabledColumnType;
import bg.sirma.keycloak.external.config.PasswordHashingAlgorithm;
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
    public static final String DB_DATABASE_ENGINE_KEY = "db:database-engine";
    public static final String DB_HOST_KEY = "db:host";
    public static final String DB_PORT_KEY = "db:port";
    public static final String DB_DATABASE_NAME_KEY = "db:database-name";
    public static final String DB_USERNAME_KEY = "db:username";
    public static final String DB_PASSWORD_KEY = "db:password";
    public static final String DB_USER_TABLE_KEY = "db:user-table";
    public static final String DB_USER_TABLE_USERNAME_COLUMN_KEY = "db:user-table-username";
    public static final String DB_USER_TABLE_EMAIL_COLUMN_KEY = "db:user-table-email";
    public static final String DB_USER_TABLE_FIRST_NAME_COLUMN_KEY = "db:user-table-first-name";
    public static final String DB_USER_TABLE_LAST_NAME_COLUMN_KEY = "db:user-table-last-name";
    public static final String DB_USER_TABLE_PASSWORD_COLUMN_KEY = "db:user-table-password";
    public static final String DB_USER_TABLE_ENABLED_COLUMN_KEY = "db:user-table-enabled";
    public static final String DB_USER_TABLE_ENABLED_COLUMN_TYPE_KEY = "db:user-table-enabled_type";
    public static final String DB_PASSWORD_HASHING_ALGORITHM_KEY = "db:password-hashing-algorithm";
    public static final String DB_USER_ROLES_TABLE_KEY = "db:user-roles-table";
    private static final String DB_USER_ROLE_COLUMN_KEY = "db:user-role_name";
    private static final String DB_ROLE_USER_ID_FOREIGN_KEY = "db:role-table-user-id-fk";
    private static final String DB_USER_PRIMARY_KEY = "db:user-table-pk";

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
            String emailColumn = config.getFirst(DB_USER_TABLE_EMAIL_COLUMN_KEY);
            String passwordColumn = config.getFirst(DB_USER_TABLE_PASSWORD_COLUMN_KEY);
            String first = config.getFirst(DB_USER_TABLE_FIRST_NAME_COLUMN_KEY);
            String last = config.getFirst(DB_USER_TABLE_LAST_NAME_COLUMN_KEY);
            String enabled = config.getFirst(DB_USER_TABLE_ENABLED_COLUMN_KEY);
            EnabledColumnType enabledColumnType = EnabledColumnType.valueOf(config.getFirst(DB_USER_TABLE_ENABLED_COLUMN_TYPE_KEY));

            String rolesTable = config.getFirst(DB_USER_ROLES_TABLE_KEY);
            String roleColumn = config.getFirst(DB_USER_ROLE_COLUMN_KEY);
            String userIdForeignKeyColumn = config.getFirst(DB_ROLE_USER_ID_FOREIGN_KEY);
            String userIdPrimaryKeyColumn = config.getFirst(DB_USER_PRIMARY_KEY);

            DatabaseConfig databaseConfig = new DatabaseConfig(userTable, usernameColumn, emailColumn, first, last, enabled, enabledColumnType, passwordColumn,
                    rolesTable, roleColumn, userIdForeignKeyColumn, userIdPrimaryKeyColumn);
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

                // First Name Column
                .property().name(DB_USER_TABLE_FIRST_NAME_COLUMN_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("First Name Column Name")
                .defaultValue("first_name")
                .add()

                // First Name Column
                .property().name(DB_USER_TABLE_LAST_NAME_COLUMN_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Last Name Column Name")
                .defaultValue("last_name")
                .add()

                // Enabled Column
                .property().name(DB_USER_TABLE_ENABLED_COLUMN_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("User Enabled Column Name")
                .defaultValue("enabled")
                .add()

                // Enabled Column Type
                .property().name(DB_USER_TABLE_ENABLED_COLUMN_TYPE_KEY)
                .type(ProviderConfigProperty.LIST_TYPE)
                .label("User Enabled Column Type")
                .helpText("This represents the type of the 'user enabled' column.")
                .options(EnabledColumnType.BOOLEAN.name(), EnabledColumnType.NUMERIC.name())
                .defaultValue(EnabledColumnType.BOOLEAN.name())
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

                // User Roles Table Name
                .property().name(DB_USER_ROLES_TABLE_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("User Roles Table Name")
                .add()

                // User Role Column Name
                .property().name(DB_USER_ROLE_COLUMN_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Role Column Name")
                .add()

                // Role Table User ID Foreign Key
                .property().name(DB_ROLE_USER_ID_FOREIGN_KEY)
                .label("Role Table User ID Foreign Key")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()

                // User Table Primary Key
                .property().name(DB_USER_PRIMARY_KEY)
                .label("User Table Primary Key")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()

                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
}
