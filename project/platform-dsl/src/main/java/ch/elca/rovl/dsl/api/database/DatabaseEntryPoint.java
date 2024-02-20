package ch.elca.rovl.dsl.api.database;

import ch.elca.rovl.dsl.registry.SystemRegistry;
import ch.elca.rovl.dsl.resource.database.DatabaseEngine;

public class DatabaseEntryPoint {

    FluentDatabaseBuilder fdb;

    public DatabaseEntryPoint(SystemRegistry sysReg, String dbName) {
        this.fdb = new FluentDatabaseBuilder(dbName);
        sysReg.registerDatabase(dbName, fdb);
    }

    public EnginedDatabaseStep engine(DatabaseEngine engine) {
        fdb.withEngine(engine);
        return new EnginedDatabaseStep(fdb);
    }
    
}
