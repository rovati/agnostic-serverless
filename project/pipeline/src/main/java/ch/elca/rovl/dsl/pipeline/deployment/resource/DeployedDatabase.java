package ch.elca.rovl.dsl.pipeline.deployment.resource;

import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.DatabaseAccess;
import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database.DatabaseRole;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableDatabase;

/**
 * Object representing a deployed cloud database with its target provider chosen by the user and the
 * endpoint url of the database.
 */
public class DeployedDatabase extends DeployedResource {

    final String endpoint;

    public DeployedDatabase(DeployableDatabase db, String endpoint) {
        super(db);
        this.endpoint = endpoint;
    }

    public DeployableDatabase getDatabase() {
        return (DeployableDatabase) deployableResource;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public DatabaseRole getSuperUser() {
        return ((DatabaseAccess) accessInfo).superuser();
    }

}
