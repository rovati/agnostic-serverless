package ch.elca.rovl.dsl.api.database;

import ch.elca.rovl.dsl.registry.SystemRegistry;
import ch.elca.rovl.dsl.resource.database.DatabaseEngine;

/**
 * Entry point for the fluent configuration of a database resource.
 */
public class DatabaseEntryPoint {

    FluentDatabaseBuilder fdb;

    public DatabaseEntryPoint(SystemRegistry sysReg, String dbName) {
        this.fdb = new FluentDatabaseBuilder(dbName);
        sysReg.registerDatabase(dbName, fdb);
    }

    /**
     * Sets the database engine.
     * 
     * @param engine
     * @return the next step in the configuration
     */
    public EnginedDatabaseStep engine(DatabaseEngine engine) {
        fdb.withEngine(engine);
        return new EnginedDatabaseStep(fdb);
    }
    
}
