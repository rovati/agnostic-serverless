package ch.elca.rovl.dsl.api.database;

import java.util.HashMap;
import java.util.Map;

import ch.elca.rovl.dsl.api.FluentResourceBuilder;
import ch.elca.rovl.dsl.resource.database.Database;
import ch.elca.rovl.dsl.resource.database.DatabaseEngine;
import ch.elca.rovl.dsl.resource.database.WorkloadType;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseConfigType;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseSize;

public class FluentDatabaseBuilder implements FluentResourceBuilder {

    String name;
    String rootUserName;
    String rootUserPwd;
    DatabaseEngine engine;
    WorkloadType workload;
    int sizeGB = 0; // NOTE if 0, use provider min. if -1, user provider max

    protected FluentDatabaseBuilder(String name) {
        this.name = name;
    }

    public FluentDatabaseBuilder withEngine(DatabaseEngine engine) {
        this.engine = engine;
        return this;
    }

    public FluentDatabaseBuilder withRootUser(String username, String password) {
        this.rootUserName = username;
        this.rootUserPwd = password;
        return this;
    }

    public FluentDatabaseBuilder withWorkloadType(WorkloadType type) {
        this.workload = type;
        return this;
    }

    public FluentDatabaseBuilder withSizeGB(int size) {
        this.sizeGB = size;
        return this;
    }

    public FluentDatabaseBuilder withSize(DatabaseSize size) {
        if (size == DatabaseSize.MIN)
            this.sizeGB = 0;
        if (size == DatabaseSize.MAX)
            this.sizeGB = -1;

        return this;
    }

    @Override
    public void validate() {
        if (name == null)
            throw new IllegalStateException("Database is missing  the mandatory field 'name'.");

        if (rootUserName == null || rootUserPwd == null)
            throw new IllegalStateException(String.format(
                "Database '%s' is missing root user information.", name));

        if (engine == null)
            throw new IllegalStateException(String.format(
                "Database '%s' is missing the mandatory field 'engine'.", name));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Database build() {
        Database db = new Database(name, rootUserName, rootUserPwd, engine);
        Map<DatabaseConfigType,Object> config = new HashMap<>();

        if (workload != null)
            config.put(DatabaseConfigType.WORKLOAD_TYPE, workload);

        config.put(DatabaseConfigType.SIZE_GB, sizeGB);

        db.addConfig(config);
        return db;
    }
    
}
