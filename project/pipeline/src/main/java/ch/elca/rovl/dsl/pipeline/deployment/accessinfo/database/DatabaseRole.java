package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database;

/**
 * Object representing a database role. Contains a username and a password.
 */
public class DatabaseRole {
    final String username;
    final String password;

    public DatabaseRole(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String username() { return username; }
    public String password() { return password; }
}
