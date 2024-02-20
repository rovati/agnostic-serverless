package ch.elca.rovl.dsl.pipeline.deployment.helper;

import ch.elca.rovl.dsl.pipeline.deployment.UpdateInfo;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedDatabase;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedFunction;
import ch.elca.rovl.dsl.pipeline.deployment.resource.DeployedQueue;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableDatabase;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableQueue;

/**
 * Interface offering helper methods for the deployment of resources to their target provider.
 */
public interface DeploymentHelper {

    DeployedQueue deploy(DeployableQueue queue);

    DeployedFunction deploy(DeployableFunction function) throws InterruptedException;

    DeployedDatabase deploy(DeployableDatabase database) throws InterruptedException;

    void configureFunction(DeployedFunction function, UpdateInfo updateInfo)
            throws InterruptedException;

    void close();
}
