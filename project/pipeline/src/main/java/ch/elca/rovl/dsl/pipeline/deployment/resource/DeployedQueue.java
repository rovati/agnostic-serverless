package ch.elca.rovl.dsl.pipeline.deployment.resource;

import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableQueue;

/**
 * Object representing a deployed cloud queue with its target provider chosen by the user and the
 * name of the corresponding resource in the cloud.
 */
public class DeployedQueue extends DeployedResource {

    public DeployedQueue(DeployableQueue q) {
        super(q);
    }

    public DeployableQueue getQueue() {
        return (DeployableQueue) deployableResource;
    }

    @Override
    public String toString() {
        return String.format("DeployedQueue[name=%s, provider=%s]", getName(), getProvider());
    }

}
