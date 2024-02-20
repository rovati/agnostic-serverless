package ch.elca.rovl.dsl.api.database;

public class EnginedDatabaseStep {
    
    FluentDatabaseBuilder fdb;

    public EnginedDatabaseStep(FluentDatabaseBuilder fdb) {
        this.fdb = fdb;
    }

    public ConfigurableDatabaseStep rootUser(String username, String password) {
        fdb.withRootUser(username, password);
        return new ConfigurableDatabaseStep(fdb);
    }
}
