package ch.elca.rovl.dsl.api.database;

/**
 * Configuration step to set root user configuration.
 */
public class EnginedDatabaseStep {
    
    FluentDatabaseBuilder fdb;

    protected EnginedDatabaseStep(FluentDatabaseBuilder fdb) {
        this.fdb = fdb;
    }

    /**
     * Sets username and password for the root user of the database.
     * <p>
     * NOTE this is the last step of required values for a database resource.
     * 
     * @param username
     * @param password
     * @return the step for optional values configuration
     */
    public ConfigurableDatabaseStep rootUser(String username, String password) {
        fdb.withRootUser(username, password);
        return new ConfigurableDatabaseStep(fdb);
    }
}
