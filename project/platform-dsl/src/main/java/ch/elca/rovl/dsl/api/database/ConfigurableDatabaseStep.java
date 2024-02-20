package ch.elca.rovl.dsl.api.database;

import ch.elca.rovl.dsl.resource.database.WorkloadType;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseSize;

public class ConfigurableDatabaseStep {
    FluentDatabaseBuilder fdb;

    public ConfigurableDatabaseStep(FluentDatabaseBuilder builder) {
        this.fdb = builder;
    }

    public ConfigurableDatabaseStep workload(WorkloadType type) {
        fdb.withWorkloadType(type);
        return this;
    }

    public ConfigurableDatabaseStep sizeGB(int size) {
        fdb.withSizeGB(size);
        return this;
    }

    public ConfigurableDatabaseStep sizeGB(DatabaseSize size) {
        fdb.withSize(size);
        return this;
    }
}
