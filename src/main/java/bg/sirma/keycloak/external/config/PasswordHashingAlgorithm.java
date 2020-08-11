package bg.sirma.keycloak.external.config;

public enum PasswordHashingAlgorithm {
    PKCS5S2("Atlassian PKCS5S2");

    private String name;

    PasswordHashingAlgorithm(String name) {
        this.name = name;
    }

    public static PasswordHashingAlgorithm fromName(String name) {
        for (PasswordHashingAlgorithm algorithm : PasswordHashingAlgorithm.values()) {
            if (algorithm.getName().equalsIgnoreCase(name)) {
                return algorithm;
            }
        }

        throw new RuntimeException(name + " is not supported.");
    }

    public String getName() {
        return name;
    }
}
