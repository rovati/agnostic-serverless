package ch.elca.rovl.dsl.pipeline.linking.resource;

import java.util.Map;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslDatabase;
import ch.elca.rovl.dsl.pipeline.util.Provider;
import ch.elca.rovl.dsl.resource.database.DatabaseEngine;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseConfigType;

/**
 * Object representing a cloud database with its target provider chosen by the user. It contains a
 * reference to the database object of the previous pipeline step.
 */
public final class LinkedDatabase extends LinkedResource {

    public LinkedDatabase(DslDatabase db, Provider provider) {
        super(db, provider);
    }

    public DslDatabase getDatabase() {
        return (DslDatabase) dslResource;
    }

    public String getUsername() {
        return getDatabase().getUsername();
    }

    public String getPassword() {
        return getDatabase().getPassword();
    }

    public DatabaseEngine getEngine() {
        return getDatabase().getEngine();
    }

    public Map<DatabaseConfigType, Object> getConfig() {
        return getDatabase().getConfig();
    }

    @Override
    public String toString() {
        return String.format("LinkedDatabase[name=%s, provider=%s]", dslResource.getName(), provider);
    }

}
