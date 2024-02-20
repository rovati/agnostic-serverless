package ch.elca.rovl.dsl.pipeline.infraparsing.resource;

import java.util.Map;
import ch.elca.rovl.dsl.resource.database.Database;
import ch.elca.rovl.dsl.resource.database.DatabaseEngine;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseConfigType;


/**
 * Object representing a cloud database. It contains configuration specified through the
 * infrastructure DSL.
 */
public final class DslDatabase extends DslResource {

    private final String username;
    private final String password;
    private final DatabaseEngine engine;
    Map<DatabaseConfigType, Object> config;

    /**
     * Object constructor. The parameters are mandatory configuration required to deploy the
     * database on any provider.
     * 
     * @param name unique name of the database
     * @param username username of the master user of the database
     * @param password password of the master user of the database
     * @param engine engine and version of the database
     */
    public DslDatabase(String name, String username, String password, DatabaseEngine engine) {
        super(name);
        this.username = username;
        this.password = password;
        this.engine = engine;
    }

    public static DslDatabase fromApiDatabase(Database db) {
        DslDatabase dslDb = new DslDatabase(db.getName(), db.getUsername(), db.getPassword(), db.getEngine());
        dslDb.setConfig(db.getConfig());
        return dslDb;
    }

    /**
     * Registers a map of optional configuration data for the database, such as the minimum size.
     * 
     * @param config
     */
    public void setConfig(Map<DatabaseConfigType, Object> config) {
        this.config = config;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public DatabaseEngine getEngine() {
        return engine;
    }

    public Map<DatabaseConfigType, Object> getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return String.format("Database[name=%s]", name);
    }

}
