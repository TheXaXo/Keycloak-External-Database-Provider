package bg.sirma.keycloak.external;

import org.keycloak.models.RoleModel;

import java.util.Set;

public class SimpleUserModel {

    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final boolean enabled;
    private final String credential;
    private final Set<RoleModel> roleMappings;

    public SimpleUserModel(String username, String email, String firstName, String lastName, boolean enabled, String credential, Set<RoleModel> roleMappings) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.credential = credential;
        this.roleMappings = roleMappings;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCredential() {
        return credential;
    }

    public Set<RoleModel> getRoleMappings() {
        return roleMappings;
    }
}
