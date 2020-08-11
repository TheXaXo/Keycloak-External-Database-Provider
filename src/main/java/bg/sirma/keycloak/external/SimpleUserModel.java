package bg.sirma.keycloak.external;

public class SimpleUserModel {
    private final String username;
    private final String email;
    private final String credential;

    public SimpleUserModel(String username, String email, String credential) {
        this.username = username;
        this.email = email;
        this.credential = credential;
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
}
