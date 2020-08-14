package bg.sirma.keycloak.external;

import bg.sirma.keycloak.external.config.PasswordHashingAlgorithm;
import bg.sirma.keycloak.external.dao.UserDAO;
import com.atlassian.security.password.DefaultPasswordEncoder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ExternalDatabaseStorageProvider implements
        UserStorageProvider, UserLookupProvider, UserQueryProvider, CredentialInputValidator {

    private static final String CREDENTIAL_ATTRIBUTE = "credential";

    private final Map<String, UserModel> loadedUsers;
    private final PasswordHashingAlgorithm passwordHashingAlgorithm;
    private final KeycloakSession session;
    private final ComponentModel model;
    private final UserDAO userDAO;

    public ExternalDatabaseStorageProvider(PasswordHashingAlgorithm passwordHashingAlgorithm,
                                           KeycloakSession session,
                                           ComponentModel model,
                                           UserDAO userDAO) {
        this.loadedUsers = new ConcurrentHashMap<>();
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
            SimpleUserModel user = userDAO.getUserByColumn(realm, UserColumn.USERNAME, username);

            if (user != null) {
                adapter = createAdapter(realm, user);
                loadedUsers.put(username, adapter);
            }
        }

        return adapter;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        SimpleUserModel user = userDAO.getUserByColumn(realm, UserColumn.EMAIL, email);
        if (user != null) {
            UserModel adapter = createAdapter(realm, user);
            loadedUsers.put(user.getUsername(), adapter);
            return adapter;
        }
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
    public boolean isValid(RealmModel realm, UserModel userModel, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        UserModel adapter = this.getUserByUsername(userModel.getUsername(), realm);
        String credential = adapter.getFirstAttribute(CREDENTIAL_ATTRIBUTE);

        if (passwordHashingAlgorithm.name().equals(PasswordHashingAlgorithm.PKCS5S2.name())) {
            return DefaultPasswordEncoder.getDefaultInstance().isValidPassword(input.getChallengeResponse(), credential);
        }

        return false;
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

    @Override
    public int getUsersCount(RealmModel realm) {
        return userDAO.getUsersCount();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return convertUserModel(realm, userDAO.getUsers(realm));
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return convertUserModel(realm, userDAO.getUsers( realm, firstResult, maxResults));
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return convertUserModel(realm, userDAO.searchForUser(realm, search, null, null));
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return convertUserModel(realm, userDAO.searchForUser(realm, search, firstResult, maxResults));
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return convertUserModel(realm, userDAO.searchForUser(realm, params, null, null));
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        return convertUserModel(realm, userDAO.searchForUser(realm, params, firstResult, maxResults));
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return null;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return null;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return null;
    }

    private List<UserModel> convertUserModel(RealmModel realm, List<SimpleUserModel> users) {
        return users.stream().map(u -> createAdapter(realm, u)).collect(Collectors.toList());
    }

    private UserModel createAdapter(RealmModel realm, SimpleUserModel user) {
        return new AbstractUserAdapter(session, realm, model) {
            @Override
            public String getUsername() {
                return user.getUsername();
            }

            @Override
            public String getEmail() {
                return user.getEmail();
            }

            @Override
            public String getFirstName() {
                return user.getFirstName();
            }

            @Override
            public String getLastName() {
                return user.getLastName();
            }

            @Override
            public boolean isEnabled() {
                return user.isEnabled();
            }

            @Override
            public List<String> getAttribute(String name) {
                return super.getAttribute(name);
            }

            @Override
            public String getFirstAttribute(String name) {
                switch (name) {
                    case CREDENTIAL_ATTRIBUTE:
                        return user.getCredential();
                    case UserModel.USERNAME:
                        return getUsername();
                    case UserModel.EMAIL:
                        return getEmail();
                    case UserModel.FIRST_NAME:
                        return getFirstName();
                    case UserModel.LAST_NAME:
                        return getLastName();
                    case UserModel.ENABLED:
                        return String.valueOf(isEnabled());
                }
                throw new RuntimeException("Unknown user attribute: " + name);
            }

            @Override
            public Map<String, List<String>> getAttributes() {
                MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
                attributes.add(UserModel.USERNAME, getUsername());
                attributes.add(UserModel.EMAIL, getEmail());
                attributes.add(UserModel.FIRST_NAME, getFirstName());
                attributes.add(UserModel.LAST_NAME, getLastName());
                attributes.add(UserModel.ENABLED, String.valueOf(isEnabled()));
                return attributes;
            }

            @Override
            public Set<RoleModel> getRoleMappings() {
                return user.getRoleMappings();
            }
        };
    }
}

