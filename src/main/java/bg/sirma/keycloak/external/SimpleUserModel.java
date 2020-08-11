package bg.sirma.keycloak.external;

import org.keycloak.models.RoleModel;

import java.util.Set;

public class SimpleUserModel {

    private final String username;
    private final String email;
    private final String credential;
    private final Set<RoleModel> roleMappings;

    public SimpleUserModel(String username, String email, String credential, Set<RoleModel> roleMappings) {
        this.username = username;
        this.email = email;
        this.credential = credential;
        this.roleMappings = roleMappings;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getCredential() {
        return credential;
    }

    public Set<RoleModel> getRoleMappings() {
        return roleMappings;
    }
}
