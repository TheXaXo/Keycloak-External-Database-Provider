import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        ExternalDatabaseStorageProviderFactory factory = new ExternalDatabaseStorageProviderFactory();
        ComponentModel componentModel = new ComponentModel();

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.add("db:host", "localhost");
        config.add("db:port", "5432");
        config.add("db:database", "bocore");
        config.add("db:username", "bocore");
        config.add("db:password", "bocore12345");
        config.add("db:user-table", "user");
        config.add("db:user-table-username", "username");
        config.add("db:user-table-password", "password");
        componentModel.setConfig(config);

        ExternalDatabaseStorageProvider provider = factory.create(null, componentModel);

        UserModel testUser = createTestUser("admin@example.com");
        CredentialInput credentialInput = createTestCredentialInput("admin");

        provider.isValid(null, testUser, credentialInput);
    }

    private static UserModel createTestUser(String username) {
        return new UserModel() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public void setUsername(String username) {

            }

            @Override
            public Long getCreatedTimestamp() {
                return null;
            }

            @Override
            public void setCreatedTimestamp(Long timestamp) {

            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public void setEnabled(boolean enabled) {

            }

            @Override
            public void setSingleAttribute(String name, String value) {

            }

            @Override
            public void setAttribute(String name, List<String> values) {

            }

            @Override
            public void removeAttribute(String name) {

            }

            @Override
            public String getFirstAttribute(String name) {
                return null;
            }

            @Override
            public List<String> getAttribute(String name) {
                return null;
            }

            @Override
            public Map<String, List<String>> getAttributes() {
                return null;
            }

            @Override
            public Set<String> getRequiredActions() {
                return null;
            }

            @Override
            public void addRequiredAction(String action) {

            }

            @Override
            public void removeRequiredAction(String action) {

            }

            @Override
            public void addRequiredAction(RequiredAction action) {

            }

            @Override
            public void removeRequiredAction(RequiredAction action) {

            }

            @Override
            public String getFirstName() {
                return null;
            }

            @Override
            public void setFirstName(String firstName) {

            }

            @Override
            public String getLastName() {
                return null;
            }

            @Override
            public void setLastName(String lastName) {

            }

            @Override
            public String getEmail() {
                return null;
            }

            @Override
            public void setEmail(String email) {

            }

            @Override
            public boolean isEmailVerified() {
                return false;
            }

            @Override
            public void setEmailVerified(boolean verified) {

            }

            @Override
            public Set<GroupModel> getGroups() {
                return null;
            }

            @Override
            public void joinGroup(GroupModel group) {

            }

            @Override
            public void leaveGroup(GroupModel group) {

            }

            @Override
            public boolean isMemberOf(GroupModel group) {
                return false;
            }

            @Override
            public String getFederationLink() {
                return null;
            }

            @Override
            public void setFederationLink(String link) {

            }

            @Override
            public String getServiceAccountClientLink() {
                return null;
            }

            @Override
            public void setServiceAccountClientLink(String clientInternalId) {

            }

            @Override
            public Set<RoleModel> getRealmRoleMappings() {
                return null;
            }

            @Override
            public Set<RoleModel> getClientRoleMappings(ClientModel app) {
                return null;
            }

            @Override
            public boolean hasRole(RoleModel role) {
                return false;
            }

            @Override
            public void grantRole(RoleModel role) {

            }

            @Override
            public Set<RoleModel> getRoleMappings() {
                return null;
            }

            @Override
            public void deleteRoleMapping(RoleModel role) {

            }
        };
    }

    private static CredentialInput createTestCredentialInput(String password) {
        return new CredentialInput() {
            @Override
            public String getCredentialId() {
                return null;
            }

            @Override
            public String getType() {
                return PasswordCredentialModel.TYPE;
            }

            @Override
            public String getChallengeResponse() {
                return password;
            }
        };
    }
}
