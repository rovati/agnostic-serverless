package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.database;

import ch.elca.rovl.dsl.pipeline.deployment.accessinfo.AccessInfo;

/**
 * Object containing access information for a database.
 */
public class DatabaseAccess extends AccessInfo {
    DatabaseRole superuser;

    public DatabaseAccess() {}

    public void setSuperUser(DatabaseRole superuser) {
        this.superuser = superuser;
    }

    public DatabaseRole superuser() {
        return superuser;
    }

}
