package ch.elca.rovl.dsl.api.database;

import ch.elca.rovl.dsl.resource.database.WorkloadType;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseSize;

/**
 * Database configuration step for optional values.
 */
public class ConfigurableDatabaseStep {
    FluentDatabaseBuilder fdb;

    public ConfigurableDatabaseStep(FluentDatabaseBuilder builder) {
        this.fdb = builder;
    }

    /**
     * Sets the workload type for the server hosting the database instance.
     * 
     * @param type
     * @return this
     */
    public ConfigurableDatabaseStep workload(WorkloadType type) {
        fdb.withWorkloadType(type);
        return this;
    }

    /**
     * Sets the max size in gigabytes of the database.
     * 
     * @param size
     * @return
     */
    public ConfigurableDatabaseStep sizeGB(int size) {
        fdb.withSizeGB(size);
        return this;
    }

    /**
     * Sets the database size to the minimum or maximum supported by the platform it
     * will be provisioned on.
     * 
     * @param size
     * @return
     */
    public ConfigurableDatabaseStep sizeGB(DatabaseSize size) {
        fdb.withSize(size);
        return this;
    }
}
