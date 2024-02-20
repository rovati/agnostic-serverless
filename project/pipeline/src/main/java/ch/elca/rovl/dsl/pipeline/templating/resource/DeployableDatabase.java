package ch.elca.rovl.dsl.pipeline.templating.resource;

import java.util.Map;
import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedDatabase;
import ch.elca.rovl.dsl.resource.database.DatabaseEngine;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseConfigType;

/**
 * Object representing a cloud database with its target provider chosen by the user and the name of
 * the corresponding resource in the cloud. It contains a reference to the database object of the
 * previous pipeline step.
 */
public final class DeployableDatabase extends DeployableResource {

    public DeployableDatabase(LinkedDatabase db) {
        super(db);
    }

    public LinkedDatabase getDatabase() {
        return (LinkedDatabase) linkedResource;
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
        return String.format("DeployableDatabase[name=%s, provider=%s, cloud-name=%s]", getName(),
                getProvider(), cloudName);
    }

}
