import com.atlassian.security.password.DefaultPasswordEncoder;
import config.PasswordHashingAlgorithm;
import dao.UserDAO;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExternalDatabaseStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator, CredentialInputUpdater {

    private final Map<String, UserModel> loadedUsers;

    private final PasswordHashingAlgorithm passwordHashingAlgorithm;
    private final KeycloakSession session;
    private final ComponentModel model;
    private final UserDAO userDAO;

    public ExternalDatabaseStorageProvider(PasswordHashingAlgorithm passwordHashingAlgorithm,
                                           KeycloakSession session,
                                           ComponentModel model,
                                           UserDAO userDAO) {

        this.loadedUsers = new HashMap<>();
        this.passwordHashingAlgorithm = passwordHashingAlgorithm;
        this.session = session;
        this.model = model;
        this.userDAO = userDAO;
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();

        return getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel adapter = loadedUsers.get(username);

        if (adapter == null) {
            String password = userDAO.getPasswordHashedByUsername(username);

            if (password != null) {
                adapter = createAdapter(realm, username);
                loadedUsers.put(username, adapter);
            }
        }

        return adapter;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        String passwordHashed = userDAO.getPasswordHashedByUsername(user.getUsername());

        if (passwordHashed == null) {
            return false;
        }

        if (passwordHashingAlgorithm.name().equals(PasswordHashingAlgorithm.PKCS5S2.name())) {
            return DefaultPasswordEncoder.getDefaultInstance().isValidPassword(input.getChallengeResponse(), passwordHashed);
        }

        return false;
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (supportsCredentialType(input.getType())) {
            throw new ReadOnlyException("Users are read-only.");
        }

        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return new HashSet<>();
    }

    private UserModel createAdapter(RealmModel realm, String username) {
        Set<RoleModel> roleMappings = userDAO.getRoleMappings(realm, username);

        return new AbstractUserAdapter(session, realm, model) {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public Set<RoleModel> getRoleMappings() {
                return roleMappings;
            }
        };
    }

    @Override
    public void close() {
        try {
            this.userDAO.getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
